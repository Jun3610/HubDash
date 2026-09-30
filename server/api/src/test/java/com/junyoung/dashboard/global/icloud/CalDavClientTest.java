package com.junyoung.dashboard.global.icloud;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

// 응답 XML은 iCloud CalDAV가 실제로 주는 모양(계정마다 다른 서버 주소로 안내)을 줄인 것
class CalDavClientTest {

    private static final ICloudLogin LOGIN = new ICloudLogin("me@icloud.com", "abcd-efgh-ijkl-mnop");
    private static final String AUTH = "Basic " + Base64.getEncoder()
            .encodeToString("me@icloud.com:abcd-efgh-ijkl-mnop".getBytes(StandardCharsets.UTF_8));

    private MockRestServiceServer server;
    private CalDavClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new CalDavClient("https://caldav.test/", builder);
    }

    static String multistatus(String responses) {
        return "<?xml version=\"1.0\"?><d:multistatus xmlns:d=\"DAV:\" xmlns:c=\"urn:ietf:params:xml:ns:caldav\">"
                + responses + "</d:multistatus>";
    }

    void expectPrincipal() {
        server.expect(requestTo("https://caldav.test/"))
                .andExpect(method(HttpMethod.valueOf("PROPFIND")))
                .andExpect(header("Authorization", AUTH))
                .andExpect(header("Depth", "0"))
                .andRespond(withStatus(HttpStatus.MULTI_STATUS).contentType(MediaType.APPLICATION_XML).body(multistatus("""
                        <d:response><d:href>/</d:href><d:propstat><d:prop>
                          <d:current-user-principal><d:href>/123456/principal/</d:href></d:current-user-principal>
                        </d:prop></d:propstat></d:response>""")));
    }

    @Test
    void principalIsResolvedAgainstServer() {
        expectPrincipal();

        assertThat(client.principal(LOGIN)).isEqualTo(URI.create("https://caldav.test/123456/principal/"));
        server.verify();
    }

    @Test
    void listsOnlyEventCalendarsFollowingHomeSetOnOtherHost() {
        expectPrincipal();
        server.expect(requestTo("https://caldav.test/123456/principal/"))
                .andRespond(withStatus(HttpStatus.MULTI_STATUS).contentType(MediaType.APPLICATION_XML).body(multistatus("""
                        <d:response><d:href>/123456/principal/</d:href><d:propstat><d:prop>
                          <c:calendar-home-set><d:href>https://p43-caldav.test:443/123456/calendars/</d:href></c:calendar-home-set>
                        </d:prop></d:propstat></d:response>""")));
        server.expect(requestTo("https://p43-caldav.test:443/123456/calendars/"))
                .andExpect(header("Depth", "1"))
                .andRespond(withStatus(HttpStatus.MULTI_STATUS).contentType(MediaType.APPLICATION_XML).body(multistatus("""
                        <d:response><d:href>/123456/calendars/</d:href><d:propstat><d:prop>
                          <d:resourcetype><d:collection/></d:resourcetype></d:prop></d:propstat></d:response>
                        <d:response><d:href>/123456/calendars/home/</d:href><d:propstat><d:prop>
                          <d:displayname>집</d:displayname>
                          <d:resourcetype><d:collection/><c:calendar/></d:resourcetype>
                          <c:supported-calendar-component-set><c:comp name="VEVENT"/></c:supported-calendar-component-set>
                        </d:prop></d:propstat></d:response>
                        <d:response><d:href>/123456/calendars/tasks/</d:href><d:propstat><d:prop>
                          <d:displayname>미리 알림</d:displayname>
                          <d:resourcetype><d:collection/><c:calendar/></d:resourcetype>
                          <c:supported-calendar-component-set><c:comp name="VTODO"/></c:supported-calendar-component-set>
                        </d:prop></d:propstat></d:response>
                        <d:response><d:href>/123456/calendars/ParkJunYoung_Schedule/</d:href><d:propstat><d:prop>
                          <d:displayname>ParkJunYoung_Schedule</d:displayname>
                          <d:resourcetype><d:collection/><c:calendar/></d:resourcetype>
                        </d:prop></d:propstat></d:response>""")));

        assertThat(client.calendars(LOGIN)).containsExactly(
                new CalDavCalendar("집", "https://p43-caldav.test:443/123456/calendars/home/"),
                new CalDavCalendar("ParkJunYoung_Schedule", "https://p43-caldav.test:443/123456/calendars/ParkJunYoung_Schedule/"));
        server.verify();
    }

    @Test
    void wrongPasswordGivesFriendlyMessageWithoutSecret() {
        server.expect(requestTo("https://caldav.test/")).andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> client.principal(LOGIN))
                .isInstanceOf(ICloudException.class)
                .hasMessageContaining("앱 암호")
                .message().doesNotContain("abcd-efgh-ijkl-mnop");
    }

    @Test
    void reportAsksForTimeRangeAndReturnsCalendarData() {
        server.expect(requestTo("https://p43-caldav.test/123456/calendars/home/"))
                .andExpect(method(HttpMethod.valueOf("REPORT")))
                .andExpect(header("Depth", "1"))
                .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers.content()
                        .string(org.hamcrest.Matchers.containsString("start=\"20250930T000000Z\" end=\"20270930T000000Z\"")))
                .andRespond(withStatus(HttpStatus.MULTI_STATUS).contentType(MediaType.APPLICATION_XML).body(multistatus("""
                        <d:response><d:href>/123456/calendars/home/a.ics</d:href><d:propstat><d:prop>
                          <c:calendar-data>BEGIN:VCALENDAR
                        BEGIN:VEVENT
                        UID:A
                        SUMMARY:장보기
                        END:VEVENT
                        END:VCALENDAR</c:calendar-data>
                        </d:prop></d:propstat></d:response>""")));

        java.util.List<String> ics = client.events(LOGIN, "https://p43-caldav.test/123456/calendars/home/",
                java.time.Instant.parse("2025-09-30T00:00:00Z"), java.time.Instant.parse("2027-09-30T00:00:00Z"));

        assertThat(ics).singleElement().asString().contains("UID:A").contains("장보기");
        server.verify();
    }

    @Test
    void loginToStringHidesPassword() {
        assertThat(LOGIN.toString()).doesNotContain("abcd");
    }
}

package com.junyoung.dashboard.global.icloud;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * iCloud 캘린더를 CalDAV(RFC 4791)로 읽는다 (이슈 #227, #229).
 * 순서: 계정 주소(current-user-principal) → 캘린더 모음 주소(calendar-home-set) → 캘린더 목록 → 캘린더별 일정 조회.
 * iCloud는 계정마다 다른 서버(p43-caldav.icloud.com 등)로 안내하므로, 응답의 주소를 그대로 따라간다.
 */
@Component
public class CalDavClient {

    private static final String DAV = "DAV:";
    private static final String CALDAV = "urn:ietf:params:xml:ns:caldav";
    private static final DateTimeFormatter UTC_STAMP = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC);

    private final URI baseUri;
    private final RestClient rest;

    // 생성자가 둘(테스트용 Builder 주입)이라 스프링이 쓸 생성자를 명시한다
    @Autowired
    public CalDavClient(@Value("${app.icloud.caldav-url:https://caldav.icloud.com/}") String baseUrl) {
        // 기본 엔진(HttpURLConnection)은 PROPFIND/REPORT 같은 WebDAV 메서드를 보내지 못해 JDK HttpClient를 쓴다
        this(baseUrl, RestClient.builder().requestFactory(new JdkClientHttpRequestFactory(
                HttpClient.newBuilder()
                        .followRedirects(HttpClient.Redirect.NORMAL)
                        .connectTimeout(Duration.ofSeconds(10))
                        .build())));
    }

    CalDavClient(String baseUrl, RestClient.Builder builder) {
        this.baseUri = URI.create(baseUrl.endsWith("/") ? baseUrl : baseUrl + "/");
        this.rest = builder.build();
    }

    /** 로그인이 되는지 확인 (계정 주소를 받아 온다). 실패하면 ICloudException */
    public URI principal(ICloudLogin login) {
        Document doc = propfind(login, baseUri, "0", """
                <d:propfind xmlns:d="DAV:"><d:prop><d:current-user-principal/></d:prop></d:propfind>""");
        String href = firstHref(doc, "current-user-principal");
        if (href == null) {
            throw new ICloudException("iCloud 응답에서 계정 주소를 찾지 못했어요");
        }
        return baseUri.resolve(href);
    }

    /** 일정을 담는 캘린더 목록 (미리 알림 목록 같은 할 일 전용 캘린더는 뺀다) */
    public List<CalDavCalendar> calendars(ICloudLogin login) {
        URI principal = principal(login);
        Document home = propfind(login, principal, "0", """
                <d:propfind xmlns:d="DAV:" xmlns:c="urn:ietf:params:xml:ns:caldav"><d:prop><c:calendar-home-set/></d:prop></d:propfind>""");
        String homeHref = firstHref(home, "calendar-home-set");
        if (homeHref == null) {
            throw new ICloudException("iCloud 응답에서 캘린더 주소를 찾지 못했어요");
        }
        URI homeUri = principal.resolve(homeHref);
        Document list = propfind(login, homeUri, "1", """
                <d:propfind xmlns:d="DAV:" xmlns:c="urn:ietf:params:xml:ns:caldav">
                  <d:prop><d:displayname/><d:resourcetype/><c:supported-calendar-component-set/></d:prop>
                </d:propfind>""");
        List<CalDavCalendar> calendars = new ArrayList<>();
        NodeList responses = list.getElementsByTagNameNS(DAV, "response");
        for (int i = 0; i < responses.getLength(); i++) {
            Element response = (Element) responses.item(i);
            if (response.getElementsByTagNameNS(CALDAV, "calendar").getLength() == 0) {
                continue; // 캘린더가 아닌 모음(홈 자체, 받은편지함 등)
            }
            if (!supportsEvents(response)) {
                continue;
            }
            String href = text(response, DAV, "href");
            String name = text(response, DAV, "displayname");
            if (href != null) {
                calendars.add(new CalDavCalendar(name == null || name.isBlank() ? href : name.strip(), homeUri.resolve(href).toString()));
            }
        }
        return calendars;
    }

    /** 캘린더 하나에서 기간(from ~ to)에 걸친 일정의 iCalendar 원문들 (REPORT calendar-query) */
    public List<String> events(ICloudLogin login, String calendarUrl, Instant from, Instant to) {
        String body = """
                <c:calendar-query xmlns:d="DAV:" xmlns:c="urn:ietf:params:xml:ns:caldav">
                  <d:prop><c:calendar-data/></d:prop>
                  <c:filter><c:comp-filter name="VCALENDAR"><c:comp-filter name="VEVENT">
                    <c:time-range start="%s" end="%s"/>
                  </c:comp-filter></c:comp-filter></c:filter>
                </c:calendar-query>""".formatted(UTC_STAMP.format(from), UTC_STAMP.format(to));
        Document doc = request(login, HttpMethod.valueOf("REPORT"), URI.create(calendarUrl), "1", body);
        List<String> ics = new ArrayList<>();
        NodeList data = doc.getElementsByTagNameNS(CALDAV, "calendar-data");
        for (int i = 0; i < data.getLength(); i++) {
            String text = data.item(i).getTextContent();
            if (text != null && !text.isBlank()) {
                ics.add(text);
            }
        }
        return ics;
    }

    private Document propfind(ICloudLogin login, URI uri, String depth, String body) {
        return request(login, HttpMethod.valueOf("PROPFIND"), uri, depth, body);
    }

    private Document request(ICloudLogin login, HttpMethod method, URI uri, String depth, String body) {
        byte[] xml;
        try {
            // 바이트로 받아 XML 선언(없으면 UTF-8)으로 읽는다 — 문자열로 받으면 charset 없는 응답의 한글이 깨진다
            xml = rest.method(method)
                    .uri(uri)
                    .header("Authorization", basic(login))
                    .header("Depth", depth)
                    .contentType(new MediaType("application", "xml", StandardCharsets.UTF_8))
                    .body(body)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, r) -> {
                        int code = r.getStatusCode().value();
                        throw new ICloudException(code == 401 || code == 403
                                ? "Apple ID나 앱 암호가 올바르지 않아요 — appleid.apple.com에서 만든 앱 전용 암호를 넣어 주세요"
                                : "iCloud 캘린더 오류 (" + code + ")");
                    })
                    .body(byte[].class);
        } catch (ResourceAccessException e) {
            throw new ICloudException("iCloud에 연결할 수 없어요 — 네트워크를 확인해 주세요");
        }
        return parse(xml);
    }

    private static String basic(ICloudLogin login) {
        String pair = login.appleId() + ":" + login.appPassword();
        return "Basic " + Base64.getEncoder().encodeToString(pair.getBytes(StandardCharsets.UTF_8));
    }

    static Document parse(byte[] xml) {
        if (xml == null || xml.length == 0) {
            throw new ICloudException("iCloud 응답이 비어 있어요");
        }
        try {
            DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
            f.setNamespaceAware(true);
            // 외부 엔티티를 막는다 (XXE)
            f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            f.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            f.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            return f.newDocumentBuilder().parse(new ByteArrayInputStream(xml));
        } catch (Exception e) {
            throw new ICloudException("iCloud 응답을 읽지 못했어요");
        }
    }

    /** <prop> 안의 이름(localName) 요소 밑 첫 <href> */
    private static String firstHref(Document doc, String propLocalName) {
        NodeList props = doc.getElementsByTagNameNS("*", propLocalName);
        for (int i = 0; i < props.getLength(); i++) {
            String href = text((Element) props.item(i), DAV, "href");
            if (href != null) {
                return href;
            }
        }
        return null;
    }

    private static boolean supportsEvents(Element response) {
        NodeList sets = response.getElementsByTagNameNS(CALDAV, "supported-calendar-component-set");
        if (sets.getLength() == 0) {
            return true; // 알려 주지 않으면 일정 캘린더로 본다
        }
        NodeList comps = ((Element) sets.item(0)).getElementsByTagNameNS(CALDAV, "comp");
        for (int i = 0; i < comps.getLength(); i++) {
            if ("VEVENT".equalsIgnoreCase(((Element) comps.item(i)).getAttribute("name"))) {
                return true;
            }
        }
        return false;
    }

    private static String text(Element parent, String ns, String localName) {
        NodeList nodes = parent.getElementsByTagNameNS(ns, localName);
        if (nodes.getLength() == 0) {
            return null;
        }
        Node n = nodes.item(0);
        String t = n.getTextContent();
        return t == null ? null : t.strip();
    }
}

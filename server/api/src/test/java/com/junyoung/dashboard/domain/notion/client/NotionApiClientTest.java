package com.junyoung.dashboard.domain.notion.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

// 응답 JSON은 Notion API 문서의 실제 응답 형태에서 필요한 필드만 남긴 것이다.
class NotionApiClientTest {

    private static final String BASE = "https://api.notion.test";

    private MockRestServiceServer server;
    private NotionApiClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new NotionApiClient(builder, "secret-token", BASE);
    }

    @Test
    void queriesDatabaseAcrossPagesAndReadsTitleProperty() {
        server.expect(requestTo(BASE + "/v1/databases/db1/query"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer secret-token"))
                .andExpect(header("Notion-Version", NotionApiClient.NOTION_VERSION))
                .andExpect(content().json("{\"page_size\":100}"))
                .andRespond(withSuccess("""
                        {"object":"list","has_more":true,"next_cursor":"cursor-2","results":[
                          {"object":"page","id":"3532b9e1-fd14-80ff-b994-fe9eb920f8c3","archived":false,"in_trash":false,
                           "properties":{"상태":{"type":"select","select":{"name":"완료"}},
                                         "개념 ":{"type":"title","title":[{"plain_text":"도커 "},{"plain_text":"개념"}]}}},
                          {"object":"page","id":"3552b9e1-fd14-80fe-93cb-edb55280bbd5","archived":false,"in_trash":true,
                           "properties":{"개념 ":{"type":"title","title":[{"plain_text":"지운 페이지"}]}}}
                        ]}""", MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE + "/v1/databases/db1/query"))
                .andExpect(content().json("{\"page_size\":100,\"start_cursor\":\"cursor-2\"}"))
                .andRespond(withSuccess("""
                        {"object":"list","has_more":false,"next_cursor":null,"results":[
                          {"object":"page","id":"3572b9e1-fd14-8066-a3ad-dd6f3ee1a0f4","archived":false,"in_trash":false,
                           "properties":{"개념 ":{"type":"title","title":[]}}}
                        ]}""", MediaType.APPLICATION_JSON));

        List<NotionPage> pages = client.listDatabasePages("db1");

        assertThat(pages).containsExactly(
                new NotionPage("3532b9e1fd1480ffb994fe9eb920f8c3", "도커 개념"),
                new NotionPage("3572b9e1fd148066a3addd6f3ee1a0f4", ""));
        server.verify();
    }

    @Test
    void listsOnlyChildPagesOfParentPage() {
        server.expect(requestTo(BASE + "/v1/blocks/page1/children?page_size=100"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"object":"list","has_more":true,"next_cursor":"c2","results":[
                          {"object":"block","id":"3ba2b9e1-fd14-8097-9fda-f6365c0672af","type":"child_page",
                           "archived":false,"in_trash":false,"child_page":{"title":"알고리즘과자료구조"}},
                          {"object":"block","id":"aaaaaaaa-fd14-8097-9fda-f6365c0672af","type":"paragraph","archived":false}
                        ]}""", MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE + "/v1/blocks/page1/children?page_size=100&start_cursor=c2"))
                .andRespond(withSuccess("""
                        {"object":"list","has_more":false,"next_cursor":null,"results":[
                          {"object":"block","id":"bbbbbbbb-fd14-8097-9fda-f6365c0672af","type":"child_database",
                           "archived":false,"child_database":{"title":"하위 DB"}},
                          {"object":"block","id":"3ba2b9e1-fd14-8071-916d-e2b84b8b9828","type":"child_page",
                           "archived":false,"in_trash":false,"child_page":{"title":"프로그래밍기초2"}}
                        ]}""", MediaType.APPLICATION_JSON));

        List<NotionPage> pages = client.listChildPages("page1");

        assertThat(pages).containsExactly(
                new NotionPage("3ba2b9e1fd1480979fdaf6365c0672af", "알고리즘과자료구조"),
                new NotionPage("3ba2b9e1fd148071916de2b84b8b9828", "프로그래밍기초2"));
        server.verify();
    }

    @Test
    void notConfiguredWithoutToken() {
        assertThat(new NotionApiClient(RestClient.builder(), "", BASE).isConfigured()).isFalse();
        assertThat(client.isConfigured()).isTrue();
    }
}

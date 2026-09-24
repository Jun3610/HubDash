package com.junyoung.dashboard.domain.hub.entity;

import com.junyoung.dashboard.global.common.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "hub_category")
public class HubCategory extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    /** 'Notion 불러오기'로 새 글을 가져올 노션 DB (대시 없는 32자리, 이슈 #163) */
    @Column(name = "notion_database_id", length = 32)
    private String notionDatabaseId;

    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<HubLink> links = new ArrayList<>();

    public HubCategory(String name, String description) {
        this(name, description, null);
    }

    public HubCategory(String name, String description, String notionDatabaseId) {
        this.name = name;
        this.description = description;
        this.notionDatabaseId = notionDatabaseId;
    }

    public void update(String name, String description, String notionDatabaseId) {
        this.name = name;
        this.description = description;
        this.notionDatabaseId = notionDatabaseId;
    }
}

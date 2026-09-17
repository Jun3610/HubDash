package com.junyoung.dashboard.global.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    public static final String HEALTH_RAW_LOG_TOPIC = "health-raw-log";
    public static final String SCHEDULE_RAW_EVENT_TOPIC = "schedule-raw-event";
    public static final String MEMO_RAW_TOPIC = "memo-raw";
    public static final String REMINDER_RAW_TOPIC = "reminder-raw";
    public static final String HUB_RAW_LINK_TOPIC = "hub-raw-link";
    public static final String STUDY_RAW_PROGRESS_TOPIC = "study-raw-progress";

    @Bean
    public NewTopic healthRawLogTopic() {
        return TopicBuilder.name(HEALTH_RAW_LOG_TOPIC)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic scheduleRawEventTopic() {
        return TopicBuilder.name(SCHEDULE_RAW_EVENT_TOPIC)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic memoRawTopic() {
        return TopicBuilder.name(MEMO_RAW_TOPIC)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic reminderRawTopic() {
        return TopicBuilder.name(REMINDER_RAW_TOPIC)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic hubRawLinkTopic() {
        return TopicBuilder.name(HUB_RAW_LINK_TOPIC)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic studyRawProgressTopic() {
        return TopicBuilder.name(STUDY_RAW_PROGRESS_TOPIC)
                .partitions(1)
                .replicas(1)
                .build();
    }
}

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
}

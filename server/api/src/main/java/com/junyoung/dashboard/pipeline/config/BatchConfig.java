package com.junyoung.dashboard.pipeline.config;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.EnableJdbcJobRepository;
import org.springframework.context.annotation.Configuration;

// Spring Boot 4.1(Spring Batch 6)의 기본 BatchAutoConfiguration은 in-memory(ResourcelessJobRepository)만
// 제공한다 — Job/Step 실행 이력이 재시작 시 전부 사라지고 Postgres에도 전혀 기록되지 않는다(V18 Flyway로
// BATCH_* 테이블을 만들어뒀어도 사용되지 않음). 다른 도메인 데이터처럼 실행 이력도 Postgres에 남기기 위해
// JDBC 기반 JobRepository를 명시적으로 활성화한다. dataSourceRef/transactionManagerRef 기본값
// ("dataSource"/"transactionManager")이 이미 이 앱의 빈 이름과 일치해 별도 지정이 필요 없다.
@Configuration
@EnableBatchProcessing
@EnableJdbcJobRepository
public class BatchConfig {
}

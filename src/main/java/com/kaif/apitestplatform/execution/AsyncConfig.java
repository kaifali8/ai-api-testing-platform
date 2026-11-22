package com.kaif.apitestplatform.execution;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {

    // Bounded on purpose: at most 2 runs execute at once and 10 wait.
    // Beyond that, new runs are rejected instead of piling up in memory.
    @Bean(name = "testRunExecutor")
    public ThreadPoolTaskExecutor testRunExecutor() {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setCorePoolSize(2);
        ex.setMaxPoolSize(2);
        ex.setQueueCapacity(10);
        ex.setThreadNamePrefix("test-run-");
        return ex;
    }

    @Bean(name = "aiExecutor")
    public ThreadPoolTaskExecutor aiExecutor() {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setCorePoolSize(1);
        ex.setMaxPoolSize(1);
        ex.setQueueCapacity(5);
        ex.setThreadNamePrefix("ai-analysis-");
        return ex;
    }
}
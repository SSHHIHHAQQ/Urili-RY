package com.ruoyi.integration.config;

import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 上游手动同步是外部接口长任务，单线程排队执行，避免弱服务器被人工同步打满。
 */
@Configuration
public class UpstreamSyncTaskExecutorConfig
{
    @Bean(name = "upstreamSyncTaskExecutor")
    public ThreadPoolTaskExecutor upstreamSyncTaskExecutor()
    {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(50);
        executor.setKeepAliveSeconds(300);
        executor.setThreadNamePrefix("upstream-sync-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        return executor;
    }
}

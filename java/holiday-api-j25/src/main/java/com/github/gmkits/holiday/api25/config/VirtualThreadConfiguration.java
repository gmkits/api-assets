package com.github.gmkits.holiday.api25.config;

import org.springframework.boot.tomcat.TomcatProtocolHandlerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.support.TaskExecutorAdapter;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.Executors;

/**
 * 启用虚拟线程，使每个 HTTP 请求和异步任务都在虚拟线程上运行。
 *
 * <p>虚拟线程（Virtual Threads）自 Java 21 起正式可用，
 * 在小机器上也能支撑高并发：不再受平台线程池大小限制，
 * 每个请求使用独立的虚拟线程，IO 等待时自动让出底层载体线程。</p>
 */
@Configuration
@EnableAsync
public class VirtualThreadConfiguration {

    /**
     * 异步任务执行器：所有 @Async 方法都在虚拟线程上运行。
     */
    @Bean
    public AsyncTaskExecutor applicationTaskExecutor() {
        return new TaskExecutorAdapter(Executors.newVirtualThreadPerTaskExecutor());
    }

    /**
     * 让 Tomcat 的请求处理线程也使用虚拟线程。
     */
    @Bean
    public TomcatProtocolHandlerCustomizer<?> protocolHandlerVirtualThreadExecutorCustomizer() {
        return protocolHandler -> protocolHandler.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
    }
}

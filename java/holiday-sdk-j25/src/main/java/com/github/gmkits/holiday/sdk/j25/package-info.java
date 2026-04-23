/**
 * cn-holiday-kit Java 25 SDK —— 纯 JDK 客户端（无 Spring 依赖）。
 *
 * <p>主要入口：{@link com.github.gmkits.holiday.sdk.j25.HolidayClient}。</p>
 *
 * <h2>设计要点</h2>
 * <ul>
 *   <li>HTTP/2 + 虚拟线程 ({@link java.util.concurrent.Executors#newVirtualThreadPerTaskExecutor()})</li>
 *   <li>每个查询方法同时提供同步与 {@link java.util.concurrent.CompletableFuture} 版本</li>
 *   <li>{@link com.github.gmkits.holiday.sdk.j25.HolidayClient#batchDays} 批量查询：
 *     虚拟线程 fan-out / fan-in，单一子任务失败不影响其它结果</li>
 *   <li>内置 {@link java.util.concurrent.ConcurrentHashMap} 轻量缓存（不引入 Caffeine）</li>
 *   <li>Builder 配置：endpoint / timeout / maxInflight / cache / userAgent</li>
 * </ul>
 */
package com.github.gmkits.holiday.sdk.j25;

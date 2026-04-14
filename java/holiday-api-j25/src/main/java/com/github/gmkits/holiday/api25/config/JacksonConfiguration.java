package com.github.gmkits.holiday.api25.config;

import org.springframework.context.annotation.Configuration;

/**
 * Jackson 序列化配置（Jackson 3.x / Spring Boot 4）。
 *
 * <p>Spring Boot 4 自动配置 Jackson ObjectMapper，日期/时间序列化通过
 * {@code spring.jackson.serialization} 属性控制。
 * 此配置类保留为扩展点，如需自定义序列化行为可在此添加 Bean。</p>
 */
@Configuration
public class JacksonConfiguration {
    // Spring Boot 4 自动注册 ObjectMapper（Jackson 3.x），无需手动创建。
    // 日期格式通过 application.yml 的 spring.jackson.serialization 配置。
}

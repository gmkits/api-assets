package com.github.gmkits.holiday.api25.config;

import com.google.common.collect.ImmutableList;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 文档配置。
 */
@Configuration
public class OpenApiConfiguration {

    @Bean
    public OpenAPI holidayApi25OpenApi(HolidayApi25Properties properties) {
        return new OpenAPI()
                .info(new Info()
                        .title("cn-holiday-kit 内网节假日 API")
                        .version(properties.getApiVersion())
                        .description("基于 .hday bundle 的 Java 25 / Spring Boot 4 查询服务"))
                .servers(ImmutableList.of(new Server().url("/")));
    }
}

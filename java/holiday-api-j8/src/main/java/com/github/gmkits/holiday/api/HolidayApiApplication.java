package com.github.gmkits.holiday.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 唯一 HTTP API 服务入口。
 *
 * <p>应用编译为 Java 8 字节码，同一个可执行 JAR 可运行在 JDK 8 及更高版本。</p>
 */
@SpringBootApplication
public class HolidayApiApplication {

    /**
     * 启动内嵌 HTTP 服务。
     *
     * @param args Spring Boot 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(HolidayApiApplication.class, args);
    }
}

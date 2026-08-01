# Java library architecture

## Publication boundary

Applications consume one coordinate and one physical artifact:

```xml
<dependency>
  <groupId>com.github.gmkits</groupId>
  <artifactId>cn-holiday-kit</artifactId>
  <version>1.0.0-rc1</version>
</dependency>
```

The published JAR contains:

```text
com.github.gmkits.holiday          unified facade
com.github.gmkits.holiday.core     binary bundle reader and query engine
com.github.gmkits.holiday.lunar    lunar and solar-term algorithms
com.github.gmkits.holiday.spec     stable value types
cn-holiday-kit/assets              built-in offline date assets
```

These are Java subpackages inside one artifact, not separate runtime modules.
Keeping package boundaries makes the source understandable without creating
split packages: every package is owned by the single
`com.github.gmkits.holiday` runtime module.

The Maven projects `holiday-spec-java`, `holiday-lunar-java`, and
`holiday-core-java` are internal build partitions, not separate product
releases. They allow focused tests and short dependency directions while the
shade step merges them into the final artifact and removes them from its
published POM. HTTP API and JMH are deployment/test tools and are also
outside the library publication boundary.
Their POMs set `maven.deploy.skip=true`, so a reactor deploy cannot
accidentally publish them as independent products.

## JDK compatibility

The complete library is compiled with `--release 8`, produces Java 8 class
files, and uses only JDK classes at runtime. It contains no Lombok, Guava,
Spring, Caffeine, or other third-party runtime code.

JDK 8 uses the JAR on the ordinary classpath:

```bash
java -cp cn-holiday-kit.jar:app.jar example.Main
```

JDK 9 and later can place the same unchanged JAR on the module path. The
manifest declares the stable automatic module name
`com.github.gmkits.holiday`:

```java
module example.app {
    requires com.github.gmkits.holiday;
}
```

An automatic module is intentional here. A physical `module-info.class` cannot
be compiled by a pure JDK 8 build without adding a second JDK toolchain or a
multi-release JAR. The manifest approach preserves one reproducible Java 8
artifact while still giving modern runtimes a stable module name.

## Documentation contract

All public library classes, methods, parameters, return values, exceptional
conditions, and package responsibilities have source Javadoc. The four library
modules run doclint on both JDK 8 and JDK 25 with warnings treated as build
failures.

## Verification

```bash
# Complete Java 8 build, tests, strict Javadocs, and standalone single-JAR test
mvn -B -f java/pom.xml clean verify

# The same command is also a compatibility gate on JDK 17 and JDK 21. On
# JDK 9+, verify additionally compiles and runs the module-path consumer.

# JDK 17/21/25 会额外构建并测试 Spring Boot API；JDK 8 只验证核心库。
mvn -B -f java/pom.xml clean verify

# Inspect the automatic module on JDK 9+ (the verify phase also compiles and
# runs a consumer module with `requires com.github.gmkits.holiday`)
jar --describe-module --file \
  java/cn-holiday-kit/target/cn-holiday-kit-1.0.0-rc1.jar
```

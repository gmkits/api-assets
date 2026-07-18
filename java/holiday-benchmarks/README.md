# Java JMH benchmarks

The benchmarks measure the in-process query engine after bundle warm-up. They do
not include HTTP parsing, JSON serialization, network I/O, rate limiting, or
audit logging.

Recorded runs are kept under [`results`](results/).

From the `java` directory:

```bash
mvn -pl holiday-benchmarks -am clean package -DskipTests
java -jar holiday-benchmarks/target/benchmarks.jar
```

Each fork is fixed to a 4 GiB heap (`-Xms4g -Xmx4g`) and uses G1. Override the
unified date-asset directory when running from another working directory:

```bash
java -Dcn.holiday.assets.path=/absolute/path/to/data/date-assets \
  -jar holiday-benchmarks/target/benchmarks.jar
```

For a quick smoke run:

```bash
java -jar holiday-benchmarks/target/benchmarks.jar \
  -wi 1 -i 2 -w 1s -r 1s
```

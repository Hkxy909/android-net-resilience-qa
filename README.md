# android-net-resilience-qa

Java QA testing tool for checking how mobile sync endpoints handle unstable network connections, HTTP 503 drops, and malformed JSON payloads.

Includes an `AdbNetworkController` wrapper (`adb shell svc wifi` / `airplane-mode`) for toggling device connectivity during live Android tests, plus a built-in local `HttpServer` harness so the JUnit 5 retry and timeout tests run cleanly offline.

## Stack
- Java 17 (`java.net.http.HttpClient`, `HttpServer`, `ProcessBuilder`)
- JUnit 5
- Maven
- Android Debug Bridge (ADB)

## Running the tests
```bash
mvn test
```

## Running the local chaos test demo
```bash
mvn compile exec:java -Dexec.mainClass="com.hkxy.netqa.Main"
```

# Logify

[![Maven Central](https://img.shields.io/maven-central/v/cloud.slashdev.logging/Logify)
](https://central.sonatype.com/artifact/cloud.slashdev.logging/Logify)


**Logify:** Your ultimate logging solution for Java and Kotlin frameworks, seamlessly empowering developers in Android, SpringBoot, and beyond.

The `Logify` class provides a flexible and customizable logging framework in Java.
It includes features for logging messages, exceptions, measuring execution time,
and writing logs to files. The class supports different log levels (DEBUG, INFO, WARNING, ERROR)
and provides methods to customize log output formats and destinations.

**Usage:**
To use Logify, initialize the logger using one of the `initialize()` methods before
logging any messages. You can customize the log behavior by setting loggable levels,
loggable tags, and enabling/disabling logging. Log messages can be sent using the
`d()`, `i()`, `w()`, and `e()` methods with various overloads allowing logging of
messages and exceptions.

**Download:**
```xml
<dependency>
    <groupId>cloud.slashdev.logging</groupId>
    <artifactId>Logify</artifactId>
    <version>3.0.0</version>
</dependency>
```

**Example Usage:**
```java
Logify.initialize("MyApp", "/path/to/logs", true);
Logify.d("Debug message");
Logify.e(exception, "Error occurred");
```

**Features:**
- Customizable log levels: DEBUG, INFO, WARNING, ERROR.
- Logging to console or files with configurable log paths.
- Flexible log message formatting and filtering.
- Stack trace logging and custom tag support.
- Execution time measurement using `measureTimeMillis()` method.

**Note:** To use this class, make sure to initialize the logger using one of the `initialize()`
methods before logging any messages. Logging can be enabled, disabled, or customized using
the provided static methods.
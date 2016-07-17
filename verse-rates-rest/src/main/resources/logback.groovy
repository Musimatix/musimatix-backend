import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.core.ConsoleAppender
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.core.rolling.RollingFileAppender
import ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy
import ch.qos.logback.classic.filter.ThresholdFilter
import ch.qos.logback.core.util.FileSize

import static ch.qos.logback.classic.Level.ERROR
import static ch.qos.logback.classic.Level.INFO
import static ch.qos.logback.classic.Level.WARN

appender("STDOUT", ConsoleAppender) {
  encoder(PatternLayoutEncoder) {
    pattern = "%d{HH:mm:ss.SSS} [%thread] %-5level %logger{5} - %msg%n"
  }
}

appender("ROLLING_PROSODY", RollingFileAppender) {
  rollingPolicy(SizeAndTimeBasedRollingPolicy) {
    fileNamePattern = "/var/log/msmx/msmx-prosody-%d{yyyy-MM-dd}.%i.txt"
    maxFileSize = "50MB"
    maxHistory = 10
    totalSizeCap = FileSize.valueOf("1GB")
  }
  encoder(PatternLayoutEncoder) {
    pattern = "%d{HH:mm:ss.SSS} [%thread] %-5level %logger{5} - %msg%n"
  }
}

appender("ROLLING_SERVER", RollingFileAppender) {
  rollingPolicy(SizeAndTimeBasedRollingPolicy) {
    fileNamePattern = "/var/log/msmx/msmx-server-%d{yyyy-MM-dd}.%i.txt"
    maxFileSize = "50MB"
    maxHistory = 10
    totalSizeCap = FileSize.valueOf("1GB")
    filter(ThresholdFilter) {
      level = logLevel
    }
  }
  encoder(PatternLayoutEncoder) {
    pattern = "%d{HH:mm:ss.SSS} [%thread] %-5level %logger{5} - %msg%n"
  }
}

logger("VerseProcessor", INFO, ["ROLLING_PROSODY"])
root(INFO, ["ROLLING_SERVER"])

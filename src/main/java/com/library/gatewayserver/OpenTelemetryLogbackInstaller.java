package com.library.gatewayserver;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class OpenTelemetryLogbackInstaller implements ApplicationListener<ApplicationReadyEvent> {

    private final OpenTelemetry openTelemetry;

    public OpenTelemetryLogbackInstaller(OpenTelemetry openTelemetry) {
        this.openTelemetry = openTelemetry;
    }

    @Override
    public void onApplicationEvent(@NonNull ApplicationReadyEvent event) {
        OpenTelemetryAppender.install(openTelemetry);
    }
}
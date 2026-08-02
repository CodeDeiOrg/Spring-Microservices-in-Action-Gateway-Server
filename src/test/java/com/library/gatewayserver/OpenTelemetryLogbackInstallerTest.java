package com.library.gatewayserver;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.context.event.ApplicationReadyEvent;

import static org.mockito.Mockito.mockStatic;

@ExtendWith(MockitoExtension.class)
class OpenTelemetryLogbackInstallerTest {

    @Mock OpenTelemetry openTelemetry;
    @Mock ApplicationReadyEvent event;

    @Test
    void onApplicationEvent_installsAppenderWithInjectedOpenTelemetryInstance() {
        OpenTelemetryLogbackInstaller installer = new OpenTelemetryLogbackInstaller(openTelemetry);

        try (MockedStatic<OpenTelemetryAppender> appenderMock = mockStatic(OpenTelemetryAppender.class)) {
            installer.onApplicationEvent(event);

            appenderMock.verify(() -> OpenTelemetryAppender.install(openTelemetry));
        }
    }
}

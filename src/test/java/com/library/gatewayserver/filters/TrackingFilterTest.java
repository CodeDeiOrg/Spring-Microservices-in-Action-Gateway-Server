package com.library.gatewayserver.filters;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TrackingFilterTest {

    @Mock Tracer tracer;
    @Mock GatewayFilterChain chain;

    private TrackingFilter filter;

    @BeforeEach
    void setUp() {
        filter = new TrackingFilter(tracer);
    }

    @Test
    void filter_withCurrentSpan_delegatesToChain() {
        Span span = mock(Span.class);
        TraceContext context = mock(TraceContext.class);
        given(context.traceId()).willReturn("abc123");
        given(span.context()).willReturn(context);
        given(tracer.currentSpan()).willReturn(span);
        given(chain.filter(org.mockito.ArgumentMatchers.any())).willReturn(Mono.empty());

        ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/books").build());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        verify(chain).filter(exchange);
    }

    @Test
    void filter_withNoCurrentSpan_stillDelegatesToChain() {
        given(tracer.currentSpan()).willReturn(null);
        given(chain.filter(org.mockito.ArgumentMatchers.any())).willReturn(Mono.empty());

        ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/books").build());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        verify(chain).filter(exchange);
    }
}

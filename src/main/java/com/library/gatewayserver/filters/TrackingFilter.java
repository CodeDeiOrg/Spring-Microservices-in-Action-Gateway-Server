package com.library.gatewayserver.filters;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Order(1)
@Component
public class TrackingFilter implements GlobalFilter {
    private static final Logger logger = LoggerFactory.getLogger(TrackingFilter.class);
    private final Tracer tracer;

    public TrackingFilter(Tracer tracer) {
        this.tracer = tracer;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        final String traceId = Optional.ofNullable(tracer.currentSpan())
                .map(Span::context)
                .map(context -> context.traceId())
                .orElse("null");

        logger.debug("Trace id found in tracking filter GATEWAY: {}. ", traceId);

        return chain.filter(exchange);
    }

}

package com.demo.wideevents.filter;

import com.demo.wideevents.domain.WideEvent;
import com.demo.wideevents.domain.WideEventContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static net.logstash.logback.argument.StructuredArguments.kv;

@Slf4j
@Component
@RequiredArgsConstructor
public class WideEventFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        if (request.getRequestURI().startsWith("/actuator")) {
            filterChain.doFilter(request, response);
            return;
        }

        var requestId = UUID.randomUUID().toString().substring(0, 8);
        var traceId = UUID.randomUUID().toString().replace("-", "");
        var startTime = System.currentTimeMillis();

        MDC.put("request_id", requestId);
        MDC.put("trace_id", traceId);

        var builder = WideEvent.builder()
                .requestId(requestId)
                .traceId(traceId)
                .timestamp(Instant.now().toString())
                .service("checkout-service")
                .version("1.0.0")
                .region("us-east-1")
                .environment("production")
                .httpMethod(request.getMethod())
                .httpPath(request.getRequestURI());

        WideEventContext.init(builder);

        try {
            filterChain.doFilter(request, response);
            builder.httpStatus(response.getStatus());
            builder.outcome(response.getStatus() < 400 ? "success" : "error");
        } catch (Exception e) {
            builder.httpStatus(500)
                   .outcome("error")
                   .errorType(e.getClass().getSimpleName())
                   .errorMessage(e.getMessage());
            throw e;
        } finally {
            builder.durationMs(System.currentTimeMillis() - startTime);
            emitWideEvent(builder.build());
            WideEventContext.clear();
            MDC.clear();
        }
    }

    @SuppressWarnings("unchecked")
    private void emitWideEvent(WideEvent event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            Map<String, Object> fields = objectMapper.readValue(json, Map.class);

            // Flatten feature_flags
            Object flags = fields.remove("feature_flags");
            if (flags instanceof Map<?, ?> flagMap) {
                flagMap.forEach((k, v) -> fields.put("ff_" + k, String.valueOf(v)));
            }

            fields.forEach((k, v) -> {
                if (v != null) {
                    MDC.put(k, String.valueOf(v));
                }
            });

            log.info("wide_event");
        } catch (Exception e) {
            log.error("Failed to emit wide event", e);
        }
    }
}

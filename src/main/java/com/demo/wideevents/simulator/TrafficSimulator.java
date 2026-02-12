package com.demo.wideevents.simulator;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Component
@ConditionalOnProperty(name = "simulator.enabled", havingValue = "true")
public class TrafficSimulator {

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Scheduled(fixedRateString = "#{${simulator.requests-per-second} > 0 ? (1000 / ${simulator.requests-per-second}) : 1000}")
    public void simulateTraffic() {
        try {
            var userId = "user_" + ThreadLocalRandom.current().nextInt(1000, 9999);

            var request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/checkout"))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .header("X-User-Id", userId)
                    .header("Content-Type", "application/json")
                    .build();

            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            // Silently ignore - simulator shouldn't crash the app
        }
    }
}

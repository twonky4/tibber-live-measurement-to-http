package de.viseit.tibber.controller;

import static java.lang.String.format;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.framing.CloseFrame;
import org.java_websocket.handshake.ServerHandshake;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.RequestEntity;
import org.springframework.http.RequestEntity.BodyBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.viseit.tibber.domain.messaging.ConnectionAck;
import de.viseit.tibber.domain.messaging.ConnectionInit;
import de.viseit.tibber.domain.messaging.NextMessage;
import de.viseit.tibber.domain.messaging.NextMessage.DayPrices;
import de.viseit.tibber.domain.messaging.NextMessage.LiveMeasurement;
import de.viseit.tibber.domain.messaging.Subscription;
import de.viseit.tibber.service.JsonConverterService;
import de.viseit.tibber.service.MessageReaderService;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
@RestController
public class LiveMeasurementController {
    private static final BigDecimal EURO_IN_CENT = BigDecimal.valueOf(100);
    private final ObjectMapper mapper;
    private final JsonConverterService converter;
    private final MessageReaderService reader;

    @Value("${app.tibber.token}")
    private String token;
    @Value("${app.tibber.websocket-url}")
    private String websocketUrl;
    @Value("${app.tibber.url}")
    private String url;
    @Value("${app.tibber.home-id}")
    private String homeId;

    private WebSocketChatClient client;
    private BigDecimal price;
    private int calls = 0;
    private final List<BigDecimal> dayPrices = new ArrayList<>();

    @GetMapping("/api/v1/live-measurement")
    public LiveMeasurement getData() throws URISyntaxException, InterruptedException, IOException {
        connectIfNeeded();

        synchronized (this) {
            if (client == null) {
                log.error("no connection established");
                return null;
            }
            LiveMeasurement liveMeasurement;
            liveMeasurement = client.getLiveMeasurement();
            if (liveMeasurement != null) {
                liveMeasurement.setPrice(price);
                liveMeasurement.setDayPrices(buildDayPrices());

                if (liveMeasurement.getTimestamp().isBefore(OffsetDateTime.now().minusSeconds(10))) {
                    calls++;
                    log.warn("no new data since last {} call(s)", calls);
                    if (liveMeasurement.getTimestamp().isBefore(OffsetDateTime.now().minusMinutes(10))) {
                        calls = 0;
                        client.closeConnection(CloseFrame.SERVICE_RESTART, "no new values");
                        client = null;
                        liveMeasurement = null;
                    }
                } else {
                    if (calls > 0) {
                        log.info("got new values in the mean time");
                    }
                    calls = 0;
                }
            }
            return liveMeasurement;
        }
    }

    private DayPrices buildDayPrices() {
        DayPrices prices = new DayPrices();

        try {
            prices.setPrice00(dayPrices.get(0));
            prices.setPrice01(dayPrices.get(1));
            prices.setPrice02(dayPrices.get(2));
            prices.setPrice03(dayPrices.get(3));
            prices.setPrice04(dayPrices.get(4));
            prices.setPrice05(dayPrices.get(5));
            prices.setPrice06(dayPrices.get(6));
            prices.setPrice07(dayPrices.get(7));
            prices.setPrice08(dayPrices.get(8));
            prices.setPrice09(dayPrices.get(9));
            prices.setPrice10(dayPrices.get(10));
            prices.setPrice11(dayPrices.get(11));
            prices.setPrice12(dayPrices.get(12));
            prices.setPrice13(dayPrices.get(13));
            prices.setPrice14(dayPrices.get(14));
            prices.setPrice15(dayPrices.get(15));
            prices.setPrice16(dayPrices.get(16));
            prices.setPrice17(dayPrices.get(17));
            prices.setPrice18(dayPrices.get(18));
            prices.setPrice19(dayPrices.get(19));
            prices.setPrice20(dayPrices.get(20));
            prices.setPrice21(dayPrices.get(21));
            prices.setPrice22(dayPrices.get(22));
            prices.setPrice23(dayPrices.get(23));
        } catch (Exception e) {
            log.error("some values are missing", e);
        }
        return prices;
    }

    private synchronized void connectIfNeeded() throws InterruptedException, URISyntaxException {
        synchronized (this) {
            if (client != null) {
                return;
            }

            log.info("connect");
            client = new WebSocketChatClient(new URI(websocketUrl), converter, reader, token, homeId);
            client.connectBlocking(1, MINUTES);
        }

        boolean connected = false;
        for (int i = 0; i < 60; i++) {
            LiveMeasurement liveMeasurement = null;
            synchronized (this) {
                if (client != null) {
                    liveMeasurement = client.getLiveMeasurement();
                }
            }
            if (liveMeasurement != null) {
                connected = true;
                log.info("needed {} seconds to connect", i);
                break;
            }
            TimeUnit.SECONDS.sleep(1);
        }

        if (connected) {
            log.info("connected");
        } else {
            log.error("connection not possible within a minute");
            synchronized (this) {
                if (client != null) {
                    client.closeConnection(CloseFrame.SERVICE_RESTART, "no connected");
                    client = null;
                }
            }
        }
    }

    @PostConstruct
    @Scheduled(cron = "2 0 * * * *")
    public void loadPrice() {
        RestTemplate rest = new RestTemplate();

        String body = "{\"query\":\"{\\n  viewer {\\n    homes {\\n      currentSubscription{\\n        priceInfo{\\n          current{\\n            total\\n          }\\n        }\\n      }\\n    }\\n  }\\n}\\n\"}";
        RequestEntity<String> request = buildRequest()
                .body(body);
        ResponseEntity<String> response = rest.exchange(request, String.class);

        try {
            JsonNode node = mapper.readTree(response.getBody());

            price = BigDecimal.valueOf(node
                    .get("data")
                    .get("viewer")
                    .get("homes")
                    .get(0)
                    .get("currentSubscription")
                    .get("priceInfo")
                    .get("current")
                    .get("total")
                    .asDouble());

            log.info("💵 {}", price.toPlainString());
        } catch (JsonProcessingException e) {
            log.error("parser error", e);

            price = null;
        }
    }

    @PostConstruct
    @Scheduled(cron = "2 0 * * * *")
    public void loadDayPrice() {
        RestTemplate rest = new RestTemplate();

        String body = "{\"query\":\"{\\n  viewer {\\n    homes {\\n      currentSubscription{\\n        priceInfo{\\n          today {\\n            total\\n            startsAt\\n          }\\n          tomorrow {\\n            total\\n            startsAt\\n          }\\n        }\\n      }\\n    }\\n  }\\n}\\n\"}";
        RequestEntity<String> request = buildRequest()
                .body(body);
        ResponseEntity<String> response = rest.exchange(request, String.class);

        dayPrices.clear();

        try {
            JsonNode node = mapper.readTree(response.getBody());

            OffsetDateTime now = OffsetDateTime.now().truncatedTo(HOURS);

            JsonNode today = node
                    .get("data")
                    .get("viewer")
                    .get("homes")
                    .get(0)
                    .get("currentSubscription")
                    .get("priceInfo")
                    .get("today");

            int todaySize = today.size();
            for (int i = 0; i < todaySize; i++) {
                JsonNode hour = today.get(i);
                BigDecimal total = BigDecimal.valueOf(hour.get("total").asDouble())
                        .multiply(EURO_IN_CENT)
                        .stripTrailingZeros();
                OffsetDateTime startsAt = OffsetDateTime.parse(hour.get("startsAt").asText());

                if (startsAt.isBefore(now)) {
                    continue;
                }

                dayPrices.add(total);
            }

            JsonNode tomorrow = node
                    .get("data")
                    .get("viewer")
                    .get("homes")
                    .get(0)
                    .get("currentSubscription")
                    .get("priceInfo")
                    .get("tomorrow");

            int tomorrowSize = tomorrow.size();
            for (int i = 0; i < tomorrowSize; i++) {
                BigDecimal total = BigDecimal.valueOf(tomorrow.get(i).get("total").asDouble())
                        .multiply(EURO_IN_CENT)
                        .stripTrailingZeros();

                dayPrices.add(total);
            }

            log.info("{} daily prices loaded", dayPrices.size());

            int loadedSize = dayPrices.size();
            BigDecimal last = dayPrices.get(dayPrices.size() - 1);
            for (int i = loadedSize; i < 24; i++) {
                dayPrices.add(last);
            }
        } catch (JsonProcessingException e) {
            log.error("parser error", e);
        }
    }

    private BodyBuilder buildRequest() {
        return RequestEntity
                .post(URI.create("https://api.tibber.com/v1-beta/gql"))
                .accept(APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json");
    }

    public class WebSocketChatClient extends WebSocketClient {
        private final JsonConverterService converter;
        private final MessageReaderService reader;
        private final String token;
        private final String homeId;
        @Getter
        private LiveMeasurement liveMeasurement;
        private OffsetDateTime lastApiTimestamp = OffsetDateTime.now();

        public WebSocketChatClient(URI uri, JsonConverterService converter, MessageReaderService reader, String token, String homeId) {
            super(uri, Map.of(
                    "cache-control", "no-cache",
                    "content-type", "application/json",
                    "User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/111.0.0.0 Safari/537.36",
                    "Authorization", "Bearer " + token,
                    "Sec-WebSocket-Protocol", "graphql-transport-ws"));
            this.converter = converter;
            this.reader = reader;
            this.token = token;
            this.homeId = homeId;
        }

        @Override
        public void onOpen(ServerHandshake handshakedata) {
            log.info("open");

            send(converter.convertMessage(new ConnectionInit(token)));
        }

        @Override
        public void onMessage(String messageStr) {
            log.debug("got: {}", messageStr);
            Object msg = reader.read(messageStr);

            if (msg instanceof ConnectionAck) {
                Subscription subscription = Subscription.builder()
                        .query(format(
                                "subscription {%n  liveMeasurement(homeId: \"%s\") {%n    timestamp%n    power%n    powerProduction%n    lastMeterConsumption%n    lastMeterProduction%n  }%n}",
                                homeId))
                        .build();
                String message = converter.convertMessage(subscription);
                log.info("subscription message send");
                send(message);
            } else if (msg instanceof NextMessage nextNessage) {
                if (nextNessage.getData() != null) {
                    synchronized (this) {
                        liveMeasurement = nextNessage.getData().getLiveMeasurement();

                        if (lastApiTimestamp.equals(liveMeasurement.getTimestamp())) {
                            log.warn("got same value as last time, fake new data");
                            liveMeasurement.setTimestamp(OffsetDateTime.now());
                        } else {
                            log.info("↓ {} ↑ {}", liveMeasurement.getPower(), liveMeasurement.getPowerProduction());
                            lastApiTimestamp = liveMeasurement.getTimestamp();
                        }
                    }
                } else {
                    log.warn("got no data {}", messageStr);
                }
            } else {
                log.error("was not able to read message {}", messageStr);
            }
        }

        @Override
        public void onClose(int code, String reason, boolean remote) {
            log.warn("connection closed code={}, reason={}, remote={}", code, reason, remote);

            synchronized (LiveMeasurementController.this) {
                LiveMeasurementController.this.client = null;
            }
        }

        @Override
        public void onError(Exception e) {
            log.error("error", e);
            synchronized (LiveMeasurementController.this) {
                LiveMeasurementController.this.client = null;
            }
            closeConnection(CloseFrame.SERVICE_RESTART, "got error");
        }
    }
}

package ee.ooloros.sandbox.guavacachedemo.props;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;

import ee.ooloros.sandbox.guavacachedemo.common.StaleWhileRevalidateCacheLoader;
import lombok.extern.slf4j.Slf4j;

import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@SpringBootTest
@AutoConfigureMockMvc
@Execution(ExecutionMode.SAME_THREAD)
@ContextConfiguration(initializers = WiremockInitializer.class)
class PropsControllerIntegrationTest {

    private static final String EXAMPLE_RESPONSE_JSON_VALUE = "123";

    @Autowired(required = false)
    private WireMockServer wireMockServer;
    @Autowired
    private PropsCacheService propsCacheService;

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void reset() {
        wireMockServer.resetAll();
    }

    @Nested
    class NoEntryInCache {

        @Test
        void shouldReturnContent_whenSourceIsOk() throws Exception {
            var key = UUID.randomUUID().toString();
            wireMockServer.stubFor(
                    WireMock.get("/api/props/" + key)
                            .willReturn(okJson(EXAMPLE_RESPONSE_JSON_VALUE).withFixedDelay(1_000)));

            mockMvc.perform(get("/api/props/{key}", key))
                    .andExpect(status().isOk())
                    .andExpect(content().string(EXAMPLE_RESPONSE_JSON_VALUE));
        }

        @Test
        void shouldReturn500_whenSourceIsDown() throws Exception {
            var key = UUID.randomUUID().toString();
            wireMockServer.stubFor(
                    WireMock.get("/api/props/" + key)
                            .willReturn(serverError().withBody(key).withFixedDelay(1_000)));

            mockMvc.perform(get("/api/props/{key}", key))
                    .andExpect(status().is5xxServerError());
        }
    }

    @Nested
    class FreshEntryInCache {

        @Test
        void shouldReturnContent() throws Exception {
            var key = UUID.randomUUID().toString();
            wireMockServer.stubFor(
                    WireMock.get("/api/props/" + key)
                            .willReturn(okJson(EXAMPLE_RESPONSE_JSON_VALUE).withFixedDelay(1_000)));

            propsCacheService.setValue(key, EXAMPLE_RESPONSE_JSON_VALUE);
            mockMvc.perform(get("/api/props/{key}", key))
                    .andExpect(status().isOk())
                    .andExpect(content().string(EXAMPLE_RESPONSE_JSON_VALUE));
            wireMockServer.verify(0, getRequestedFor(urlEqualTo("/api/props/" + key)));
        }
    }

    @Nested
    class StaleEntryInCache {

        @Test
        void shouldReturnContent_whenServerIsUp() throws Exception {
            var key = UUID.randomUUID().toString();
            wireMockServer.stubFor(
                    WireMock.get("/api/props/" + key)
                            .willReturn(okJson(EXAMPLE_RESPONSE_JSON_VALUE).withFixedDelay(1_000)));

            propsCacheService.setValue(key, EXAMPLE_RESPONSE_JSON_VALUE);
            Thread.sleep(StaleWhileRevalidateCacheLoader.DURATION_FRESH);
            mockMvc.perform(get("/api/props/{key}", key))
                    .andExpect(status().isOk())
                    .andExpect(content().string(EXAMPLE_RESPONSE_JSON_VALUE));

            List<LoggedRequest> all = wireMockServer.findAll(getRequestedFor(urlEqualTo("/api/props/" + key)));
            log.info("verify wiremock, TODO: " + all.size());
            //FIXME: it does not fail?!?
            wireMockServer.verify(0, getRequestedFor(urlEqualTo("/api/props/" + key)));
            Thread.sleep(2_000);//wait async loading to finish
            wireMockServer.verify(1, getRequestedFor(urlEqualTo("/api/props/" + key)));

            log.info("Value is now fresh");
            mockMvc.perform(get("/api/props/{key}", key))
                    .andExpect(status().isOk())
                    .andExpect(content().string(EXAMPLE_RESPONSE_JSON_VALUE));
            Thread.sleep(2_000);//wait for potential async loading to finish
            wireMockServer.verify(1, getRequestedFor(urlEqualTo("/api/props/" + key)));
        }

        @Test
        void shouldReturnContent_whenServerDown() throws Exception {
            var key = UUID.randomUUID().toString();
            wireMockServer.stubFor(
                    WireMock.get("/api/props/" + key)
                            .willReturn(serverError().withBody(key).withFixedDelay(1_000)));

            propsCacheService.setValue(key, EXAMPLE_RESPONSE_JSON_VALUE);
            Thread.sleep(StaleWhileRevalidateCacheLoader.DURATION_FRESH);

            mockMvc.perform(get("/api/props/{key}", key))
                    .andExpect(status().isOk())
                    .andExpect(content().string(EXAMPLE_RESPONSE_JSON_VALUE));

            List<LoggedRequest> all = wireMockServer.findAll(getRequestedFor(urlEqualTo("/api/props/" + key)));
            log.info("verify wiremock, TODO: " + all.size() +" " + all);
            //FIXME: it fails
            wireMockServer.verify(0, getRequestedFor(urlEqualTo("/api/props/" + key)));
            Thread.sleep(2_000);//wait async loading to finish
            wireMockServer.verify(1, getRequestedFor(urlEqualTo("/api/props/" + key)));

            log.info("Value is still stale");
            mockMvc.perform(get("/api/props/{key}", key))
                    .andExpect(status().isOk())
                    .andExpect(content().string(EXAMPLE_RESPONSE_JSON_VALUE));
            Thread.sleep(2_000);//wait async loading to finish
            wireMockServer.verify(2, getRequestedFor(urlEqualTo("/api/props/" + key)));
        }
    }

    @Nested
    class ExpiredEntryInCache {

        @Test
        void shouldReturnContent_whenServerIsUp() throws Exception {
            var key = UUID.randomUUID().toString();
            wireMockServer.stubFor(
                    WireMock.get("/api/props/" + key)
                            .willReturn(okJson(EXAMPLE_RESPONSE_JSON_VALUE).withFixedDelay(1_000)));

            propsCacheService.setValue(key, EXAMPLE_RESPONSE_JSON_VALUE);
            Thread.sleep(StaleWhileRevalidateCacheLoader.DURATION_EXPIRED);
            mockMvc.perform(get("/api/props/{key}", key))
                    .andExpect(status().isOk())
                    .andExpect(content().string(EXAMPLE_RESPONSE_JSON_VALUE));
            wireMockServer.verify(1, getRequestedFor(urlEqualTo("/api/props/" + key)));

            log.info("Value is now fresh");
            mockMvc.perform(get("/api/props/{key}", key))
                    .andExpect(status().isOk())
                    .andExpect(content().string(EXAMPLE_RESPONSE_JSON_VALUE));
            Thread.sleep(2_000);//wait for potential async loading to finish
            wireMockServer.verify(1, getRequestedFor(urlEqualTo("/api/props/" + key)));
        }

        @Test
        void shouldReturn500_whenSourceIsDown() throws Exception {
            var key = UUID.randomUUID().toString();
            wireMockServer.stubFor(
                    WireMock.get("/api/props/" + key)
                            .willReturn(serverError().withBody(key).withFixedDelay(1_000)));

            propsCacheService.setValue(key, EXAMPLE_RESPONSE_JSON_VALUE);
            Thread.sleep(StaleWhileRevalidateCacheLoader.DURATION_EXPIRED);

            mockMvc.perform(get("/api/props/{key}", key))
                    .andExpect(status().is5xxServerError());
            wireMockServer.verify(1, getRequestedFor(urlEqualTo("/api/props/" + key)));
        }
    }

}
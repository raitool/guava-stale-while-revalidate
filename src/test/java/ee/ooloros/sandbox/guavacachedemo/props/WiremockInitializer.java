package ee.ooloros.sandbox.guavacachedemo.props;

import java.util.Map;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextClosedEvent;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.common.Slf4jNotifier;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

public class WiremockInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext context) {
        var server = initializeServer();
        server.start();

        var factory = context.getBeanFactory();
        factory.registerSingleton("wiremockServer.remoteProps", server);

        context.addApplicationListener(applicationEvent -> {
            if (applicationEvent instanceof ContextClosedEvent) {
                server.stop();
            }
        });
        TestPropertyValues.of(Map.of(
                "provider.baseUrl",
                "http://localhost:%d".formatted(server.port()))).applyTo(context);

    }

    private static WireMockServer initializeServer() {
        return new WireMockServer(new WireMockConfiguration()
                                          .dynamicPort()
                                          .notifier(new Slf4jNotifier(true)));
    }

}

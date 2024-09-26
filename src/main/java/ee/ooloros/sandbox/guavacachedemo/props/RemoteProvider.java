package ee.ooloros.sandbox.guavacachedemo.props;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class RemoteProvider {

    @Value("${provider.baseUrl}")
    private String providerBaseUrl;

    private final RestTemplate restTemplate;

    public RemoteProvider() {
        restTemplate = new RestTemplateBuilder().build();
    }

    public String getByKey(String key) {
        log.info("GET key {}", key);
        return restTemplate.getForObject(providerBaseUrl + "/api/props/" + key, String.class);
    }
}

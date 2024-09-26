package ee.ooloros.sandbox.guavacachedemo.props;

import org.springframework.stereotype.Service;

import ee.ooloros.sandbox.guavacachedemo.common.StaleWhileRevalidateCacheLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PropsCacheService extends StaleWhileRevalidateCacheLoader<String, String> {

    private final RemoteProvider remoteProvider;

    @Override
    protected String loadValueFromSource(String key) {
        return remoteProvider.getByKey(key);
    }

    @Override
    public String getValue(String key) {
        log.info("get value");
        return super.getValue(key);
    }

    @Override
    public void setValue(String key, String value) {
        log.info("set key {} to {}", key, value);
        super.setValue(key, value);
    }
}

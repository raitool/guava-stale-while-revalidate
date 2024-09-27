package ee.ooloros.sandbox.guavacachedemo.common;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;

import lombok.extern.slf4j.Slf4j;

import static com.google.common.cache.CacheLoader.asyncReloading;

@Slf4j
public abstract class StaleWhileRevalidateCacheLoader<T, U> {

    public static final Duration DURATION_FRESH = Duration.ofSeconds(10);
    public static final Duration DURATION_EXPIRED = Duration.ofSeconds(20);

    private final ExecutorService executorService = Executors.newCachedThreadPool();

    private final Cache<T, U> cache = CacheBuilder.newBuilder()
            .refreshAfterWrite(DURATION_FRESH)
            .expireAfterWrite(DURATION_EXPIRED)
            .build(asyncReloading(
                    CacheLoader.from(this::loadValueFromSource),
                    executorService));

    public void setValue(T key, U value) {
        cache.put(key, value);
    }

    public U getValue(T key) {
        return getStaleWhileRevalidateValue(key)
                .orElseGet(() -> getValueBlocking(key));
    }

    protected abstract U loadValueFromSource(T key);

    private Optional<U> getStaleWhileRevalidateValue(T key) {
        return Optional.ofNullable(cache.getIfPresent(key));
    }

    private U getValueBlocking(T key) {
        try {
            return cache.get(key, () -> loadValueFromSource(key));
        } catch (ExecutionException e) {
            throw new IllegalStateException(e);
        }
    }
}

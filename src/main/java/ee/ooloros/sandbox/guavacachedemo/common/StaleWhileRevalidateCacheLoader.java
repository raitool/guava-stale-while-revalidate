package ee.ooloros.sandbox.guavacachedemo.common;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;

import static com.google.common.cache.CacheLoader.asyncReloading;

/**
 * non-blocking guava cache with 3 use-cases:
 * <table>
 *     <tr>
 *         <th>Period</th>
 *         <th>Comment</th>
 *     </tr>
 *     <tr>
 *         <td>x < 10sec</td>
 *         <td>FRESH value in cache<br/>return immediately, do not trigger async (non-blocking) refresh</td>
 *     </tr>
 *     <tr>
 *         <td>10sec <= x < 20sec</td>
 *         <td>STALE value in cache<br/>return immediately, but trigger async (non-blocking) refresh</td>
 *     </tr>
 *     <tr>
 *         <td>20sec <= x</td>
 *         <td>EXPIRED value in cache<br/>blocking refresh from source. Same as no value in cache</td>
 *     </tr>
 * </table>
 */
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
        return getValueNonBlocking(key)
                .orElseGet(() -> getValueBlocking(key));
    }

    protected abstract U loadValueFromSource(T key);

    /**
     * either FRESH or STALE_WHILE_REVALIDATE
     * */
    private Optional<U> getValueNonBlocking(T key) {
        return Optional.ofNullable(cache.getIfPresent(key));
    }

    /**
     * either no value in cache or expired
     */
    private U getValueBlocking(T key) {
        try {
            return cache.get(key, () -> loadValueFromSource(key));
        } catch (ExecutionException e) {
            throw new IllegalStateException(e);
        }
    }
}

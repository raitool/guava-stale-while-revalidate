package ee.ooloros.sandbox.guavacachedemo.props;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/props")
public class PropsController {
    private final PropsCacheService propsCacheService;

    @GetMapping("/{key}")
    String getByKey(@PathVariable("key") String key) {
        var resp = propsCacheService.getValue(key);
        log.info("Response {}", resp);
        return resp;
    }

}

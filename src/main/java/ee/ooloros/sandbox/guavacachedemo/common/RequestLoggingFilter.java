package ee.ooloros.sandbox.guavacachedemo.common;


import java.io.IOException;
import java.util.UUID;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Order(Integer.MIN_VALUE)
public class RequestLoggingFilter implements Filter {

    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain filter) throws ServletException, IOException {
        var uuid = UUID.randomUUID();
        log.info("Request {} to {}", uuid, ((HttpServletRequest)req).getServletPath());
        filter.doFilter(req, resp);
        log.info("Request {} finished", uuid);

    }
}

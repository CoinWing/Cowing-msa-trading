package cowing.project.cowingmsatrading.scope;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@Order(Integer.MIN_VALUE)
public class QueryCountFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        QueryCountContext.reset();
        try {
            filterChain.doFilter(request, response);
        } finally {
            int count = QueryCountContext.get();
            log.info("SQL count={} method={} uri={}", count, request.getMethod(), request.getRequestURI());
            QueryCountContext.clear();
        }
    }
}


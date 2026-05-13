package com.sky.filter;

import com.sky.context.AuthContext;
import com.sky.context.BaseContext;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

public class TraceIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            MDC.put("traceId", UUID.randomUUID().toString().replace("-", ""));
            MDC.put("requestUri", request.getRequestURI());
            if (BaseContext.getCurrentId() != null) {
                MDC.put("userId", String.valueOf(BaseContext.getCurrentId()));
            }
            if (AuthContext.getCurrentIdentity() != null) {
                MDC.put("identity", AuthContext.getCurrentIdentity());
            }
            filterChain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }
}

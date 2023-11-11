package de.yanwittmann.presentation.aspect;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

@Aspect
@Component
public class ControllerLoggingAspect {

    private final static Logger LOG = LogManager.getLogger(ControllerLoggingAspect.class);

    @Before("execution(* de.yanwittmann.presentation.controller.*.*(..))")
    public void logBefore(JoinPoint joinPoint) {
        final ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        final HttpServletRequest request = attributes.getRequest();
        LOG.info("{} {}", request.getMethod(), request.getRequestURI());
    }
}

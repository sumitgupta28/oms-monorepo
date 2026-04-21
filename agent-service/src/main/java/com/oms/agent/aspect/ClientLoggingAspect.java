package com.oms.agent.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Aspect
@Component
@Slf4j
public class ClientLoggingAspect {

    @Around("execution(public * com.oms.agent.client.*.*(..))")
    public Object logClientCall(ProceedingJoinPoint pjp) throws Throwable {
        String method = pjp.getSignature().getDeclaringType().getSimpleName()
                + "." + pjp.getSignature().getName();

        log.debug("[{}] >> request args: {}", method, Arrays.toString(pjp.getArgs()));

        try {
            Object result = pjp.proceed();
            log.debug("[{}] << response: {}", method, result);
            return result;
        } catch (Throwable ex) {
            log.debug("[{}] << exception: {}", method, ex.getMessage());
            throw ex;
        }
    }
}

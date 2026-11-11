package com.ll.demo03.global.aspect;

import com.ll.demo03.global.rq.Rq;
import com.ll.demo03.global.rsData.RsData;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class ResponseAspect {
    private final Rq rq;

    @Around("""
            (
                within
                (
                    @org.springframework.web.bind.annotation.RestController *
                )
                &&
                (
                    @annotation(org.springframework.web.bind.annotation.GetMapping)
                    ||
                    @annotation(org.springframework.web.bind.annotation.PostMapping)
                    ||
                    @annotation(org.springframework.web.bind.annotation.PutMapping)
                    ||
                    @annotation(org.springframework.web.bind.annotation.DeleteMapping)
                )
            )
            ||
            @annotation(org.springframework.web.bind.annotation.ResponseBody)
            """)
    public Object handleResponse(ProceedingJoinPoint joinPoint) throws Throwable {
        Object proceed = joinPoint.proceed();

        if (proceed instanceof RsData) {
            RsData<?> rsData = (RsData<?>) proceed;
            rq.setStatusCode(rsData.getStatusCode());
            return rsData;
        }

        return proceed;
    }
}
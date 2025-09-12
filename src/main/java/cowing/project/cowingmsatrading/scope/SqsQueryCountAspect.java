package cowing.project.cowingmsatrading.scope;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class SqsQueryCountAspect {

    @Around("@annotation(io.awspring.cloud.sqs.annotation.SqsListener)")
    public Object aroundSqsListener(ProceedingJoinPoint pjp) throws Throwable {
        QueryCountContext.reset();
        try {
            return pjp.proceed();
        } finally {
            int count = QueryCountContext.get();
            log.info("SQS handler SQL count={}", count);
            QueryCountContext.clear();
        }
    }
}


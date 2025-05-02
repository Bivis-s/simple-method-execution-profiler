package by.bivis.metrics;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

@Aspect
@Slf4j(topic = "METHOD_METRICS")
public class MethodMetricsAspect {

    @Pointcut("execution(* by.bivis..*(..))")
    public void inPackageSelectedForGetMetrics() {
    }

    @Around("inPackageSelectedForGetMetrics()")
    public Object logExecutionTime(ProceedingJoinPoint jp) throws Throwable {
        long start = System.currentTimeMillis();
        try {
            return jp.proceed();
        } finally {
            long duration = System.currentTimeMillis() - start;
            log.info("{}.{} executed in {} ms",
                    jp.getSignature().getDeclaringType().getSimpleName(),
                    jp.getSignature().getName(),
                    duration);
        }
    }
}

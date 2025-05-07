package by.bivis.influx_aspect_profiler;

import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.ConstructorSignature;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Modifier;
import java.time.Instant;
import java.util.Arrays;
import java.util.stream.Collectors;

@Aspect
public class TimingAspect {

    @Pointcut("execution(* by.bivis..*(..))" +
            "&& !within(by.bivis.metrics.CloseInfluxClientRunnable)" +
            "&& !call(* by.bivis.metrics.CloseInfluxClientRunnable.*(..))")
    public void everyMemberInProject() {
    }

    @Around("everyMemberInProject()")
    public Object timed(ProceedingJoinPoint pjp) throws Throwable {
        long start = System.nanoTime();
        Object result;
        try {
            result = pjp.proceed();
            return result;
        } finally {
            long duration = System.nanoTime() - start;

            String className = pjp.getSignature().getDeclaringTypeName();
            String name = pjp.getSignature().getName();
            String kind = pjp.getKind();
            boolean isStatic = false;

            if (pjp.getSignature() instanceof MethodSignature ms) {
                isStatic = Modifier.isStatic(ms.getMethod().getModifiers());
            }

            Class<?>[] parameterTypes;
            if (pjp.getSignature() instanceof MethodSignature ms) {
                parameterTypes = ms.getParameterTypes();
            } else if (pjp.getSignature() instanceof ConstructorSignature cs) {
                parameterTypes = cs.getParameterTypes();
            } else {
                parameterTypes = new Class<?>[0];
            }
            int paramCount = parameterTypes.length;
            String paramTypesStr = paramCount > 0
                    ? Arrays.stream(parameterTypes)
                    .map(Class::getSimpleName)
                    .collect(Collectors.joining(", ", "(", ")"))
                    : "";

            Point point = Point
                    .measurement("method_timing")
                    .addTag("class", className)
                    .addTag("name", name)
                    .addTag("kind", kind)
                    .addTag("static", String.valueOf(isStatic))
                    .addTag("param_types", paramTypesStr)
                    .addField("duration_ns", duration)
                    .addField("duration_ns_2", duration)
                    .addField("duration_ns_3", 5555)
                    .time(Instant.now(), WritePrecision.NS);

            InfluxClient.writeApi.writePoint(point);
        }
    }
}
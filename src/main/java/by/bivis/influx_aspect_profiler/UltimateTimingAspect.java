package by.bivis.influx_aspect_profiler;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.WriteApi;
import com.influxdb.client.WriteOptions;
import com.influxdb.client.domain.WritePrecision;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.ConstructorSignature;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Modifier;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.concurrent.*;
import java.util.concurrent.atomic.LongAdder;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

@Aspect
public final class UltimateTimingAspect {

    private static final Logger LOG = Logger.getLogger(UltimateTimingAspect.class.getName());
    private static final int QUEUE_CAPACITY = Integer.getInteger("influx.queue.capacity", 20_000);
    private static final int WORKER_SHUTDOWN_TIMEOUT_SEC = 10;

    @Pointcut("execution(* by.bivis..*(..))" +
            " && !within(by.bivis.influx_aspect_profiler.UltimateTimingAspect+)")
    private void profiled() {
    }

    @Around("profiled()")
    public Object recordTiming(ProceedingJoinPoint pjp) throws Throwable {

        if (!Influx.isEnabled()) {
            return pjp.proceed();
        }

        final var start = System.nanoTime();
        try {
            return pjp.proceed();
        } finally {
            final var duration = System.nanoTime() - start;

            var meta = META_CACHE.computeIfAbsent(pjp.getStaticPart(), Meta::new);
            var epochNanos = System.currentTimeMillis() * 1_000_000L;

            var ev = new MetricEvent(meta, duration, epochNanos);

            if (!EVENT_QUEUE.offer(ev)) {
                DROPPED.increment();
            }
        }
    }

    private static final ConcurrentMap<JoinPoint.StaticPart, Meta> META_CACHE =
            new ConcurrentHashMap<>(10240);

    private record Meta(String prefix) {
        Meta(JoinPoint.StaticPart sp) {
            this(buildPrefix(sp));
        }

        private static String buildPrefix(JoinPoint.StaticPart sp) {
            var sig = sp.getSignature();
            var kind = sp.getKind();
            var cls = sig.getDeclaringTypeName();
            var name = sig.getName();

            var isStatic = false;
            Class<?>[] params;

            if (sig instanceof MethodSignature ms) {
                isStatic = Modifier.isStatic(ms.getMethod().getModifiers());
                params = ms.getParameterTypes();
            } else if (sig instanceof ConstructorSignature cs) {
                params = cs.getParameterTypes();
            } else {
                params = new Class<?>[0];
            }

            var sj = new StringJoiner("\\,", "(", ")");
            for (var p : params) sj.add(p.getSimpleName());

            return "method_timing"
                    + ",class=" + cls
                    + ",name=" + name
                    + ",kind=" + kind
                    + ",static=" + isStatic
                    + ",param_types=" + sj
                    + " duration_ns=";
        }
    }

    private record MetricEvent(Meta meta, long duration, long epochNanos) {
    }

    private static final BlockingQueue<MetricEvent> EVENT_QUEUE =
            new ArrayBlockingQueue<>(QUEUE_CAPACITY);

    private static final LongAdder DROPPED = new LongAdder();

    private static final ExecutorService WORKER =
            Executors.newSingleThreadExecutor(r -> {
                var t = new Thread(r, "influx-metrics-worker");
                t.setDaemon(true);
                return t;
            });

    static {
        WORKER.submit(UltimateTimingAspect::processLoop);
        Runtime.getRuntime().addShutdownHook(new Thread(UltimateTimingAspect::shutdown, "ultimate-aspect-shutdown"));
    }

    private static void processLoop() {
        var builderTL = ThreadLocal.withInitial(() -> new StringBuilder(512));
        WriteApi writeApi = null;

        for (; ; ) {
            try {
                if (writeApi == null) {
                    writeApi = Influx.getWriteApi();
                    if (writeApi == null) {
                        TimeUnit.SECONDS.sleep(1);
                        continue;
                    }
                    var dropped = DROPPED.sumThenReset();
                    if (dropped > 0) {
                        LOG.log(Level.WARNING, "Recovered Influx connection; {0} metrics were dropped.", dropped);
                    }
                }

                var ev = EVENT_QUEUE.take();

                var sb = builderTL.get();
                sb.setLength(0);
                sb.append(ev.meta().prefix)
                        .append(ev.duration())
                        .append('i')
                        .append(' ').append(ev.epochNanos());

                writeApi.writeRecord(WritePrecision.NS, sb.toString());

            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception ex) {
                LOG.log(Level.SEVERE, "Influx write failed, will retry …", ex);
                writeApi = null;
            }
        }
    }

    private static void shutdown() {
        LOG.info("UltimateTimingAspect shutdown initiated (" + ZonedDateTime.now(ZoneOffset.UTC) + ")");

        WORKER.shutdownNow();
        try {
            if (!WORKER.awaitTermination(WORKER_SHUTDOWN_TIMEOUT_SEC, TimeUnit.SECONDS)) {
                LOG.warning("Worker did not terminate in time.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        Influx.close();
        var lost = DROPPED.sum();
        if (lost > 0) {
            LOG.log(Level.WARNING, "Aspect shutdown complete, but {0} metrics were dropped due to queue overflow.", lost);
        } else {
            LOG.info("Aspect shutdown complete – all metrics flushed.");
        }
    }

    private static final class Influx {
        private static final boolean ENABLED;
        private static final InfluxDBClient CLIENT;
        private static final WriteApi WRITE_API;

        static {
            var url = System.getProperty("influx.url", "http://localhost:8086");
            var token = System.getProperty("influx.token", "1YgT-1a0hQ0Cd0G_LbO-hXv9d795eSqYBIRFTy12SVf-LtAtQhD_R33S3b1DjursbMfx4M1djQF-3ttsJxpH9Q==");
            var org = System.getProperty("influx.org", "dev");
            var bucket = System.getProperty("influx.bucket", "ultimate");

            var ok = Stream.of(url, token, org, bucket).allMatch(Objects::nonNull);
            if (!ok) {
                ENABLED = false;
                CLIENT = null;
                WRITE_API = null;
                LOG.warning("InfluxDB disabled – required system properties are missing.");
            } else {
                ENABLED = true;
                CLIENT = InfluxDBClientFactory.create(url, token.toCharArray(), org, bucket);
                var opts = WriteOptions.builder()
                        .batchSize(Integer.getInteger("influx.batchSize", 5000))
                        .flushInterval(Integer.getInteger("influx.flushIntervalMs", 1000))
                        .bufferLimit(Integer.getInteger("influx.bufferLimit", 50_000))
                        .retryInterval(Integer.getInteger("influx.retryIntervalMs", 2000))
                        .maxRetries(Integer.getInteger("influx.maxRetries", 5))
                        .build();
                WRITE_API = CLIENT.makeWriteApi(opts);
            }
        }

        static boolean isEnabled() {
            return ENABLED;
        }

        static WriteApi getWriteApi() {
            return WRITE_API;
        }

        static void close() {
            if (!ENABLED) {
                return;
            }

            try {
                WRITE_API.close();
            } catch (Exception ignored) {

            }
            try {
                CLIENT.close();
            } catch (Exception ignored) {
            }
        }
    }
}

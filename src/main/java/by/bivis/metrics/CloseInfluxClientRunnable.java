package by.bivis.metrics;

import by.bivis.influx_aspect_profiler.TimingAspect;

public class CloseInfluxClientRunnable {
    public static final Runnable closeInfluxClient = () -> {
        TimingAspect.InfluxClient.writeApi.flush();
        TimingAspect.InfluxClient.client.close();
    };
}

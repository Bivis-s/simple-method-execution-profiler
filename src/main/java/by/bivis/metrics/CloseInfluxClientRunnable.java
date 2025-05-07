package by.bivis.metrics;

import by.bivis.influx_aspect_profiler.InfluxClient;

public class CloseInfluxClientRunnable {
    public static final Runnable closeInfluxClient = () -> {
        InfluxClient.writeApi.flush();
        InfluxClient.client.close();
    };
}

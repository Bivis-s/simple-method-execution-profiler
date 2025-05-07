package by.bivis.influx_aspect_profiler;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.WriteApi;
import com.influxdb.client.WriteOptions;

//@Slf4j
public class InfluxClient {
    public static final InfluxDBClient client;
    public static final WriteApi writeApi;

    static {
        String url = "http://localhost:8086";
        char[] token = "tgLxNM8N02nmcsteHfKF1KY5NT1wfV07TUGGS5C9loOxE5UabqBVe7g3IF9EMRfTP5ovpeN9sftRG-KFKg9lSg==".toCharArray();
        String org = "dev";
        String bucket = "tmp3";

        client = InfluxDBClientFactory.create(
                url,
                token,
                org,
                bucket
        );

        WriteOptions options = WriteOptions.builder()
                .batchSize(200)        // отправлять пакетами по 200 точек
                .flushInterval(500)    // не дольше 0.5 секунд в буфере
                .bufferLimit(1000)     // максимум 1000 точек в памяти
                .jitterInterval(0)    // небольшая «рандомизация» интервала
                .retryInterval(300)    // пауза перед retry
                .maxRetries(3)          // до 3 попыток при ошибках
                .build();

        writeApi = client.makeWriteApi(options);
    }
}

package by.bivis.metrics;

import java.util.logging.Logger;

public class Main {
    public static Logger log = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        Runtime.getRuntime().addShutdownHook(new Thread(CloseInfluxClientRunnable.closeInfluxClient));

        log.info("Start");

        var demo = new Demo();

        for (int i = 0; i < 3; i++) {
            demo.imitateWork1();
            demo.imitateWork2();
            demo.imitateWork3();
            demo.imitateWork4();
        }

        yesStatic();
    }

    public static void yesStatic() {
        System.out.println("qwe");
    }
}

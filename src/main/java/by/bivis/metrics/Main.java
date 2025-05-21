package by.bivis.metrics;

import java.util.logging.Logger;

public class Main {
    public static Logger log = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        log.info("Start");

        var demo = new Demo();

        for (var i = 0; i < 5000; i++) {
            demo.imitateWork1();
            demo.imitateWork2();
            demo.imitateWork3();
            demo.imitateWork4();
            yesStatic();
            emptyMethod();
        }
    }

    public static void yesStatic() {
        System.out.println("qwe");
    }

    public static void emptyMethod() {
    }
}

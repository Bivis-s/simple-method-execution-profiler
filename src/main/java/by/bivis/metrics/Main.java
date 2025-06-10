package by.bivis.metrics;

import java.util.logging.Logger;

public class Main {
    public static Logger log = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        log.info("Start");

        var demo = new Demo();

        for (var i = 0; i < 50; i++) {
            demo.imitateWork1();
            demo.imitateWork2();
            demo.imitateWork3();
            demo.imitateWork4();
            yesStatic("q", 1);
            emptyMethod();
        }
    }

    public static void yesStatic(String s, int i) {
        System.out.println("qwe " + s + " " + i);
    }

    public static void emptyMethod() {
    }
}

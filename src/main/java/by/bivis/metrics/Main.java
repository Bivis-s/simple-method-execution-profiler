package by.bivis.metrics;

public class Main {
    public static void main(String[] args) {
        var demo = new Demo();

        for (int i = 0; i < 10; i++) {
            demo.imitateWork1();
            demo.imitateWork2();
            demo.imitateWork3();
        }
    }
}

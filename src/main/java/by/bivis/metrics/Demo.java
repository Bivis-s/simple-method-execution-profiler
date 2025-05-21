package by.bivis.metrics;

import java.util.Random;

public class Demo {
    private static final Random rand = new Random();

    public void imitateWork1() {
        try {
            Thread.sleep(rand.nextInt(0, 400));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void imitateWork2() {
        try {
            Thread.sleep(rand.nextInt(0, 200));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void imitateWork3() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void imitateWork4() {
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}

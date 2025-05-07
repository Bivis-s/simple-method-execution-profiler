package by.bivis.metrics;

import java.util.Random;

public class Demo {
    private static final Random rand = new Random();

    public void imitateWork1() {
        try {
            Thread.sleep(rand.nextInt(0, 50));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void imitateWork2() {
        try {
            Thread.sleep(rand.nextInt(0, 25));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void imitateWork3() {
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void imitateWork4() {
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}

package by.bivis.metrics;

import org.testng.annotations.Test;

public class DemoTest {

    @Test
    public void testWork() {
        var demo = new Demo();

        for (var i = 0; i < 10; i++) {
            demo.imitateWork1();
            demo.imitateWork2();
            demo.imitateWork3();
        }
    }
}

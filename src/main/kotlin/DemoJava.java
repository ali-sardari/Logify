import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import org.utils.logging.Logify;

import java.io.IOException;

public class DemoJava {
    public static void main(String[] args) {
        Logify.initialize("TestLog", "out/logs", false);

        Logify.d("debug");
        Logify.i("info");
        Logify.w("warn");
        Logify.e("error");

        Logify.tag("CustomTag").i("info");

        Logify.e(new IOException("Network error!!!"));
        Logify.stackTrace();

        Logify.measureTimeMillis(new Function0<Unit>() {
            @Override
            public Unit invoke() {
                for (int i = 1; i <= 10; i++) {
                    try {
                        Thread.sleep(60);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }

                return null;
            }
        });
    }
}
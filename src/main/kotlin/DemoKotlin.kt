import org.utils.logging.Logify
import java.io.IOException

object DemoKotlin {
    @JvmStatic
    fun main(args: Array<String>) {
        Logify.initialize("TestLog", "out/logs", false)

        Logify.d("debug")
        Logify.i("info")
        Logify.w("warn")
        Logify.e("error")

        Logify.tag("CustomTag").i("information")
        Logify.e(IOException("Network error!!!"))
        Logify.stackTrace()

        Logify.measureTimeMillis {
            for (i in 1..10) {
                try {
                    Thread.sleep(60)
                } catch (e: InterruptedException) {
                    throw RuntimeException(e)
                }
            }
        }
    }
}
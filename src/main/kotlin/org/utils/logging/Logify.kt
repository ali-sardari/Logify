package org.utils.logging

import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.logging.ConsoleHandler
import java.util.logging.Formatter
import java.util.logging.LogRecord
import java.util.logging.Logger
import java.util.regex.Pattern

/**
 * The `Logify` class provides a flexible and customizable logging framework in Java.
 * It includes features for logging messages, exceptions, measuring execution time,
 * and writing logs to files. The class supports different log levels (DEBUG, INFO, WARNING, ERROR)
 * and provides methods to customize log output formats and destinations.
 *
 * Usage:
 * To use Logify, initialize the logger using one of the `initialize()` methods before
 * logging any messages. You can customize the log behavior by setting loggable levels,
 * loggable tags, and enabling/disabling logging. Log messages can be sent using the
 * `d()`, `i()`, `w()`, and `e()` methods with various overloads allowing logging of
 * messages and exceptions.
 *
 * Example Usage:
 * ```
 * Logify.initialize("MyApp", "/path/to/logs", true);
 * Logify.d("Debug message");
 * Logify.e(exception, "Error occurred");
 * ```
 *
 * Features:
 * - Customizable log levels: DEBUG, INFO, WARNING, ERROR.
 * - Logging to console or files with configurable log paths.
 * - Flexible log message formatting and filtering.
 * - Stack trace logging and custom tag support.
 * - Execution time measurement using `measureTimeMillis()` method.
 *
 * Note: To use this class, make sure to initialize the logger using one of the `initialize()`
 * methods before logging any messages. Logging can be enabled, disabled, or customized using
 * the provided static methods.
 */
@Suppress("unused")
class Logify private constructor() {
    /**
     * Abstract class representing the logging interface. Provides methods for logging messages,
     * exceptions, and measuring execution time. Can be extended to customize logging behavior.
     */
    abstract class ILogger {
        //region tag
        @get:JvmSynthetic
        internal val explicitTag = ThreadLocal<String>()

        @get:JvmSynthetic
        internal open val tag: String?
            get() {
                val tag = explicitTag.get()
                if (tag != null) {
                    explicitTag.remove()
                }
                return tag
            }
        //endregion

        //region logs
        open fun d(message: String?, vararg args: Any?) {
            prepareLog(java.util.logging.Level.FINE, null, message, *args)
        }

        open fun d(t: Throwable?, message: String?, vararg args: Any?) {
            prepareLog(java.util.logging.Level.FINE, t, message, *args)
        }

        open fun d(t: Throwable?) {
            prepareLog(java.util.logging.Level.FINE, t, null)
        }

        open fun i(message: String?, vararg args: Any?) {
            prepareLog(java.util.logging.Level.INFO, null, message, *args)
        }

        open fun i(t: Throwable?, message: String?, vararg args: Any?) {
            prepareLog(java.util.logging.Level.INFO, t, message, *args)
        }

        open fun i(t: Throwable?) {
            prepareLog(java.util.logging.Level.INFO, t, null)
        }

        open fun w(message: String?, vararg args: Any?) {
            prepareLog(java.util.logging.Level.WARNING, null, message, *args)
        }

        open fun w(t: Throwable?, message: String?, vararg args: Any?) {
            prepareLog(java.util.logging.Level.WARNING, t, message, *args)
        }

        open fun w(t: Throwable?) {
            prepareLog(java.util.logging.Level.WARNING, t, null)
        }

        open fun e(message: String?, vararg args: Any?) {
            prepareLog(java.util.logging.Level.SEVERE, null, message, *args)
        }

        open fun e(t: Throwable?, message: String?, vararg args: Any?) {
            prepareLog(java.util.logging.Level.SEVERE, t, message, *args)
        }

        open fun e(t: Throwable?) {
            prepareLog(java.util.logging.Level.SEVERE, t, null)
        }

        open fun stackTrace(tag: String): String {
            var stackTraceString: String

            stackTraceString = try {
                stackTraceString = Arrays.toString(Thread.currentThread().stackTrace)
                stackTraceString
                    .replace(", ", "\n")
                    .replace("[", "")
                    .replace("]", "")
            } catch (ex: Exception) {
                ex.stackTraceToString()
            }

            i("$tag\n$stackTraceString")
            return stackTraceString
        }

        open fun stackTrace(): String {
            return stackTrace("stackTrace")
        }
        //endregion

        @get:JvmSynthetic
        internal var isLoggingEnabled: Boolean = true

        private fun isLoggable(tag: String?, level: Level): Boolean {
            return (loggableLevels.isEmpty() || (level in loggableLevels)) &&
                    (loggableTags.isEmpty() || (tag in loggableTags))
        }

        private fun prepareLog(level: java.util.logging.Level, t: Throwable?, msg: String?, vararg args: Any?) {
            if (!isLoggingEnabled)
                return

            val logLevel = level.toLogifyLevel()
            val tag = tag

            if (!isLoggable(tag, logLevel)) {
                return
            }

            var message = msg
            if (message.isNullOrEmpty()) {
                if (t == null) {
                    return
                }
                message = getStackTraceString(t)
            } else {
                if (args.isNotEmpty()) {
                    message = formatMessage(message, args)
                }
                if (t != null) {
                    message += "\n" + getStackTraceString(t)
                }
            }

            log(logLevel, tag, message, t)
        }

        protected open fun formatMessage(message: String, args: Array<out Any?>) = message.format(*args)

        private fun getStackTraceString(t: Throwable): String {
            val sw = StringWriter(256)
            val pw = PrintWriter(sw, false)
            t.printStackTrace(pw)
            pw.flush()
            return sw.toString()
        }

        protected abstract fun log(level: Level, tag: String?, message: String, t: Throwable?)
    }

    /**
     * Implementation of ILogger providing logging functionality. Allows customization of log
     * formatting, log paths, and loggable levels.
     */
    open class DebugLogger : ILogger() {
        private val logger by lazy {
            Logger.getLogger(baseTag).also {
                formatter(it)
            }
        }

        protected open val baseTag: String? = null

        protected open val logPath: String? = null

        protected open val useSystemStyle: Boolean? = false

        private val fqcnIgnore = listOf(
            Logify::class.java.name,
            MainLogger::class.java.name,
            ILogger::class.java.name,
            DebugLogger::class.java.name
        )

        override val tag: String?
            get() = super.tag ?: Throwable().stackTrace
                .first { it.className !in fqcnIgnore }
                .let(::createStackElementTag)

        private fun formatter(logger: Logger) {
            val handler = ConsoleHandler()
            logger.level = java.util.logging.Level.FINE
            handler.level = java.util.logging.Level.FINE
            handler.formatter = object : Formatter() {
                override fun format(record: LogRecord): String {
                    val logLevel = record.level.toLogifyLevel()
                    val logDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")
                        .format(Date(record.millis))

                    return "${logLevel.color}$logDate [${logLevel.value}] $baseTag: ${record.message}${LogifyColors.RESET_COLOR}\n"
                }
            }
            if (useSystemStyle == true) {
                logger.useParentHandlers = true
            } else {
                logger.useParentHandlers = false
                logger.addHandler(handler)
            }
        }

        protected open fun createStackElementTag(element: StackTraceElement): String? {
            var tag = element.className.substringAfterLast('.')
            val m = ANONYMOUS_CLASS.matcher(tag)
            if (m.find()) {
                tag = m.replaceAll("")
            }

            return if (tag.length <= MAX_TAG_LENGTH) {
                tag
            } else {
                tag.substring(0, MAX_TAG_LENGTH)
            }
        }

        override fun log(level: Level, tag: String?, message: String, t: Throwable?) {
            if (message.length < MAX_LOG_LENGTH) {
                logger.log(level.toLoggingLevel(), "$tag -> $message")
                writeLogToFile(level, "$tag -> $message")
                return
            }

            var i = 0
            val length = message.length
            while (i < length) {
                var newline = message.indexOf('\n', i)
                newline = if (newline != -1) newline else length
                do {
                    val end = newline.coerceAtMost(i + MAX_LOG_LENGTH)
                    val part = message.substring(i, end)
                    logger.log(level.toLoggingLevel(), "$tag -> $part")
                    writeLogToFile(level, "$tag -> $part")

                    i = end
                } while (i < newline)
                i++
            }
        }

        private fun currentDate() = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(Date())

        //region Write log to file
        private fun writeLogToFile(level: Level, log: String) {
            try {
                if (logPath.isNullOrEmpty()) return

                val logText = "${currentDate()} [${level.value}] $baseTag: $log"

                //Make a root log directory
                val folderLogs = File("$logPath")
                if (!folderLogs.exists()) {
                    folderLogs.mkdirs()
                }

                //Make a log directory per date
                val dateFormat = SimpleDateFormat("yyyy-MM-dd")
                val currentDate = dateFormat.format(Date())
                val folder = File(folderLogs.path + "/" + currentDate)
                if (!folder.exists()) {
                    folder.mkdir()
                }
                val files = folder.listFiles()

                // Find the last log file
                var lastFile = files?.let { findLastLogFile(it) }

                // If the last log file doesn't exist or its size is greater than or equal to 10KB, create a new file
                if (lastFile == null || lastFile.length() >= MAX_FILE_SIZE) {
                    val fileName = String.format("log-%s.log", SimpleDateFormat("yyyyMMdd_HHmmss").format(Date()))
                    lastFile = File(folder, fileName)
                }

                // Write the log to the last file
                try {
                    FileWriter(lastFile, true).use { fileWriter ->
                        BufferedWriter(fileWriter).use { bufferedWriter ->
                            bufferedWriter.write(logText)
                            bufferedWriter.newLine()
                        }
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }

        private fun findLastLogFile(files: Array<File>): File? {
            var lastFile: File? = null
            for (file in files) {
                if (file.isFile && file.name.startsWith("log-")) {
                    if ((lastFile == null) || (file.lastModified() > lastFile.lastModified())) {
                        lastFile = file
                    }
                }
            }
            return lastFile
        }
        //endregion

        companion object {
            private const val MAX_FILE_SIZE = 1024 * 1024 // 1MB
            private const val MAX_LOG_LENGTH = 4000
            private const val MAX_TAG_LENGTH = 24
            private val ANONYMOUS_CLASS = Pattern.compile("(\\$\\d+)+$")
        }
    }

    companion object MainLogger : ILogger() {
        private const val ERROR_MESSAGE = "Error: Logify has not been initialized! Please ensure that the logging system is properly initialized before attempting to use it. You can initialize Logify by calling the initialize() method before logging any messages."
        private const val TAG = "Logify"

        private var log: ILogger? = null

        /**
         * Initializes the Logify logger with default settings. Log messages will be tagged with the
         * default tag ("Logify"), and log output will be directed to the console.
         */
        @JvmStatic
        fun initialize() {
            initialize(null, null, null)
        }

        /**
         * Initializes the Logify logger with a custom base tag. Log messages will be tagged with
         * the specified base tag, and log output will be directed to the console.
         *
         * @param baseTag The base tag to be used for log messages.
         */
        @JvmStatic
        fun initialize(baseTag: String) {
            initialize(baseTag, null, null)
        }

        /**
         * Initializes the Logify logger with a custom base tag and log file path. Log messages will be
         * tagged with the specified base tag, and log output will be written to the specified log file.
         *
         * @param baseTag The base tag to be used for log messages.
         * @param logPath The path to the log file where log messages will be written.
         */
        @JvmStatic
        fun initialize(baseTag: String?, logPath: String) {
            initialize(baseTag, logPath, false)
        }

        /**
         * Initializes the Logify logger with a custom base tag and system style logging option. Log messages will be tagged with
         * the specified base tag, and log output will be directed to the console. System style logging can be enabled or disabled.
         * If no log file path is provided, log messages will be printed to the console.
         *
         * @param baseTag        The base tag to be used for log messages. If null, the default tag "Logify" will be used.
         * @param useSystemStyle If true, system style logging will be used; otherwise, console logging will be used.
         */
        @JvmStatic
        fun initialize(baseTag: String?, useSystemStyle: Boolean? = false) {
            initialize(baseTag, null, useSystemStyle)
        }

        /**
         * Initializes the Logify logger with system style logging option. Log messages will be tagged with
         * the default tag "Logify", and log output will be directed to the console. System style logging can be enabled or disabled.
         * If no log file path is provided, log messages will be printed to the console.
         *
         * @param useSystemStyle If true, system style logging will be used; otherwise, console logging will be used.
         */
        @JvmStatic
        fun initialize(useSystemStyle: Boolean? = false) {
            initialize(null, null, useSystemStyle)
        }

        /**
         * Initializes the Logify logger with a custom implementation of the ILogger interface. This method allows you to provide
         * a custom logger implementation, enabling advanced customization of logging behavior.
         *
         * @param log An instance of ILogger interface representing the custom logger implementation.
         */
        @JvmStatic
        fun initialize(log: ILogger) {
            MainLogger.log = log
        }

        /**
         * Initializes the Logify logger with a custom base tag, log file path, and system style logging option.
         * Log messages will be tagged with the specified base tag, and log output will be written to the specified
         * log file. System style logging can be enabled or disabled.
         *
         * @param baseTag        The base tag to be used for log messages.
         * @param logPath        The path to the log file where log messages will be written.
         * @param useSystemStyle If true, system style logging will be used; otherwise, console logging will be used.
         */
        @JvmStatic
        fun initialize(baseTag: String? = null, logPath: String? = null, useSystemStyle: Boolean? = false) {
            initialize(object : DebugLogger() {
                override fun createStackElementTag(element: StackTraceElement): String {
                    return super.createStackElementTag(element) + ":" +
                            element.methodName + ":" +
                            element.lineNumber
                }

                override val baseTag = baseTag ?: TAG

                override val logPath = logPath

                override val useSystemStyle: Boolean? = useSystemStyle
            })
        }

        private var loggableLevels = emptyArray<Level>()
        private var loggableTags = emptyArray<String>()

        /**
         * Sets the loggable levels. Only log messages with levels specified in the `levels` array
         * will be processed and displayed.
         *
         * @param levels An array of log levels to be considered loggable.
         */
        @JvmStatic
        fun setLoggableLevels(levels: Array<Level>) {
            loggableLevels = levels
        }

        /**
         * Sets the loggable tags. Only log messages with tags specified in the `tags` array
         * will be processed and displayed.
         *
         * @param tags An array of tags to be considered loggable.
         */
        @JvmStatic
        fun setLoggableTags(tags: Array<String>) {
            loggableTags = tags
        }

        @JvmStatic
        fun disableLogging() {
            log?.let {
                it.isLoggingEnabled = false
            } ?: run {
                Logger.getLogger(TAG).severe(ERROR_MESSAGE)
            }
        }

        @JvmStatic
        fun enableLogging() {
            log?.let {
                it.isLoggingEnabled = true
            } ?: run {
                Logger.getLogger(TAG).severe(ERROR_MESSAGE)
            }
        }

        //region logs
        @JvmStatic
        override fun d(message: String?, vararg args: Any?) {
            log?.d(message, *args) ?: run {
                Logger.getLogger(TAG).severe(ERROR_MESSAGE)
            }
        }

        @JvmStatic
        override fun d(t: Throwable?, message: String?, vararg args: Any?) {
            log?.d(t, message, *args) ?: run {
                Logger.getLogger(TAG).severe(ERROR_MESSAGE)
            }
        }

        @JvmStatic
        override fun d(t: Throwable?) {
            log?.d(t) ?: run {
                Logger.getLogger(TAG).severe(ERROR_MESSAGE)
            }
        }

        @JvmStatic
        override fun i(message: String?, vararg args: Any?) {
            log?.i(message, *args) ?: run {
                Logger.getLogger(TAG).severe(ERROR_MESSAGE)
            }
        }

        @JvmStatic
        override fun i(t: Throwable?, message: String?, vararg args: Any?) {
            log?.i(t, message, *args) ?: run {
                Logger.getLogger(TAG).severe(ERROR_MESSAGE)
            }
        }

        @JvmStatic
        override fun i(t: Throwable?) {
            log?.i(t) ?: run {
                Logger.getLogger(TAG).severe(ERROR_MESSAGE)
            }
        }

        @JvmStatic
        override fun w(message: String?, vararg args: Any?) {
            log?.w(message, *args) ?: run {
                Logger.getLogger(TAG).severe(ERROR_MESSAGE)
            }
        }

        @JvmStatic
        override fun w(t: Throwable?, message: String?, vararg args: Any?) {
            log?.w(t, message, *args) ?: run {
                Logger.getLogger(TAG).severe(ERROR_MESSAGE)
            }
        }

        @JvmStatic
        override fun w(t: Throwable?) {
            log?.w(t) ?: run {
                Logger.getLogger(TAG).severe(ERROR_MESSAGE)
            }
        }

        @JvmStatic
        override fun e(message: String?, vararg args: Any?) {
            log?.e(message, *args) ?: run {
                Logger.getLogger(TAG).severe(ERROR_MESSAGE)
            }
        }

        @JvmStatic
        override fun e(t: Throwable?, message: String?, vararg args: Any?) {
            log?.e(t, message, *args) ?: run {
                Logger.getLogger(TAG).severe(ERROR_MESSAGE)
            }
        }

        @JvmStatic
        override fun e(t: Throwable?) {
            log?.e(t) ?: run {
                Logger.getLogger(TAG).severe(ERROR_MESSAGE)
            }
        }

        @JvmStatic
        override fun stackTrace(): String {
            return super.stackTrace()
        }

        @JvmStatic
        override fun stackTrace(tag: String): String {
            return super.stackTrace(tag)
        }

        override fun log(level: Level, tag: String?, message: String, t: Throwable?) {
            throw AssertionError()
        }
        //endregion

        @JvmStatic
        fun tag(tag: String): ILogger {
            log?.explicitTag?.set(tag) ?: run {
                Logger.getLogger(TAG).severe(ERROR_MESSAGE)
            }

            return this
        }

        @JvmStatic
        inline fun measureTimeMillis(block: () -> Unit): Long {
            return measureTimeMillis(null, block)
        }

        @JvmStatic
        inline fun measureTimeMillis(tag: String? = null, block: () -> Unit): Long {
            val startTime = System.currentTimeMillis()
            block()
            val endTime = System.currentTimeMillis()
            val elapsedTime = endTime - startTime

            if (tag.isNullOrEmpty()) {
                i("Time taken: $elapsedTime milliseconds")
            } else {
                tag(tag).i("Time taken: $elapsedTime milliseconds")
            }

            return elapsedTime
        }
    }
}
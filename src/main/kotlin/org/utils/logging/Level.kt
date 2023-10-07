package org.utils.logging

enum class Level(val value: String, val color: String) {
    DEBUG("D", LogifyColors.DEBUG_COLOR),
    INFO("I", LogifyColors.INFO_COLOR),
    WARNING("W", LogifyColors.WARN_COLOR),
    ERROR("E", LogifyColors.ERROR_COLOR)
}

internal fun java.util.logging.Level.toLogifyLevel(): Level {
    return when (this.name) {
        "FINE" -> Level.DEBUG
        "INFO" -> Level.INFO
        "WARNING" -> Level.WARNING
        "SEVERE" -> Level.ERROR
        else -> Level.INFO
    }
}

internal fun Level.toLoggingLevel(): java.util.logging.Level {
    return when (this.name) {
        "DEBUG" -> java.util.logging.Level.FINE
        "INFO" -> java.util.logging.Level.INFO
        "WARNING" -> java.util.logging.Level.WARNING
        "ERROR" -> java.util.logging.Level.SEVERE
        else -> java.util.logging.Level.INFO
    }
}
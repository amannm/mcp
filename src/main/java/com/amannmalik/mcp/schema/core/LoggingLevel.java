package com.amannmalik.mcp.schema.core;

/** Severity of a log message. */
public enum LoggingLevel {
    EMERGENCY("emergency"),
    ALERT("alert"),
    CRITICAL("critical"),
    ERROR("error"),
    WARNING("warning"),
    NOTICE("notice"),
    INFO("info"),
    DEBUG("debug");

    private final String value;

    LoggingLevel(String value) { this.value = value; }

    @Override
    public String toString() { return value; }
}

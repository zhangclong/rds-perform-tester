package com.uh.rds.testing.logger;

import org.slf4j.Logger;

public class TestLoggerFactory {

    public static Logger getLogger(Class<?> clazz) {
        return new TestLogger(org.slf4j.LoggerFactory.getLogger(clazz));
    }

    public static Logger getLogger(String name) {
        return new TestLogger(org.slf4j.LoggerFactory.getLogger(name));
    }

    public static Logger getLogger(Class<?> clazz, int maxErrors) {
        return new TestLogger(org.slf4j.LoggerFactory.getLogger(clazz), maxErrors);
    }

    public static Logger getLogger(String name, int maxErrors) {
        return new TestLogger(org.slf4j.LoggerFactory.getLogger(name), maxErrors);
    }
}

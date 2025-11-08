package com.uh.rds.testing.logger;

import org.slf4j.Logger;
import org.slf4j.Marker;


public class TestLogger implements Logger {

    private Logger logger;
    private int maxErrors = 50; //error类型的日志最大数量，大于此数量后不再记录
    private int errors = 0; //记录error类型的日志数量
    private final static String reachMaxErrors = " [The number of error logs has reached the maximum limit!]";

    public TestLogger(Logger logger) {
        this.logger = logger;
    }

    public TestLogger(Logger logger, int maxErrors) {
        this.logger = logger;
        this.maxErrors = maxErrors;
    }

    @Override
    public String getName() {
        return logger.getName();
    }

    @Override
    public boolean isTraceEnabled() {
        return logger.isTraceEnabled();
    }

    @Override
    public void trace(String msg) {
        logger.trace(msg);
    }

    @Override
    public void trace(String format, Object arg) {
        logger.trace(format, arg);
    }

    @Override
    public void trace(String format, Object arg1, Object arg2) {
        logger.trace(format, arg1, arg2);
    }

    @Override
    public void trace(String format, Object... arguments) {
        logger.trace(format, arguments);
    }

    @Override
    public void trace(String msg, Throwable t) {
        logger.trace(msg, t);
    }

    @Override
    public boolean isTraceEnabled(Marker marker) {
        return logger.isTraceEnabled(marker);
    }

    @Override
    public void trace(Marker marker, String msg) {
        logger.trace(marker, msg);
    }

    @Override
    public void trace(Marker marker, String format, Object arg) {
        logger.trace(marker, format, arg);
    }

    @Override
    public void trace(Marker marker, String format, Object arg1, Object arg2) {
        logger.trace(marker, format, arg1, arg2);
    }

    @Override
    public void trace(Marker marker, String format, Object... argArray) {
        logger.trace(marker, format, argArray);
    }

    @Override
    public void trace(Marker marker, String msg, Throwable t) {
        logger.trace(marker, msg, t);
    }

    @Override
    public boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }

    @Override
    public void debug(String msg) {
        logger.debug(msg);
    }

    @Override
    public void debug(String format, Object arg) {
        logger.debug(format, arg);
    }

    @Override
    public void debug(String format, Object arg1, Object arg2) {
        logger.debug(format, arg1, arg2);
    }

    @Override
    public void debug(String format, Object... arguments) {
        logger.debug(format, arguments);
    }

    @Override
    public void debug(String msg, Throwable t) {
        logger.debug(msg, t);
    }

    @Override
    public boolean isDebugEnabled(Marker marker) {
        return logger.isDebugEnabled(marker);
    }

    @Override
    public void debug(Marker marker, String msg) {
        logger.debug(marker, msg);
    }

    @Override
    public void debug(Marker marker, String format, Object arg) {
        logger.debug(marker, format, arg);
    }

    @Override
    public void debug(Marker marker, String format, Object arg1, Object arg2) {
        logger.debug(marker, format, arg1, arg2);
    }

    @Override
    public void debug(Marker marker, String format, Object... arguments) {
        logger.debug(marker, format, arguments);
    }

    @Override
    public void debug(Marker marker, String msg, Throwable t) {
        logger.debug(marker, msg, t);
    }

    @Override
    public boolean isInfoEnabled() {
        return logger.isInfoEnabled();
    }

    @Override
    public void info(String msg) {
        logger.info(msg);
    }

    @Override
    public void info(String format, Object arg) {
        logger.info(format, arg);
    }

    @Override
    public void info(String format, Object arg1, Object arg2) {
        logger.info(format, arg1, arg2);
    }

    @Override
    public void info(String format, Object... arguments) {
        logger.info(format, arguments);
    }

    @Override
    public void info(String msg, Throwable t) {
        logger.info(msg, t);
    }

    @Override
    public boolean isInfoEnabled(Marker marker) {
        return logger.isInfoEnabled(marker);
    }

    @Override
    public void info(Marker marker, String msg) {
        logger.info(marker, msg);
    }

    @Override
    public void info(Marker marker, String format, Object arg) {
        logger.info(marker, format, arg);
    }

    @Override
    public void info(Marker marker, String format, Object arg1, Object arg2) {
        logger.info(marker, format, arg1, arg2);
    }

    @Override
    public void info(Marker marker, String format, Object... arguments) {
        logger.info(marker, format, arguments);
    }

    @Override
    public void info(Marker marker, String msg, Throwable t) {
        logger.info(marker, msg, t);
    }

    @Override
    public boolean isWarnEnabled() {
        return logger.isWarnEnabled();
    }

    @Override
    public void warn(String msg) {
        logger.warn(msg);
    }

    @Override
    public void warn(String format, Object arg) {
        logger.warn(format, arg);
    }

    @Override
    public void warn(String format, Object arg1, Object arg2) {
        logger.warn(format, arg1, arg2);
    }

    @Override
    public void warn(String format, Object... arguments) {
        logger.warn(format, arguments);
    }

    @Override
    public void warn(String msg, Throwable t) {
        logger.warn(msg, t);
    }

    @Override
    public boolean isWarnEnabled(Marker marker) {
        return logger.isWarnEnabled(marker);
    }

    @Override
    public void warn(Marker marker, String msg) {
        logger.warn(marker, msg);
    }

    @Override
    public void warn(Marker marker, String format, Object arg) {
        logger.warn(marker, format, arg);
    }

    @Override
    public void warn(Marker marker, String format, Object arg1, Object arg2) {
        logger.warn(marker, format, arg1, arg2);
    }

    @Override
    public void warn(Marker marker, String format, Object... arguments) {
        logger.warn(marker, format, arguments);
    }

    @Override
    public void warn(Marker marker, String msg, Throwable t) {
        logger.warn(marker, msg, t);
    }

    @Override
    public boolean isErrorEnabled() {
        return logger.isErrorEnabled();
    }

    @Override
    public void error(String msg) {
        errors++;
        if (errors <= maxErrors) {
            logger.error(msg);
        }
        else if (errors == maxErrors) {
            logger.error(msg + reachMaxErrors);
        }
    }

    @Override
    public void error(String format, Object arg) {
        errors++;
        if (errors <= maxErrors) {
            logger.error(format, arg);
        }
        else if (errors == maxErrors) {
            logger.error(format + reachMaxErrors, arg);
        }
    }

    @Override
    public void error(String format, Object arg1, Object arg2) {
        errors++;
        if (errors <= maxErrors) {
            logger.error(format, arg1, arg2);
        }
        else if (errors == maxErrors) {
            logger.error(format + reachMaxErrors, arg1, arg2);
        }
    }

    @Override
    public void error(String format, Object... arguments) {
        errors++;
        if (errors <= maxErrors) {
            logger.error(format, arguments);
        }
        else if (errors == maxErrors) {
            logger.error(format + reachMaxErrors, arguments);
        }
    }

    @Override
    public void error(String msg, Throwable t) {
        errors++;
        if (errors <= maxErrors) {
            logger.error(msg, t);
        }
        else if (errors == maxErrors) {
            logger.error(msg + reachMaxErrors, t);
        }
    }

    @Override
    public boolean isErrorEnabled(Marker marker) {
        return logger.isErrorEnabled(marker);
    }

    @Override
    public void error(Marker marker, String msg) {
        errors++;
        if (errors < maxErrors) {
            logger.error(marker, msg);
        }
        else if (errors == maxErrors) {
            logger.error(marker, msg + reachMaxErrors);
        }

    }

    @Override
    public void error(Marker marker, String format, Object arg) {
        errors++;
        if (errors <= maxErrors) {
            logger.error(marker, format, arg);
        }
        else if (errors == maxErrors) {
            logger.error(marker, format + reachMaxErrors, arg);
        }
    }

    @Override
    public void error(Marker marker, String format, Object arg1, Object arg2) {
        errors++;
        if (errors <= maxErrors) {
            logger.error(marker, format, arg1, arg2);
        }
        else if (errors == maxErrors) {
            logger.error(marker, format + reachMaxErrors, arg1, arg2);
        }
    }

    @Override
    public void error(Marker marker, String format, Object... arguments) {
        errors++;
        if (errors <= maxErrors) {
            logger.error(marker, format, arguments);
        }
        else if (errors == maxErrors) {
            logger.error(marker, format + reachMaxErrors, arguments);
        }
    }

    @Override
    public void error(Marker marker, String msg, Throwable t) {
        errors++;
        if (errors <= maxErrors) {
            logger.error(marker, msg, t);
        }
        else if (errors == maxErrors) {
            logger.error(marker, msg + reachMaxErrors, t);
        }
    }
}

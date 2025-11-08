package com.uh.rds.testing.utils;


import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

import java.io.StringWriter;
import java.util.Map;

/**
 * Velocity模版引擎的封装；
 * 使用时建议使用单实例模式，避免重复初始化Velocity引擎。
 */
public class VelocityEvaluator {

    private final VelocityEngine velocityEngine;

    public VelocityEvaluator() {
        velocityEngine = new VelocityEngine();
        velocityEngine.init();
    }

    /**
     * 使用Velocity引擎对模版进行解析
     * @param template  模版内容
     * @param variable  变量
     * @return
     */
    public String evaluatedTemplate(String template, Map<String, Object> variable) {
        VelocityContext context = new VelocityContext();
        for (String key : variable.keySet()) {
            context.put(key, variable.get(key));
        }
        return evaluatedTemplate(template, context);
    }


    /**
     * 使用Velocity引擎对模版进行解析
     * @param template  模版内容
     * @param context  变量
     * @return
     */
    public String evaluatedTemplate(String template, VelocityContext context) {
        try(StringWriter stringWriter = new StringWriter()) {
            velocityEngine.evaluate(context, stringWriter, "evaluateTemplate", template);
            return stringWriter.toString();
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }

    }
}
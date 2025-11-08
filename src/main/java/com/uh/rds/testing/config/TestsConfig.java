package com.uh.rds.testing.config;

import java.util.List;

/**
 * 包含1个或多个测试配置的类。
 */
public class TestsConfig {

    protected List<TestConfig> tests;

    public List<TestConfig> getTests() {
        return tests;
    }

    public void setTests(List<TestConfig> tests) {
        this.tests = tests;
    }
}

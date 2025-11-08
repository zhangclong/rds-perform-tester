package com.uh.rds.testing.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestsConfigRoot extends TestsConfig{

    private String testsBase; // 测试配置文件的基础目录

    private List<String> testPatterns; // 测试配置文件的匹配模式

    private Map<String, ConnectionConfig> connections; // 连接配置列表，key为连接配置的ID

    private Map<String, DataFileConfig> dataFiles; // 数据文件配置列表，key为数据文件配置的ID

    private Map<String, TestConfig> id2TestConfig;

    public Map<String, ConnectionConfig> getConnections() {
        return connections;
    }

    public void setConnections(Map<String, ConnectionConfig> connections) {
        this.connections = connections;
    }

    public Map<String, DataFileConfig> getDataFiles() {
        return dataFiles;
    }

    public void setDataFiles(Map<String, DataFileConfig> dataFiles) {
        this.dataFiles = dataFiles;
    }

    public String getTestsBase() {
        return testsBase;
    }

    public void setTestsBase(String testsBase) {
        this.testsBase = testsBase;
    }

    public List<String> getTestPatterns() {
        return testPatterns;
    }

    public void setTestPatterns(List<String> testPatterns) {
        this.testPatterns = testPatterns;
    }

    /**
     * 通过ID获取某一个测试配置
     * @param id
     * @return
     */
    public TestConfig getTestConfig(String id) {
        if(id2TestConfig == null) {
            id2TestConfig = new HashMap<>();
            for (TestConfig test : tests) {
                id2TestConfig.put(test.getId(), test);
            }
        }
        return id2TestConfig.get(id);
    }

    /**
     * 添加新的测试配置到当前的测试列表中。
     * @param newTests
     * @return true if this list changed as a result of the call
     */
    protected boolean addTests(TestsConfig newTests) {
        if(tests == null) {
            tests = new ArrayList<>();
        }

        return tests.addAll(newTests.getTests());
    }

}

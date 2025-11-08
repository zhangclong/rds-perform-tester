package com.uh.rds.testing.config;

import java.util.List;
import java.util.Map;

/**
 * 测试配置类，包含测试用例的配置参数
 */
public class TestConfig {
    private String id; // 唯一配置的ID
    private String description;; // 配置描述
    private Map<String, Object> configs; // 配置参数

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Map<String, Object> getConfigs() {
        return configs;
    }

    public void setConfigs(Map<String, Object> configs) {
        this.configs = configs;
    }

    public Object getObject(String key) {
        return configs.get(key);
    }

    public Boolean getBoolean(String key) {
        return (Boolean) configs.get(key);
    }

    public Integer getInteger(String key) {
        return (Integer) configs.get(key);
    }

    public <T> List<T> getList(String key) {
        return (List<T>) configs.get(key);
    }

    public Long getLong(String key) {
        Object value = configs.get(key);
        if(value instanceof Integer) {
            return ((Integer) value).longValue();
        }
        else if(value instanceof Long) {
            return (Long) value;
        }
        else {
            throw new RuntimeException("Invalid value type for key[" + key + "] in test config[" + id + "]");
        }
    }

    public String getString(String key) {
        return (String) configs.get(key);
    }

    @Override
    public String toString() {
        return "TestCaseConfig{" +
                "id='" + id + '\'' +
                ", description='" + description + '\'' +
                ", configs=" + configs +
                '}';
    }
}

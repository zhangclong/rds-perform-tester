package com.uh.rds.testing.performance;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.uh.rds.testing.config.PerformanceConfig;
import com.uh.rds.testing.config.TestConfig;
import com.uh.rds.testing.config.TestConfigManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;


public class PerformanceTestMain {

    private static final Logger logger = LoggerFactory.getLogger(PerformanceTestMain.class);

    private static List<PerformanceConfig> configList;

    private static final DataFileManager dataFileManager = new DataFileManager();

    /**
     * 初始化测试环境
     */
    public static void loadConfigs() {
        logger.info("PerformanceMainTest init ...");
        TestConfigManager.setConfigFile("perform-config.yml");
        List<TestConfig> allConfigs = TestConfigManager.getTestConfigs();

        configList = new ArrayList<>();
        for (TestConfig config : allConfigs) {
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            PerformanceConfig performConfig = mapper.convertValue(config.getConfigs(), PerformanceConfig.class);
            performConfig.setId(config.getId());
            performConfig.setDescription(config.getDescription());
            // 通过配置的连接名称获取连接信息
            String connectionName = performConfig.getConnectionName();
            if(connectionName != null && !connectionName.isEmpty()) {
                performConfig.setConnection(TestConfigManager.getConnectionConfig(connectionName.trim()));
            }

            // 通过配置的数据文件名称获取数据文件信息
            String dataFileName = performConfig.getDataFileName();
            if(dataFileName != null && !dataFileName.isEmpty()) {
                performConfig.setDataFileConfig(TestConfigManager.getDataFileConfig(dataFileName.trim()));
            }

            configList.add(performConfig);
        }

        // configList 按 id 排序
        configList.sort(Comparator.comparing(PerformanceConfig::getId));

    }

    public static void main(String[] args) throws Exception {
        // 读入所有配置
        loadConfigs();
        // 生成所有测试数据文件
        dataFileManager.generateDataFiles(configList);

        for (PerformanceConfig config : configList) {
            if(config.isDisable()) {
                logger.info("Performance test [{} - {}] is disabled, skipped.", config.getId(), config.getDescription());
                continue;
            }
            // 验证配置合法性, 不合法则抛出异常
            config.validate();

            System.out.println(config.getSummary());
            PerformanceTestRunner runner = new PerformanceTestRunner(config);
            runner.prepareThreadsData();
            runner.runTest();
        }

    }





}

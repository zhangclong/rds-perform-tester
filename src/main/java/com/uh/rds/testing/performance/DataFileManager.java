package com.uh.rds.testing.performance;

import com.uh.rds.testing.config.DataFileConfig;
import com.uh.rds.testing.config.PerformanceConfig;
import com.uh.rds.testing.data.FileTask;
import com.uh.rds.testing.data.TestDataGenerator;
import com.uh.rds.testing.utils.ValueUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 数据文件管理器，负责生成和管理测试数据文件
 */
public class DataFileManager {

    private final static Logger logger = LoggerFactory.getLogger(DataFileManager.class);

    //set to ["string", "set", "zset", "list", "hash", "stream"];
    static Set<String> DATA_TYPES = Set.of("string", "set", "zset", "list", "hash", "stream");

    /**
     * 根据配置生成数据文件
     * @param configList
     */
    public void generateDataFiles(List<PerformanceConfig> configList) {
        TestDataGenerator dataGenerator = new TestDataGenerator();

        //先获得所有未禁用的DataFileConfig配置，并滤除重复。
        Set<DataFileConfig> dataFileConfigs = configList.stream()
                .filter(c -> !c.isDisable())
                .map(PerformanceConfig::getDataFileConfig)
                .collect(Collectors.toSet());


        for (DataFileConfig fileConfig : dataFileConfigs) {
            fileConfig.setKeyGenMode(
                    (fileConfig.getKeyGenMode() == null || fileConfig.getKeyGenMode().isEmpty()) ?
                            "random" : fileConfig.getKeyGenMode().toLowerCase()
            );

            verifyConfig(fileConfig);
            String dataType = fileConfig.getDataType();
            File dataFile = new File(fileConfig.getDataFile());
            dataFile.getAbsolutePath();
            int[] valueSizeRange = {fileConfig.getValueLength(), fileConfig.getValueLength()};


            File folder = dataFile.getParentFile();
            if(!folder.exists()) {
                folder.mkdirs();
            }

            if(dataFile.exists() && isConfigChanged(dataFile, fileConfig) == false) {
                logger.info("Data file already exists, skipped: {}", dataFile.getAbsolutePath());
            }
            else {
                //把文件加入执行列表，并保存配置信息到 .properties 文件中
                dataGenerator.addTask(new FileTask(dataFile, dataType, fileConfig.getDataCount(),
                        ("string".equals(dataType)) ? 1 : fileConfig.getSubDataCount(), valueSizeRange,
                        fileConfig.getKeyGenMode(), fileConfig.getKeyStartIndex(), fileConfig.getKeyPrefix(), fileConfig.getKeyGenLength()));
                logger.info("Generating test data into file: {}", dataFile.getAbsolutePath());

                try {
                    saveConfigToFile(getPropertiesFile(dataFile), fileConfig);
                } catch (Exception e) {
                    logger.warn("Failed to save data file properties: " + getPropertiesFile(dataFile).getAbsolutePath(), e);
                }
            }
        }

        dataGenerator.executeTasks();
    }

    private boolean isConfigChanged(File dataFile, DataFileConfig config) {
        File propertiesFile = getPropertiesFile(dataFile);
        if(propertiesFile.exists()) {
            try {
                DataFileConfig existingConfig = loadConfigFromFile(propertiesFile);
                if (existingConfig != null && existingConfig.equals(config)) {
                    return false;
                }
            } catch (Exception e) {
                logger.error("Failed to load data file properties: {}", propertiesFile.getAbsolutePath(), e);
            }
        }
        return true;
    }

    private File getPropertiesFile(File dataFile) {
        File parent = dataFile.getParentFile();
        String propertiesFileName = dataFile.getName().endsWith(".csv") ?
                dataFile.getName().substring(0, dataFile.getName().length() - 4) + ".properties" :
                dataFile.getName() + ".properties";

        return new File(parent, propertiesFileName);
    }

    private void saveConfigToFile(File propertiesFile, DataFileConfig config) throws Exception {
        java.util.Properties props = new java.util.Properties();
        props.setProperty("dataType", config.getDataType());
        props.setProperty("dataCount", Integer.toString(config.getDataCount()));
        props.setProperty("valueLength", Integer.toString(config.getValueLength()));
        props.setProperty("subDataCount", Integer.toString(config.getSubDataCount()));
        props.setProperty("dataFile", config.getDataFile());
        props.setProperty("keyGenMode", config.getKeyGenMode());
        props.setProperty("keyStartIndex", Integer.toString(config.getKeyStartIndex()));
        props.setProperty("keyPrefix", config.getKeyPrefix());
        props.setProperty("keyGenLength", Integer.toString(config.getKeyGenLength()));

        try(java.io.FileOutputStream out = new java.io.FileOutputStream(propertiesFile)) {
            props.store(out, "Data file configuration");
        }
    }

    private DataFileConfig loadConfigFromFile(File propertiesFile) throws Exception {
        java.util.Properties props = new java.util.Properties();
        try(java.io.FileInputStream in = new java.io.FileInputStream(propertiesFile)) {
            props.load(in);
        }

        DataFileConfig config = new DataFileConfig();
        config.setDataType(props.getProperty("dataType"));
        config.setDataCount(Integer.parseInt(props.getProperty("dataCount", "0")));
        config.setValueLength(Integer.parseInt(props.getProperty("valueLength", "0")));
        config.setSubDataCount(Integer.parseInt(props.getProperty("subDataCount", "0")));
        config.setDataFile(props.getProperty("dataFile"));
        config.setKeyGenMode(props.getProperty("keyGenMode", "random"));
        config.setKeyStartIndex(Integer.parseInt(props.getProperty("keyStartIndex", "0")));
        config.setKeyPrefix(props.getProperty("keyPrefix", ""));
        config.setKeyGenLength(Integer.parseInt(props.getProperty("keyGenLength", "0")));

        return config;
    }





    /**
     * 验证数据文件配置的合法性
     * @param config
     */
    private void verifyConfig(DataFileConfig config) {
        if(config == null) {
            throw new IllegalArgumentException("Data file configuration is not defined.");
        }
        if(config.getDataType() == null || config.getDataType().isEmpty()) {
            throw new IllegalArgumentException("Data type is not defined.");
        }
        if(config.getDataCount() <= 0) {
            throw new IllegalArgumentException("Data count must be greater than zero.");
        }
        if(config.getValueLength() <= 0) {
            throw new IllegalArgumentException("Value length must be greater than zero.");
        }
        if(config.getSubDataCount() <= 0) {
            throw new IllegalArgumentException("Sub data count must be greater than zero.");
        }
        if(! DATA_TYPES.contains(config.getDataType())) {
            throw new IllegalArgumentException("Unknown data type: " + config.getDataType() + ". Supported types are: " + Arrays.toString(DATA_TYPES.toArray()));
        }
        if(ValueUtils.isEmpty(config.getDataFile())) {
            throw new IllegalArgumentException("dataFile is not defined.");
        }
        if(isAbsolute(config.getDataFile())) {
            throw new IllegalArgumentException("dataFile must be a relative path: " + config.getDataFile());
        }
        if(! "sequential".equals(config.getKeyGenMode()) && !"random".equals(config.getKeyGenMode())) {
            throw new IllegalArgumentException("keyGenMode must be either 'sequential' or 'random'.");
        }
        if(config.getKeyStartIndex() < 0) {
            throw new IllegalArgumentException("keyStartIndex must be greater than or equal to zero.");
        }
        if(config.getKeyGenLength() < 0) {
            throw new IllegalArgumentException("keyGenLength must be greater than or equal to zero.");
        }
        if(config.getKeyGenLength() > 0 && config.getKeyGenLength() <6) {
            throw new IllegalArgumentException("keyGenLength must be greater than or equal to 6 if specified.");
        }
    }


    /**
     * 判断文件路径是否是绝对路径
     * @param path
     * @return
     */
    private boolean isAbsolute(String path) {
        return (path.charAt(0) == '/' || path.matches("^[a-zA-Z]{1}:.*"));
    }


}

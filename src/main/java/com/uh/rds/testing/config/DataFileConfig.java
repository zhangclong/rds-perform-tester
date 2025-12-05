package com.uh.rds.testing.config;

import java.util.Objects;

public class DataFileConfig {

    // 数据类型, 可选取值 string, set, zset, list, hash, stream
    private String dataType;

    //  测试中产生的数据行数
    private int dataCount;

    // 集合类数据的子集合数量，如果是string类型，不需要此配置。
    private int subDataCount = 1;

    // value的长度, 用于生成随机字符串,单位字节
    private int valueLength;

    // Key 生成的模式，可选：“random”表示随机生成， “sequential”表示按顺序生成
    private String keyGenMode = "random"; // ,

    // key的起始索引，只在keyGenMode是sequential时使用
    private int keyStartIndex = 0; // used when keyGenerationMode is sequential

    // key的前缀，生成的key都会加上这个前缀。
    private String keyPrefix = "";

    // key的长度, 用于生成随机字符串的长度(不包括keyPrefix字符串),单位字节。
    //    0 表示使用默认值，如果是random生成规则会随机生成15-25长度的串；如果是sequential生成规则会直接是序列的数字本身。
    //    1~5 不允许使用，因为太短会导致冲突。
    //    >=6 如果是random生成规则会随机生成此长度的字符串；如果是sequential生成规则会在生成的数字前补0以补齐到指定长度。
    private int keyGenLength = 0;

    //  生成的数据文件, 相对于项目根目录
    private String dataFile;

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        if(dataType != null) {
            this.dataType = dataType.trim().toLowerCase();
        }
        else {
            this.dataType = dataType;
        }
    }

    public int getDataCount() {
        return dataCount;
    }

    public void setDataCount(int dataCount) {
        this.dataCount = dataCount;
    }

    public int getSubDataCount() {
        return subDataCount;
    }

    public void setSubDataCount(int subDataCount) {
        this.subDataCount = subDataCount;
    }

    public int getValueLength() {
        return valueLength;
    }

    public void setValueLength(int valueLength) {
        this.valueLength = valueLength;
    }

    public String getDataFile() {
        return dataFile;
    }

    public void setDataFile(String dataFile) {
        this.dataFile = dataFile;
    }

    public String getKeyGenMode() {
        return keyGenMode;
    }

    public void setKeyGenMode(String keyGenMode) {
        this.keyGenMode = keyGenMode;
    }

    public String getKeyPrefix() {
        return keyPrefix;
    }

    public void setKeyPrefix(String keyPrefix) {
        this.keyPrefix = keyPrefix;
    }

    public int getKeyStartIndex() {
        return keyStartIndex;
    }

    public void setKeyStartIndex(int keyStartIndex) {
        this.keyStartIndex = keyStartIndex;
    }

    public int getKeyGenLength() {
        return keyGenLength;
    }

    public void setKeyGenLength(int keyGenLength) {
        this.keyGenLength = keyGenLength;
    }

    public void validate() throws IllegalArgumentException {
        if(dataType == null || dataType.isEmpty()) {
            throw new IllegalArgumentException(toString() + "DataFileConfig dataType is null or empty!");
        }
        if(dataCount <= 0) {
            throw new IllegalArgumentException(toString() + "DataFileConfig dataCount must be greater than 0!");
        }
        if(valueLength < 0) {
            throw new IllegalArgumentException(toString() + "DataFileConfig valueLength must be non-negative!");
        }
        if(!keyGenMode.equals("random") && !keyGenMode.equals("sequential")) {
            throw new IllegalArgumentException(toString() + "DataFileConfig keyGenMode must be 'random' or 'sequential'!");
        }
        if(keyGenMode.equals("sequential") && keyStartIndex < 0) {
            throw new IllegalArgumentException("DataFileConfig keyStartIndex must be non-negative when keyGenMode is 'sequential'!");
        }
        if(keyGenLength < 0) {
            throw new IllegalArgumentException("DataFileConfig keyGenLength must be non-negative!");
        }
        if((dataType.equals("set") || dataType.equals("zset") || dataType.equals("list") || dataType.equals("hash") || dataType.equals("stream")) && subDataCount <= 0) {
            throw new IllegalArgumentException("DataFileConfig subDataCount must be greater than 0 for collection data types!");
        }
        if(dataFile == null || dataFile.isEmpty()) {
            throw new IllegalArgumentException("DataFileConfig dataFile is null or empty!");
        }
    }

    @Override
    public final boolean equals(Object o) {
        if (!(o instanceof DataFileConfig)) return false;

        DataFileConfig that = (DataFileConfig) o;
        return dataCount == that.dataCount && subDataCount == that.subDataCount && valueLength == that.valueLength && keyStartIndex == that.keyStartIndex && keyGenLength == that.keyGenLength && Objects.equals(dataType, that.dataType) && Objects.equals(keyGenMode, that.keyGenMode) && Objects.equals(keyPrefix, that.keyPrefix) && Objects.equals(dataFile, that.dataFile);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(dataType);
        result = 31 * result + dataCount;
        result = 31 * result + subDataCount;
        result = 31 * result + valueLength;
        result = 31 * result + Objects.hashCode(keyGenMode);
        result = 31 * result + keyStartIndex;
        result = 31 * result + Objects.hashCode(keyPrefix);
        result = 31 * result + keyGenLength;
        result = 31 * result + Objects.hashCode(dataFile);
        return result;
    }

    @Override
    public String toString() {
        return "DataFileConfig{" +
                "dataType='" + dataType + '\'' +
                ", dataCount=" + dataCount +
                ", subDataCount=" + subDataCount +
                ", valueLength=" + valueLength +
                ", keyGenMode='" + keyGenMode + '\'' +
                ", keyStartIndex=" + keyStartIndex +
                ", keyPrefix='" + keyPrefix + '\'' +
                ", keyGenLength=" + keyGenLength +
                ", dataFile='" + dataFile + '\'' +
                '}';
    }
}

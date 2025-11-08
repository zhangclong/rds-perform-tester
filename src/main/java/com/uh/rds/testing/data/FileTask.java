package com.uh.rds.testing.data;

import java.io.File;

public class FileTask {

    public final static int[] DEFAULT_VALUE_LENGTH_RANGE = new int[]{20, 100};

    public FileTask(File outFile, String dataType, int keyCount, int batchCount) {
        this(outFile, dataType, keyCount, batchCount, DEFAULT_VALUE_LENGTH_RANGE, "random", 0, "", 0);
    }

    public FileTask(File outFile, String dataType, int keyCount, int batchCount, int[] valueLengthRange) {
        this(outFile, dataType, keyCount, batchCount, valueLengthRange, "random", 0, "", 0);
    }

    public FileTask(File outFile, String dataType, int keyCount, int batchCount, int[] valueLengthRange,
                    String keyGenMode, int keyStartIndex, String keyPrefix, int keyGenLength) {
        this.outFile = outFile;
        this.dataType = dataType.toLowerCase();
        this.keyCount = keyCount;
        this.batchCount = batchCount;
        this.valueLengthMin = valueLengthRange[0];
        this.valueLengthMax = valueLengthRange[1];
        this.keyGenMode = keyGenMode;
        this.keyStartIndex = keyStartIndex;
        this.keyPrefix = keyPrefix;
        this.keyGenLength = keyGenLength;
    }

    final File outFile; //输出的文件

    final String dataType; //数据类型 "string", "set", "hash"

    final int keyCount; //key的数量

    final int batchCount; //每个key下面输出的数据条数

    final int valueLengthMin; //value的最小长度, 用于生成随机字符串

    final int valueLengthMax; //value的最大长度, 用于生成随机字符串

    // Key 生成的模式，可选：“random”表示随机生成， “sequential”表示按顺序生成
    final String keyGenMode; // ,

    // key的起始索引，只在keyGenMode是sequential时使用
    final int keyStartIndex; // used when keyGenerationMode is sequential

    // key的前缀，生成的key都会加上这个前缀。
    final String keyPrefix;

    // key的长度, 用于生成随机字符串的长度(不包括keyPrefix字符串),单位字节。
    //    0 表示使用默认值，如果是random生成规则会随机生成15-25长度的串；如果是sequential生成规则会直接是序列的数字本身。
    //    >=6 如果是random生成规则会随机生成此长度的字符串；如果是sequential生成规则会在生成的数字前补0以补齐到指定长度。
    final int keyGenLength;

}

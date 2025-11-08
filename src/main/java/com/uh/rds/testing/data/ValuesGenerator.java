package com.uh.rds.testing.data;

import java.util.Collection;
import java.util.Random;

public abstract class ValuesGenerator {

    private static final String CHARACTERS
            = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
            //"/\\'\"{}[]-_,.:;?~!@#$%^&*()_+中文北京欢迎您，。‘“；《》";
    private static final Random RANDOM = new Random();
    protected int valueLengthMin;
    protected int valueLengthMax;

    public static String generateRandomString(int minLength, int maxLength) {
        int length = RANDOM.nextInt(maxLength - minLength + 1) + minLength;
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            char c = CHARACTERS.charAt(RANDOM.nextInt(CHARACTERS.length()));
            builder.append(c);
            if(c == '\\') {
                builder.append(c); //如果是反斜杠，就把其转换为"\\\\"，因为反斜杠在字符串中是转义字符。
            }
        }
        return builder.toString();
    }

    public void setValueLengthMin(int valueLengthMin) {
        this.valueLengthMin = valueLengthMin;
    }

    public void setValueLengthMax(int valueLengthMax) {
        this.valueLengthMax = valueLengthMax;
    }

    /**
     * 返回输出的列
     * @return
     */
    abstract String[] getColumns();

    /**
     * 生成指定key下的一批数据。
     * @param key
     * @param batchSize 数据的行数
     * @return "Collection" 中的每一条对应一行数据，"String[]"中的每个数据对应的是每一列的数据值。
     */
    abstract Collection<String[]> generate(String key, int batchSize);


}

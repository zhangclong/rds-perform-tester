package com.uh.rds.testing.data;

import java.util.*;

public class ZsetValuesGenerator extends ValuesGenerator {
    @Override
    String[] getColumns() {
        return new String[]{"key", "index", "value"};
    }

    /**
     * 返回参数形式：</br>
     * [[key,index,value],[key1,index1,value1],[key2,index2,value2]]
     * @param key
     * @param batchSize 数据的行数
     * @return
     */
    @Override
    Collection<String[]> generate(String key, int batchSize) {
        List<String[]> l = new ArrayList<>();
        for (int i = 0; i < batchSize; i++) {
            l.add(new String[]{String.valueOf(i), generateRandomString(valueLengthMin, valueLengthMax)});
        }
        return l;
    }
}

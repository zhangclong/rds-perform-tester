package com.uh.rds.testing.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class StreamDataGenerator extends ValuesGenerator {
    @Override
    String[] getColumns() {
        return new String[]{"key", "value_key", "value_val"};
    }

    @Override
    Collection<String[]> generate(String key, int batchSize) {
        List<String[]> l = new ArrayList<>();
        for (int i = 0; i < batchSize; i++) {
            l.add(new String[]{generateRandomString(5, 30), generateRandomString(valueLengthMin, valueLengthMax)});
        }
        return l;
    }
}

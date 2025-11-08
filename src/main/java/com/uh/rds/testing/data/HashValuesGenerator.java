package com.uh.rds.testing.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class HashValuesGenerator extends ValuesGenerator {

    final static String[] columns = {"key", "field", "value"};

    @Override
    String[] getColumns() {
        return columns;
    }

    @Override
    Collection<String[]> generate(String key, int batchSize) {
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < batchSize; i++) {
            map.put(generateRandomString(5, 30), generateRandomString(valueLengthMin, valueLengthMax));
        }
        ArrayList l = new ArrayList();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            String[] v = new String[] {entry.getKey() , entry.getValue() };
            l.add(v);
        }

        return l;
    }
}

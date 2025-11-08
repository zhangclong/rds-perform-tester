package com.uh.rds.testing.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class SetValuesGenerator extends ValuesGenerator {

    final static String[] columns = {"key", "value"};

    @Override
    String[] getColumns() {
        return columns;
    }

    @Override
    Collection<String[]> generate(String key, int batchSize) {
        Set<String> set = new HashSet<>();
        for (int i = 0; i < batchSize; i++) {
            set.add(generateRandomString(valueLengthMin, valueLengthMax));
        }

        ArrayList l = new ArrayList();
        for(String s : set) {
            String[] v = new String[] {s};
            l.add(v);
        }
        return l;
    }
}

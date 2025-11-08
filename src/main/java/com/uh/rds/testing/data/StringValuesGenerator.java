package com.uh.rds.testing.data;

import java.util.ArrayList;
import java.util.Collection;

public class StringValuesGenerator extends ValuesGenerator {

    final static String[] columns = {"key", "value"};

    @Override
    String[] getColumns() {
        return columns;
    }

    @Override
    Collection<String[]> generate(String key, int batchSize) {
        String[] v = new String[]{generateRandomString(valueLengthMin, valueLengthMax)};
        ArrayList l = new ArrayList();
        l.add(v);
        return l;
    }
}

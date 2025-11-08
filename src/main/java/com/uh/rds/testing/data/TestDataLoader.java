package com.uh.rds.testing.data;

import com.opencsv.CSVReader;

import java.io.File;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 从 CSV 文件中读取测试数据。
 */
public class TestDataLoader {

    /**
     * 从 CSV 文件中读取数据，返回一个 Map 对象。
     * @param csvFile
     * @return  返回的 Map 对象的 key 是第一列的值，value 是第二列的值。
     */
//    public static Map<String, String> readStringData(File csvFile) {
//        Map<String, String> dataMap = new LinkedHashMap<>();
//
//        try (CSVReader reader = new CSVReader(new FileReader(csvFile))) {
//            String[] line;
//            reader.readNext();  // Skip the first line (header)
//
//            while ((line = reader.readNext()) != null) {
//                String key = line[0];
//                String value = line[1];
//                dataMap.put(key, value);
//            }
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//
//        return dataMap;
//    }


    /**
     * 从 CSV 文件中读取数据，返回一个 Map 对象。
     * @param csvFile
     * @return  返回的 Map 对象的 key 是第一列的值，value 是后面1列到多列的值集合。
     */
    public static Map<String, List<String[]>> readSubsetData(File csvFile) {
        Map<String, List<String[]>> dataMap = new LinkedHashMap<>();

        String lastKey = null;
        try (CSVReader reader = new CSVReader(new FileReader(csvFile, StandardCharsets.UTF_8))) {
            String[] line;
            reader.readNext();  // Skip the first line (header)

            List<String[]> valuesList = null;

            while ((line = reader.readNext()) != null) {
                String key = line[0];
                boolean keyChanged = !key.equals(lastKey);

                if(keyChanged) {
                    if(valuesList != null) {
                        dataMap.put(lastKey, valuesList);
                    }
                    valuesList = new ArrayList<>();
                }

                String[] values = getValues(line);
                valuesList.add(values);

                lastKey = key;
            }

            //插入最后一组数
            if(valuesList != null) {
                dataMap.put(lastKey, valuesList);
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return dataMap;
    }

    /**
     * 返回转换后的数组，它会把原数组 String[] 的第一个元素去掉。
     * @param lineFields
     * @return
     */
    private static String[] getValues(String[] lineFields) {
        int fLen = lineFields.length;
        String[] values = new String[fLen - 1];
        for(int i=1 ; i<fLen ; i++) {
            values[i-1] = lineFields[i];
        }
        return values;
    }



    public static List<Map<String, List<String[]>>> splitMapList(Map<String, List<String[]>> originalMap, int parts) {
        int size = originalMap.size();
        int subSize = size / parts;
        List<Map<String, List<String[]>>> subMaps = new ArrayList<>(parts);
        Iterator<Map.Entry<String, List<String[]>>> iterator = originalMap.entrySet().iterator();

        for (int i = 0; i < parts; i++) {
            Map<String, List<String[]>> subMap = new LinkedHashMap<>();
            for (int j = 0; j < subSize && iterator.hasNext(); j++) {
                Map.Entry<String, List<String[]>> entry = iterator.next();
                subMap.put(entry.getKey(), entry.getValue());
            }
            subMaps.add(subMap);
        }

        // Handle remaining entries
        while (iterator.hasNext()) {
            Map.Entry<String, List<String[]>> entry = iterator.next();
            subMaps.get(subMaps.size() - 1).put(entry.getKey(), entry.getValue());
        }

        return subMaps;
    }


//    public static List<Map<String, String>> splitMap(Map<String, String> originalMap, int parts) {
//        int size = originalMap.size();
//        int subSize = size / parts;
//        List<Map<String, String>> subMaps = new ArrayList<>(parts);
//        Iterator<Map.Entry<String, String>> iterator = originalMap.entrySet().iterator();
//
//        for (int i = 0; i < parts; i++) {
//            Map<String, String> subMap = new LinkedHashMap<>();
//            for (int j = 0; j < subSize && iterator.hasNext(); j++) {
//                Map.Entry<String, String> entry = iterator.next();
//                subMap.put(entry.getKey(), entry.getValue());
//            }
//            subMaps.add(subMap);
//        }
//
//        // Handle remaining entries
//        while (iterator.hasNext()) {
//            Map.Entry<String, String> entry = iterator.next();
//            subMaps.get(subMaps.size() - 1).put(entry.getKey(), entry.getValue());
//        }
//
//        return subMaps;
//    }

}


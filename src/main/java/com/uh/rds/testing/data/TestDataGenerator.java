package com.uh.rds.testing.data;

import com.opencsv.CSVWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static com.uh.rds.testing.data.ValuesGenerator.generateRandomString;

public class TestDataGenerator {

    Logger logger = LoggerFactory.getLogger(TestDataGenerator.class);

    final static int KEY_LEN_MIN = 15;
    final static int KEY_LEN_MAX = 25;

    Map<String, ValuesGenerator> typesGenerator;

    List<FileTask> tasks;

    public  TestDataGenerator() {
        tasks = new ArrayList<>();
        typesGenerator = new HashMap<>();
        typesGenerator.put("string", (ValuesGenerator)new StringValuesGenerator());
        typesGenerator.put("set", (ValuesGenerator)new SetValuesGenerator());
        typesGenerator.put("list", (ValuesGenerator)new SetValuesGenerator());
        typesGenerator.put("hash", (ValuesGenerator)new HashValuesGenerator());
        typesGenerator.put("zset", (ValuesGenerator)new ZsetValuesGenerator());
        typesGenerator.put("stream", (ValuesGenerator)new StreamDataGenerator());
    }


    public boolean addTask(FileTask task) {
        return this.tasks.add(task);
    }

    public boolean addNoExistTask(FileTask task) {
        if(task.outFile.exists()) {
            logger.warn("Task file {} already exists, will not add it again!", task.outFile.getAbsolutePath());
            return false;
        }

        return this.addTask(task);
    }

    public int getTaskCount() {
        return this.tasks.size();
    }

    public void cleanTasks() {
        this.tasks.clear();
    }

    public int executeTasks() {
        int executed = 0;

        Set<String> keys = new HashSet<>();
        for(FileTask task : tasks) {
            boolean generateModeSequential = task.keyGenMode.equalsIgnoreCase("sequential");

            ValuesGenerator generator = typesGenerator.get(task.dataType);

            generator.setValueLengthMin(task.valueLengthMin);
            generator.setValueLengthMax(task.valueLengthMax);

            //如果输出文件的文件夹不存在，就创建该文件夹
            createNoExistsFolder(task.outFile);

            try (CSVWriter writer = new CSVWriter(new FileWriter(task.outFile, StandardCharsets.UTF_8),
                    CSVWriter.DEFAULT_SEPARATOR,
                    CSVWriter.DEFAULT_QUOTE_CHARACTER,
                    CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                    CSVWriter.DEFAULT_LINE_END)
            ) {
                int columnLen = generator.getColumns().length;
                writer.writeNext(generator.getColumns());
                for (int i = 0; i < task.keyCount; i++) {
                    //插入一条不重复的Key
                    String key;

                    if(generateModeSequential) {
                        key = task.keyPrefix + getPaddedIndexString(task.keyStartIndex + i, task.keyGenLength);
                    }
                    else {
                        int duplicateRetry = 0;
                        do {
                            //random模式
                            if (task.keyGenLength >= 6) {
                                key = task.keyPrefix + generateRandomString(task.keyGenLength, task.keyGenLength);
                            } else {
                                key = task.keyPrefix + generateRandomString(KEY_LEN_MIN, KEY_LEN_MAX);
                            }

                            if (duplicateRetry > 10) {
                                throw new RuntimeException("Failed to generate non-duplicate key after 10 retries. Current key: " + key);
                            }
                            duplicateRetry++;
                        } while (keys.contains(key));
                    }

                    keys.add(key);

                    //Collection的每一个值是一个String[]，每一个String[]是一行数据（其中的每个值代表一列）
                    Collection<String[]> values = generator.generate(key, task.batchCount);
                    for (String[] v : values) {
                        if (v.length != columnLen - 1) {
                            throw new IllegalArgumentException("Generated values is not match the columns count!");
                        }

                        String[] rowData = new String[columnLen];
                        rowData[0] = key;
                        for (int j = 1; j < columnLen; j++) {
                            rowData[j] = v[j - 1];
                        }
                        writer.writeNext(rowData);
                    }
                }
                executed ++;
                logger.info("TestDataGenerator Task({}/{}) executed successfully!", task.outFile.getParentFile().getName(), task.outFile.getName());
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
        }
        return executed;
    }

    private void createNoExistsFolder(File targetFile) {
        File parentFolder = targetFile.getParentFile();
        if(parentFolder != null && (parentFolder.exists() == false)) {
            parentFolder.mkdirs();
        }
    }

    /**
     * 根据总长度对索引整数进行补齐。例如：总长度是5，索引是3，则返回"00003"
     * @param index
     * @param totalLength
     * @return
     */
    private String getPaddedIndexString(int index, int totalLength) {
        String indexStr = String.valueOf(index);
        int indexStrLen = indexStr.length();
        if(totalLength > indexStrLen) {
            return String.format("%0" + (totalLength) + "d", index);
        }
        else {
            return indexStr;
        }
    }




//    public static void main(String[] args) {
//        File baseDir = new File("data");
//        TestDataGenerator g = new TestDataGenerator();
        //g.generateStringCsv(new File(baseDir, "string-data.csv"), 20000);
//        g.generateSetCsv(new File(baseDir, "set-data.csv"), 100, 20);
//        g.generateHashCsv(new File(baseDir, "hash-data.csv"), 100, 20);
//    }

//    public void generateStringCsv(File file, int rows) {
//        String[] columns = {"key", "value"};
//        generateCSVFile(file, rows, columns, (key) -> {
//            String[] v = new String[]{generateRandomString(20, 100)};
//            ArrayList l = new ArrayList();
//            l.add(v);
//            return l;
//        });
//    }
//
//    public void generateSetCsv(File file, int rows, int setSize) {
//        String[] columns = {"key", "value"};
//        generateCSVFile(file, rows, columns, (key) -> {
//            Set<String> set = new HashSet<>();
//            for (int i = 0; i < setSize; i++) {
//                set.add(generateRandomString(20, 100));
//            }
//
//            ArrayList l = new ArrayList();
//            for(String s : set) {
//                String[] v = new String[] {s};
//                l.add(v);
//            }
//            return l;
//        });
//    }
//
//    public void generateHashCsv(File file, int rows, int hashSize) {
//        String[] columns = {"key", "field", "value"};
//        generateCSVFile(file, rows, columns, (key) -> {
//            Map<String, String> map = new HashMap<>();
//            for (int i = 0; i < hashSize; i++) {
//                map.put(generateRandomString(5, 30), generateRandomString(20, 100));
//            }
//            ArrayList l = new ArrayList();
//            for (Map.Entry<String, String> entry : map.entrySet()) {
//                String[] v = new String[] {entry.getKey() , entry.getValue() };
//                l.add(v);
//            }
//
//            return l;
//        });
//    }



//    private void generateCSVFile(File file, int rows, String[] columns, ValuesGenerator generator) {
//        Set<String> keys = new HashSet<>();
//        try (CSVWriter writer = new CSVWriter(new FileWriter(file))) {
//            writer.writeNext(columns);
//            for (int i = 0; i < rows; i++) {
//                //插入一条不重复的Key
//                String key;
//                do {
//                    key = generateRandomString(10, 30);
//                } while (keys.contains(key));
//                keys.add(key);
//
//                //Collection的每一个值是一个String[]，每一个String[]是一行数据（其中的每个值代表一列）
//                Collection<String[]> values = generator.generate(key);
//                for(String[] v : values) {
//                    if(v.length != columns.length - 1) {
//                        throw new IllegalArgumentException("Invalid number of values");
//                    }
//
//                    String[] rowData = new String[columns.length];
//                    rowData[0] = key;
//                    for(int j = 1; j < columns.length; j++) {
//                        rowData[j] = v[j - 1];
//                    }
//                    writer.writeNext(rowData);
//                }
//
//
//
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }



    /**
     * 用于生成数据的接口
     * 针对某一个key生成其对应的一组数据。
     * 例如，对于一个hash类型的key，生成其对应的多个field-value对，每个field-value对对应Collection<String[]>的一行数据；
     * String[]中的每个值对应一列数据，例如field-value对中的field对应String[0]，value对应String[1]。
     */
//    public interface ValuesGenerator {
//        Collection<String[]> generate(String key);
//    }
}

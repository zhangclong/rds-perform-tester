# rds-perform-tester性能测试工具使用说明 # 


## 简介
rds-perform-tester(Redis Performance Tester) 是一个通过配置驱动的 Redis/RDS 高并发性能测试工具。采用 Java 语言编写，基于 [Jedis] (5.x.x) 实现。
采用高并发多线程方式模拟客户端对 Redis/RDS 进行读写操作，支持单机、主从、哨兵、集群等多种部署模式。

## 特点和优势
 - 工具性能更优：在相同的软硬件环境下，相同被压测redis集群情况下，压测的OPS值约比redis-benchmark或memtier_benchmark高5%~10%左右。
   - 采用提前准备数据的方式，会在压测前加载所有的测试数据，避免测试过程中生成损耗CPU资源。
   - 采用多线程方式，每个线程会事先独立分配好数据和redis连接，集群模式下会按分片提前分配好对应的线程、连接和数据，运行时分配带来的资源损耗。
 - 模拟真实数据，测试数据通过工具提前生成为数据文件，可以保证每次测试的数据完全一致。
 - 命令可以随意组合，可以像redis-cli类似的客户端一样，执行按命令语法执行任意的命令。如： SET key value、GET key、HSET key field value、ZADD key score member等。
 - 测试过程可以进行验证，进行多种高速并发场景的测试。
   - 写入读取验证，数据集合是在测试前提前准备好的，是固定集合固定顺序执行。可按照写入顺序读取验证，验证数据的正确性。
   - 执行命令和验证数据可以进行参数化配置，如： 命令为 SET ${KEY} ${VALUE}  读取验证为 GET ${KEY} 校验 ${VALUE}。可以通过配置文件灵活配置。
   - 支持多种数据类型的读写验证，如：String、Hash、List、Set、ZSet等。
   - 可进行不同指令的读写比例调整，如：SET/GET 读写比例为1:1，SET/GET 读写比例为1:9等。
   - 多种执行控制，可以设置每个执行命令间的等待毫秒数，指定执行命令的重复次数。
 
## 使用环境
 - JDK 11 及以上版本
 - 被测试系统 Redis 5.0及以上版本、TongRDS 2.2.1.4及以上版本，支持单机、主从、哨兵、集群等多种部署模式。

## 使用说明
### 目录结构说明
```declarative
    conf                             # 配置文件目录
    ├── tests                      # 性能测试各测试用例的配置文件
        ├── HashPerformTest.yml      # Hash类型数据的测试用例配置文件
        └── StringPerformTest.yml    # String类型数据的测试用例配置文件
    └── perform-config.yml           # 主配置文件
    data                             # 测试数据存放路径
    ├── string-data.csv          # 名为string-data的数据文件
    └── string-data.properties   # 名为string-data的数据文件的属性文件，最近一次数据文件生成时的配置信息，用于对比发现配置的变更
    logs                             # 日志文件目录
    ├── commands.log                 # 执行的命令校验信息日志，主要输出校验异常
    ├── error.log                    # 错误日志
    ├── statistic.log                # 运行时统计信息日志
    └── system.log                   # 所有日志汇总，方便总体查看
    rds-testing.jar                  # 测试工具执行文件
    using-guide.md                   # 使用说明文档
```

### 测试执行过程
我们以集群模式下 String 类型数据的读写测试为例，介绍测试的执行过程。其他模式和数据类型的测试执行过程类似。

#### 1. 准备被测试环境
确保被测试的 Redis/RDS 集群已经启动并可访问。记录下集群的节点信息（IP 地址和端口号），记录好集群的分片信息。

#### 2. 配置连接参数
编辑主配置文件 `conf/perform-config.yml`下的 connections 配置段，配置连接参数。配置示例如下：
```yaml
connections:
  localCluster:
    # 表示集群模式
    mode: "cluster"
    # 连接密码
    password: "123456"
    # 分片配置，格式为 "起始槽位-结束槽位"，注意数组元素的数量必须等于集群的分片数量。
    shards: [ "0-5460", "5461-10921", "10922-16383"]
    # 地址列表，集群模式下元素个数等于分片数，每个元素格式为：“IP:端口,IP:端口,IP:端口”，其中第一个地址为主节点，后续地址为该分片的备节点。
    # 下面是三主三从的集群配置示例，数组中的元素对应shards配置的分片元素，表示每个分片的主从节点地址。
    endpoints: ["127.0.0.1:6331,127.0.0.1:6332", "127.0.0.1:6333,127.0.0.1:6334", "127.0.0.1:6335,127.0.0.1:6336"]
    # 超时时间，默认2000毫秒
    timeout: 5000
```
注意,这里‘localCluster’是这个连接配置的名称，可以自定义。后续在测试用例配置文件中会引用这个名称。

#### 3. 配置测试数据
编辑主配置文件 `conf/perform-config.yml`下的 dataFiles 配置段，配置测试数据。配置示例如下：
```yaml
dataFiles:
  string-data:
    # 数据类型, 可选取值 string, set, zset, list, hash, stream
    dataType: "string"
    # 测试中产生的数据行数
    dataCount: 18000000
    # Key 生成的模式，可选：“random”表示随机生成， “sequential”表示按顺序生成
    keyGenMode: "random"
    # key的起始索引，只在keyGenMode是sequential时使用
    keyStartIndex: 0
    # key的前缀，生成的key都会加上这个前缀。
    keyPrefix: "s"
    # key的长度, 用于生成随机字符串的长度(不包括keyPrefix字符串),单位字节。
    #    0 表示使用默认值，如果是random生成规则会随机生成15-25长度的串；如果是sequential生成规则会直接是序列的数字本身。
    #    1~5 不允许使用，因为太短会导致冲突。
    #    >=6 如果是random生成规则会随机生成此长度的字符串；如果是sequential生成规则会在生成的数字前补0以补齐到指定长度。
    keyGenLength: 0
    # 集合类数据的子集合数量，如果是string类型，不需要此配置。
    # subDataCount: 1
    #value的长度, 用于生成随机字符串,单位字节
    valueLength: 200
    # 生成的数据文件, 相对于项目根目录
    dataFile: "data/string-data.csv"
```
注意,这里‘string-data’和‘hash-data’是数据文件配置的名称，可以自定义。后续在测试用例配置文件中会引用这个名称。

#### 4. 配置测试用例
测试用例文件放置在`conf/perform/`目录下以`*Test.yml`或`*Test.yaml`为文件名结尾的文件。 这里以 `conf/perform/StringPerformTest.yml`为示例说明配置项目和用途：
```yaml
tests:
  - id: "1.StringPerformanceTest"
    description: "验证Hash数据类型，HSET后验证HSCAN命令是否正常执行，以及执行效率"
    configs:
      # 指定数据文件名称，必须在配置中的dataFiles中有相应的数据文件配置
      dataFileName: "string-data"
      # 指定连接的名称，必须在配置中的connections中有相应的连接配置
      connectionName: "localSingle"
      # 是否在测试前先清空DB
      flushBefore: true
      # 是否在测试后清理测试数据，根据key值逐条清理
      cleanAfter: false
      # 是否从从节点读取数据, 默认是false
      readFromSlave: false
      # 测试时同时产生的并发线程数
      threads: 20
      #每个线程中的客户端数
      threadClients: 50
      # 每个客户端循环执行的次数, 如果是0表示无限循环；默认值为1
      loopCount: 5
      # 每隔1000毫秒打印一次状态信息, 默认是1000
      stateInterval: 5000
      # 是否运行时解析命令行中的变量表达式，运行时解析会耗用较少的内存但会影响运行效率增大CPU使用，默认是false
      runtimeParse: true
      ############### 测试命令相关配置 ################
      # 下面执行命令中的每个命令行可使用的属性:
      #  line：是带参数的命令行，运行时会自动替换 ${KEY} ${VALUE} ${VALUE1} ${VALUE2} 为相应的数据值,  其中 ${VALUE}==${VALUE1}
      #  returnType: 指定返回值的类型，默认是STRING类型，可选值有 STRING, LONG。如果返回值是其他类型如 List, Set, Map 等集合类型，会转换为String并按STRING类型进行比较。
      #  returnAssert： 是执行这个命令时，返回值的校验，如果不等于这个值就会认为执行失败，运行时会自动替换 ${KEY} ${VALUE} ${VALUE1} ${VALUE2} ${VALUE3} ${VALUE4} ${VALUE5} 为相应的数据值。
      #               "#{NOT_EMPTY}" 表示不为空就校验通过；"#{EMPTY}" 表示为空（String类型空串或null，Long类型为0或null）就校验通过。
      #  repeatTimes： 命令重复执行的次数，默认是1次。
      #  sleep: 执行等待的时长（单位是毫秒）。该属性是排它的，如果配置了sleep属性其他属性将被忽略。
      # 逐个子项循环执行的命令，如果是String类型相当于子项只有一个，set, zset, list, hash这些类型都是集合内子项的循环。
      commands: [
        {line: "SET ${KEY} ${VALUE}", returnAssert: "OK", returnType: "STRING"},
        # {sleep: 100},  # 执行等待，单位是毫秒
        {line: "GET ${KEY}", returnAssert: "${VALUE}", returnType: "STRING", repeatTimes: 9}
      ]

      # 表示按key逐一循环执行，并在commands定义的子项循环前执行。
      keyBeforeCommands: [ ]

      # 表示按key逐一循环执行，并在commands定义的子项循环后执行。
      keyAfterCommands: [ ]
```
注意：
1. 这里的`id`是测试用例的唯一标识，测试执行会按此属性的字母顺序正序来执行。
2. `dataFileName`是引用数据文件配置的名称，必须与前面配置的名称一致。如果数据文件不存在，工具会自动生成数据文件。
3. 两个测试用例可以引用同一个数据文件，通过这种方式可以做数据关联性测试，如：前一个用例写入数据，后一个用例读取验证数据。
4. `connectionName` 是引用连接配置的名称，必须与前面配置的名称一致，同一个连接配置可以被多个测试用例引用，但注意执行每个测试用例时都会重新连接。
5. `commands`定义了测试过程中要执行的命令，可以按需修改或增加命令。`line`命令中可以使用 `${KEY}` `${VALUE}` `${VALUE1}` `${VALUE2}` 这些变量，`returnAssert`中还支持 `${VALUE3}` `${VALUE4}` `${VALUE5}` 及 `${KEY}` 占位符，运行时会自动替换为相应的数据值。`returnAssert`另外改用 `"#{NOT_EMPTY}"` 表示不为空即通过，`"#{EMPTY}"` 表示为空（String类型空串或null，Long类型为0或null）即通过。`returnType`指定 STRING 或 LONG，当命令返回 List、Set、Map 等集合类型时，会自动转换为String再按STRING类型比较。
6. 如果想不执行某个测试用例，可以加入 `disable: true` 属性来禁用该测试用例。

#### 5. 运行测试
在命令行窗口进入到项目根目录，执行以下命令启动测试runtest.sh(Windows系统执行runtest.bat)：
```bash
./runtest.sh
```
执行此命令后会自动加载 `conf/perform-config.yml` 主配置文件，并按测试用例的`id`字母顺序正序执行 `conf/perform/` 目录下所有的测试用例文件中的测试用例。   
_**注意：**_
如果测试加载的数据量比较大，建议增加JVM内存参数，可以在runtest.sh脚本中修改JAVA_OPTS参数，例如：
```bash
# JVM参数
JVM_OPTS="-Xmx8g -XX:MaxMetaspaceSize=512m -XX:+UseG1GC"
```

#### 6. 查看测试结果
测试过程中会在命令行窗口打印运行状态信息，测试完成后会打印测试结果汇总信息。
所有测试的日志文件会保存在 `logs/` 目录下.
statistic.log日志会定时输出统计信息，error.log日志会输出错误信息, commands.log日志会输出命令执行的校验异常信息。
除commands.log日志外，其他日志信息均会输出到system.log和运行终端中，方便查看。
所有日志文件会每天生成一个新文件，文件名中会包含日期信息。

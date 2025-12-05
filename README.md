# rds-perform-tester性能测试工具 # 

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

## 快速开始
1. 下载并解压工具包。
2. 调整连接配置，修改 conf 目录下的 perform-config.yml 文件，这里假设你部署了单节点 Redis 服务，地址配置在：
   connections.localSingle.endpoints: [ "127.0.0.1:6379" ]  # 修改为你的 Redis 服务地址。
3. 简单说明一下配置各元素的关系和作用：
   - perform-config.yml 配置文件中包含两个主要部分：
      - connections：定义了多个连接配置，可以配置单节点、主从、哨兵、集群等多种模式的 Redis/RDS 服务连接。connections下的元素名称可以自定义，如文件示例中还包含的 localSingle、localMasterSlave、localSentinel、localCluster 等连接配置项。
      - dataFiles: 定义了多个数据文件配置，每个数据文件配置指定了数据文件路径、数据类型、数据量等信息。dataFiles下的元素名称可以自定义，如文件示例中还包含的 string-data、hash-data 等数据文件配置。
   - tests目录下的 *.yml 文件定义了多个测试用例，一般yml文件对应一个测试用例，每个测试用例指定了要使用的连接配置、数据文件配置、执行命令、读写比例等信息。
      - 示例中的 tests/StringPerformTest.yml 文件定义了一个字符串类型的性能测试用例，使用了 localSingle 连接配置和 string-data 数据文件配置，执行 SET 和 GET 命令，读写比例为 1:9
      - 示例中的 tests/HashPerformTest.yml 文件定义了一个哈希类型的性能测试用例，使用了 localSingle 连接配置和 hash-data 数据文件配置，执行 HSET 和 HGET 命令，读写比例为 1:9
4. 整个测试工具是配置驱动的，用户只需要根据自己的需求修改配置文件后，执行 bin/runtest.sh（runtest.bat for Windows） 脚本即可开始测试。
   - 执行顺序是根据 tests 目录下的 yml 中个 tests.id 文本的字母顺序来逐一执行测试用例。示例中会先执行 "1.StringPerformanceTest"、再执行 "2.HashPerformanceTest"。
   - 运行每个测试用例的顺序是：
      - 加载连接配置，建立与 Redis/RDS 服务的连接池。
      - 加载数据文件，准备好测试数据；会检查数据文件是否存在，若不存在会自动生成数据文件。
      - 执行测试命令，根据配置的进行命令执行和数据验证。
   - 运行结果会输出到控制台，并生成日志文件在 logs/ 目录下。
   
## 使用说明
详细使用说明参看 ["usring-guide.md"](https://gitee.com/zhangclong1/rds-perform-tester/blob/master/using-guide.md)

## 下载地址
["perform-tester-1.1.2.tar.gz"](https://gitee.com/zhangclong1/rds-perform-tester/releases/download/RELEASE-1.1.2/perform-tester-v1.1.2.tar.gz)
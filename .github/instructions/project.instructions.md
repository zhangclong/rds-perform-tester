# rds-perform-tester Project Instructions

## 你在这个仓库中的角色
你是在维护一个“通过 YAML 配置定义压测行为”的工具，而不是业务应用。实现需求时优先复用现有配置能力。

## 配置关系（必须保持一致）
- 主程序是 `com.uh.rds.testing.performance.PerformanceTestMain`，运行时必须指定 `apphome` 为工作路径。
- `apphome` 的初始内容来自 `src/main/release/perform-tester`（Maven `process-resources` 阶段拷贝）。
- `src/main/release/perform-tester/conf/` 是完整注释的初始样例配置；改动配置相关逻辑时要保持注释完整且与程序行为一致。
- `conf/perform-config.yml`
  - `connections.<name>` 被测试用例通过 `connectionName` 引用。
  - `dataFiles.<name>` 被测试用例通过 `dataFileName` 引用。
- `conf/perform/*Test.yml`
  - 每个文件代表一个测试用例，运行时会逐一按其中配置执行。
  - `tests[].id` 决定执行顺序（字母顺序）。
  - `configs.commands` 支持 `${KEY}` `${VALUE}` `${VALUE1}` `${VALUE2}` 占位符。

## 对配置或代码进行修改时
- 保持最小改动，优先补充/修正配置，不随意变更执行链路。
- 新增示例时，确保注释解释清楚模式差异（single/master-slave/sentinel/cluster）。
- cluster 配置下，`shards` 数量必须与 `endpoints` 分片元素数量一致。

## 验证标准
- 至少保证脚本可启动并进入测试流程：`./runtest.sh`。
- 输出检查重点：
  - 周期状态输出中的吞吐（ops/s）；
  - 汇总统计中的线程数、操作次数、断言失败次数、响应时间分位值；
  - `commands.log` 中是否存在校验失败。

## 文档与说明
- 与使用方式相关的改动，需同步 `README.md` 或 `using-guide.md` 中对应段落。
- 对 `src/main/release/perform-tester/conf/**/*.yml` 的任何修改或补充，检查 `using-guide.md` 中是否有对应描述，如果有要同步更新；如果该改动也影响到 `README.md` 的内容，则也需要同步修改 `README.md`。
- 文档术语保持一致：`connections`、`dataFiles`、`tests.id`、`commands`、`loopCount`、`threads`、`threadClients`。

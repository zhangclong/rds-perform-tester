# Copilot Instructions for rds-perform-tester

## 项目定位
- 这是一个**配置驱动**的 Redis/RDS 高并发性能测试工具（Java 11+，Jedis 5.x）。
- 通过多线程与预生成数据文件执行压测，支持 `master`、`master-slave`、`sentinel`、`cluster` 等连接模式。
- 测试核心不是硬编码逻辑，而是 `conf/perform-config.yml` 与 `conf/perform/*Test.yml` 的组合。

## 关键目录与文件约定
- 主程序入口：`com.uh.rds.testing.performance.PerformanceTestMain`，运行时必须指定 `apphome` 作为工作路径。
- `apphome` 初始内容来自 `src/main/release/perform-tester`，由 Maven `process-resources` 阶段拷贝生成。
- `src/main/release/perform-tester/conf/` 是带完整注释的初始/样例配置来源。
- `conf/perform-config.yml`：主配置（`connections`、`dataFiles`）。
- `conf/perform/*Test.yml|*Test.yaml`：每个文件代表一个测试用例，运行时会逐一按配置执行（用例内仍按 `tests[].id` 字母顺序）。
- `data/*.csv`：测试数据文件；若不存在会按配置自动生成。
- `logs/`：运行日志（`statistic.log`、`error.log`、`commands.log`、`system.log`）。
- 入口脚本：`runtest.sh`（Windows 为 `runtest.bat`）。

## 修改代码前的工作方式
1. 先确认需求能否通过**调整配置**解决，避免不必要改代码。
2. 涉及测试流程时，同时检查：
   - 主配置中的 `connectionName` / `dataFileName` 引用是否一致；
   - `commands` 中变量占位符（`${KEY}` `${VALUE}` `${VALUE1}` `${VALUE2}`）是否正确。
3. 延续现有风格：小步改动、局部修改、避免引入与任务无关的重构。

## 测试与验证
- 优先使用现有脚本验证：`./runtest.sh`。
- 关注日志：
  - 吞吐与时延看 `statistic.log`；
  - 命令校验异常看 `commands.log`；
  - 运行错误看 `error.log` 和 `system.log`。
- 若数据量大导致内存不足，建议在脚本中调整 `JVM_OPTS`（如 `-Xmx`）。

## 变更边界与安全准则
- 修改程序涉及配置项时，必须同步维护 `src/main/release/perform-tester/conf/` 中配置注释，保证注释说明完整且与实际行为一致。
- 对 `src/main/release/perform-tester/conf/**/*.yml` 的任何修改或补充，都必须同步更新 `using-guide.md`；如果该改动也影响到 `README.md` 的内容，则也需要同步修改 `README.md`。
- 不要随意改动默认示例配置语义（尤其是 cluster 的 `shards` 与 `endpoints` 对应关系）。
- 不要删除用户已有测试用例；新增用例应保持可独立启停（可用 `disable: true` 控制）。
- 不要引入与压测无关的依赖或框架；保持工具轻量、可脚本化运行。

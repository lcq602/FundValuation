# Changelog

## 2026-05-25 — 初始版本：guzhi → Java 迁移 + 刷新策略重构

### 迁移 (guzhi Python Flask → FundValuation Spring Boot)
- 完整迁移 3 个 API 端点到 Java 17 + Spring Boot 4.0.6
- 16 个 Java 源文件，覆盖 model/dto/controller/service/config
- 基金数据通过 `funds/*.json` 加载，使用 `ResourcePatternResolver` 支持 JAR 包运行
- Jackson 全局 `SNAKE_CASE` 命名策略，与 Python 版 JSON 字段格式一致

### 刷新策略重构（相对原 Python 版的改进）

**问题**：原版将所有数据捆绑每 60 秒刷新一次，净值抓取过于频繁，行情更新不够及时。

| 维度 | 原 Python 版 | 现 Java 版 |
|------|-------------|-----------|
| 行情刷新 | 每 60 秒（不分时段） | 交易时段每 15 秒，非交易时段每 5 分钟 |
| 净值刷新 | 每 60 秒 | 交易时段每 30 分钟，非交易时段不刷新 |
| 失败处理 | 数据被 0 覆盖 | 失败保留上次成功数据 |
| 净值重试 | 无 | 失败后 30 秒重试，最多 3 次 |
| 线程管理 | `threading.Thread` + `while True` | Spring `@Scheduled` + 4 线程池 |
| 启动行为 | 启动时阻塞等待远程 API | 骨架快照立即返回，后台线程延迟加载 |
| 市场时间 | 无感知 | 工作日 9-17 北京时间判断 |

### 代码质量
- 统一 DTO 返回值（新增 `FundsResponse`）
- CORS 配置
- 单元测试：`EstimateResultTest` (7 测试) + `FundControllerTest` (5 测试)
- 启动时间：从 ~2s 降至 ~0.76s（测试环境）

### 2026-05-25 第2轮 — 修复贡献值计算（归一化权重 → 原始权重）

**问题**：`weight_contribution` 使用归一化权重（`ratio / totalRatio`），导致贡献值被放大。
例如 TSM 实际权重 10.2%，涨 10% → 贡献被算成 1.855%，正确应为 1.02%。

**修复**：
- `EstimateResult.calculate`：直接用 `ratio * stockReturn`，不再归一化
- `buildEnrichedHoldings`：同上，`weight_contribution` 直接用原始 ratio
- 新增 `calculateWithPartialHoldingRatios` 测试验证（总 ratio ≠ 1.0 的场景）

**影响**：`estimated_change_pct` 和 `estimated_nav` 现在反映的是**持仓股票对基金净值的实际影响**。若持仓总和不到 100%（如 55%），估值变化率会相应变小，更贴近真实 NAV 表现。
- 新增 `RetryScheduler` 独立线程池处理净值重试
- 新增 `SchedulerConfig` (4 线程 `ThreadPoolTaskScheduler`)
- `SnapshotService` 拆分三个 `@Scheduled` 方法（行情快/慢、净值）
- 行情刷新只更新持仓价格数据，净值刷新只更新净值字段，互不覆盖
- `collectCurrentQuotes()` 从当前快照提取上次成功行情用于重算估值
- 增量更新 `FundSummary` 列表和 `generated_at` 时间戳
- `application.properties` 新增可配刷新间隔参数
- `@EnableScheduling` 启用 Spring 调度
- 市场时间判断 `isMarketHours()`: 工作日 9-17 北京时间

### 2026-05-25 第3轮 — 前端 Vue 3 管理界面 + Admin API

**新增后端**：
- `FundFileService.java`：外部 `data/funds/*.json` 文件的 CRUD 服务，支持 JAR 外可写目录
- `FundAdminController.java`：`/api/admin/funds` REST API（GET 列表/单个、POST 新建、PUT 更新、DELETE 删除）
- `WebConfig.java`：CORS 支持 POST/PUT/DELETE + `/admin` → `/admin/index.html` 视图控制器
- `application.properties`：新增 `fund.data.dir=./data/funds` 配置

**新增前端** (`frontend/`)：
- Vue 3 + Vite 项目，`/api` 代理到 localhost:5000
- 构建输出到 `src/main/resources/static/admin/`
- 三个组件：
  - `FundList.vue`：基金列表展示，搜索过滤，新建/删除基金（含确认弹窗）
  - `FundEditor.vue`：可编辑基金信息（代码/名称/持仓表），持仓行增删，保存 PUT 到后端
  - `FundSearch.vue`：按基金代码查询持仓，表格展示，支持一键复制 JSON 和 Markdown
- `App.vue`：顶部导航切换三个功能标签页

**修复**：
- `FundControllerTest`：限定 `@WebMvcTest(FundController.class)` 避免扫描 Admin 控制器
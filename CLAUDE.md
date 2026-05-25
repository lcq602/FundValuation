# FundValuation 项目知识

## 项目概述
Spring Boot 4.0.6 + Java 17 基金实时估值应用。从 guzhi (Python Flask) 迁移而来。

## API 端点
| 端点 | 方法 | 说明 |
|------|------|------|
| `/api/funds` | GET | 基金摘要列表 |
| `/api/snapshot` | GET | 完整快照（含持仓详情） |
| `/api/estimate?fund_code=` | GET | 指定/全部基金估值详情 |

## 刷新策略
- **行情刷新**: 交易时段（工作日 9-17, 北京时间）每 15 秒，非交易时段每 5 分钟
- **净值刷新**: 交易时段每 30 分钟，失败后 30 秒重试最多 3 次
- **数据保留**: 刷新失败时保留上次成功数据，不会被 0 覆盖
- **调度方式**: Spring `@Scheduled` + `ThreadPoolTaskScheduler` (4 线程池)

## 外部 API
- **天天基金**: `https://fund.eastmoney.com/{code}.html` — 净值抓取
- **腾讯行情**: `https://qt.gtimg.cn/q={symbols}` — 实时行情
- **市场推断**: A 股 `sh`/`sz`(6/9开头→sh, 其余→sz), 港股 `hk`(5位数字), 美股 `us`(字母)

## 数据模型
- `funds/*.json`: 基金持仓配置，通过 `ResourcePatternResolver` 加载（JAR 友好）
- JSON 字段: snake_case, Jackson 全局 `SNAKE_CASE` 策略
- 状态: `pending`(初始骨架) → `ok`(正常) / `partial`(部分失败)

## 计算逻辑
- **weight_contribution** = `ratio × stockReturn`（原始持仓权重，不归一化）
- **estimated_change_pct** = `sum(ratio × stockReturn) × 100`
- **estimated_nav** = `baseNav × (1 + sum(ratio × stockReturn))`
- 持仓的 `ratio` 是实际基金权重（如 0.102 = 10.2%），非归一化值
- 若持仓占比总和不到 100%，估值变化会按比例缩减，反映现金/未跟踪资产的稀释效应

## 关键配置 (application.properties)
```properties
server.port=5000
spring.jackson.property-naming-strategy=SNAKE_CASE
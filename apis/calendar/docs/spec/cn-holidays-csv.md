# 中国节假日通用 CSV 规范

`apis/calendar/assets/source/CN/holidays.csv` 是仓库唯一需要审阅和长期维护的节假日数据表。
UTF-8 编码、LF 换行、首行固定表头；每行表示一个“官方放假”或“调休补班”覆盖。
普通日期和普通周末不写入文件，由各语言按公历星期推导。

```csv
date,status,holiday,statutory,sourceYear,confidence
2026-02-17,OFF,SPRING_FESTIVAL,1,2026,GOV_NOTICE
2026-02-28,WORK,SPRING_FESTIVAL,0,2026,GOV_NOTICE
```

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `date` | `YYYY-MM-DD` | 公历日期，文件内唯一且升序 |
| `status` | `OFF` / `WORK` | 官方放假或调休补班 |
| `holiday` | 稳定英文代码 | 所属节假日 |
| `statutory` | `0` / `1` | 是否为法定节假日本日 |
| `sourceYear` | 四位年份 | 安排所属年度 |
| `confidence` | 枚举 | 来源等级 |

置信等级：

- `GOV_NOTICE`：已与国务院年度通知逐日核对。
- `ARCHIVED_NOTICE`：依据存档通知和历史日历整理。
- `RECONSTRUCTED`：依据法规、历史日历与交叉来源重建。

`OFF` 不等于 `statutory=1`：连休中的调休日是官方休息日，但不是新增法定日。
`WORK` 即使落在周六或周日也必须视为工作日。

推荐的跨语言解释顺序：

1. 先以星期计算 `isWeekend`。
2. 默认工作日为周一至周五，普通周末为休息日。
3. `OFF` 覆盖为休息日和官方假期。
4. `WORK` 覆盖为工作日和调休补班日。
5. `statutory=1` 仅设置法定标记。
6. 农历、节气和节日由独立日期资产计算，不改变工作状态。

节假日代码当前包括：

`NEW_YEAR`、`SPRING_FESTIVAL`、`TOMB_SWEEPING`、`LABOUR_DAY`、
`DRAGON_BOAT`、`MID_AUTUMN`、`NATIONAL_DAY`、`VICTORY_DAY_70`。

新增代码必须保持大写 ASCII 和下划线，已有代码不得在同一数据格式版本内改变含义。

# 极简闪记 (MinFlash)

一款主打"秒级录入+强效防遗忘"的极简Android待办提醒工具。

## 核心特性
- ✅ 秒级文本/语音录入，自动解析时间和任务内容
- ✅ 到点强提醒，闹铃+震动
- ✅ **摇一摇关提醒**（核心亮点）
- ✅ 通知栏直接修改时间，不用打开APP
- ✅ 当日任务当日毕，凌晨0点自动清空，第二天打开就是全新无压力的列表
- ✅ 无分类、无标签、无历史包袱，极致极简

## 技术栈
- Kotlin + Jetpack Compose
- Room 本地数据库
- AlarmManager 精准闹钟
- SensorManager 摇一摇检测
- 系统语音识别API（无需第三方服务）

## 构建要求
- Android Studio Hedgehog (2023.1.1) 或更高版本
- JDK 17
- Android SDK 34 (CompileSdk 34)
- 最低支持 Android 8.0 (API 26)

## 构建方法
1. 克隆项目到本地
2. 用Android Studio打开项目根目录
3. 等待Gradle同步完成
4. 连接安卓手机（开启USB调试），点击运行即可

## 项目结构
```
app/src/main/java/com/shanji/minflash/
├── MinFlashApp.kt          # Application入口，初始化数据库和通知渠道
├── data/                   # 数据层
│   ├── Task.kt             # 任务实体
│   ├── TaskDao.kt          # 数据访问接口
│   └── TaskDatabase.kt     # Room数据库
├── reminder/               # 提醒相关
│   ├── AlarmScheduler.kt    # 闹钟调度
│   ├── AlarmReceiver.kt    # 闹钟触发接收
│   ├── BootCompletedReceiver.kt # 开机恢复闹钟
│   ├── MidnightCleanupReceiver.kt # 午夜自动清空任务
│   └── NotificationActionReceiver.kt # 通知栏操作
├── ui/                      # UI层
│   ├── MainActivity.kt      # 主界面
│   ├── ShakeAlertActivity.kt # 摇一摇提醒界面
│   └── theme/               # Compose主题
├── util/                    # 工具类
│   └── TimeParser.kt        # 自然语言时间解析
└── viewmodel/               # 业务逻辑
    └── TaskViewModel.kt
```

## 已完成功能
- [x] 基础项目结构搭建
- [x] Room数据库实现
- [x] 自然语言时间解析
- [x] 精准闹钟调度
- [x] 摇一摇关闭提醒
- [x] 午夜自动清空任务
- [x] 开机自动恢复闹钟
- [ ] 语音输入接入
- [ ] 通知栏修改时间功能
- [ ] 后台保活优化

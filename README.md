# 78HAM ARDPTT

公网手台 PTT 对讲客户端 — 专业工业风 Android 应用，面向公网对讲机/手持台设备。

基于 [NRLLink](https://github.com/hicaoc/nrllink) 服务端协议。

## 功能

- **PTT 对讲** — 按住说话，支持息屏启麦
- **Opus 语音** — 6kbps 低带宽编码，16kHz 采样
- **G711 语音** — 兼容传统 A-law 编码
- **文本消息** — 接收 Type=5 文本消息（只收不发）
- **频道切换** — 支持多房间在线切换
- **实体按键** — 自动识别设备，适配 D12/MTK 平台
- **开机自启** — 支持开机后台自动连接
- **工业风 UI** — 纯黑底、荧光绿状态、高信息密度

## 技术栈

- **语言**: Kotlin
- **UI**: Jetpack Compose + Material 3
- **音频**: AudioRecord/AudioTrack + MediaCodec Opus
- **协议**: NRL21 二进制 UDP 协议
- **最低 SDK**: Android 6.0 (API 23)
- **目标 SDK**: Android 13 (API 33)
- **构建**: Gradle + AGP 9.2.0

## 默认配置

| 参数 | 值 |
|------|-----|
| 服务器 | m.nrlptt.com:60050 |
| DMR ID | 178 |
| SSID | 178 |
| 语音编码 | Opus 6kbps |
| 心跳间隔 | 1秒 |
| 设备型号 | 101 (Android) |

## 项目结构

```
app/src/main/java/com/nrlptt/app/
├── NrlPttApp.kt              # Application
├── audio/
│   ├── G711Codec.kt          # G.711 A-law 编解码
│   ├── OpusCodec.kt          # Opus MediaCodec 编解码 (6kbps)
│   ├── AudioRecorder.kt      # 麦克风录制 (8kHz/16kHz)
│   ├── AudioPlayer.kt        # PCM 播放
│   └── AudioManager.kt       # 音频调度 (TX/RX/编解码)
├── data/
│   ├── Models.kt             # 数据模型 + 默认值
│   └── SettingsRepository.kt # SharedPreferences 存储
├── network/
│   ├── Nrl21Protocol.kt      # NRL21 二进制协议编解码
│   ├── ApiClient.kt          # HTTP API (登录/设备/群组)
│   ├── UdpClient.kt          # UDP 通信 (心跳/收发/重连)
│   └── ConnectionState.kt    # 连接状态枚举
├── ptt/
│   ├── PttController.kt      # PTT 按键控制 (WakeLock/振动)
│   └── DeviceKeyProfiles.kt  # 设备按键方案自动识别
├── service/
│   ├── PttService.kt         # 前台服务 (核心业务逻辑)
│   └── BootReceiver.kt       # 开机自启
├── theme/
│   ├── Colors.kt             # 深色工业风配色
│   ├── Typography.kt         # DIN 风格字体
│   └── Theme.kt              # Material 3 主题
└── ui/
    ├── MainActivity.kt       # Activity 入口
    ├── components/
    │   ├── RoomSwitcher.kt   # 频道切换器
    │   ├── StatusCard.kt     # TX/RX/STANDBY 状态卡片
    │   ├── SpeakerIndicator.kt # 当前发言人 + 音量条
    │   ├── ActivityList.kt   # 活动日志
    │   ├── PttButton.kt      # 按住说话按钮
    │   └── RoomPickerDialog.kt # 频道选择弹窗
    └── screen/
        ├── MainScreen.kt     # 主界面
        ├── LoginScreen.kt    # 登录界面
        └── SettingsScreen.kt # 设置界面
```

## 物理按键适配

应用启动时自动识别设备型号，匹配对应按键方案。

| 设备 | PTT 按键码 | 方式 |
|------|-----------|------|
| 和对讲 D12 | 113 (KEY_MUTE) | 物理 KeyCode |
| MTK 平台 | 0x106 | 系统广播 `PTT.down/up` |
| 通用设备 | 0x106 | 物理 KeyCode |
| 耳机线控 | HEADSETHOOK | 系统事件 |

支持的按键码：`0x106`, `113`, `368`, `270`, `531`, `532`, `HEADSETHOOK`, `BUTTON_1~12`

### 提交新设备适配

```bash
# 获取按键 KeyCode
adb shell getevent -lt | grep -i "EV_KEY"

# 获取广播按键
adb logcat -s PttController:* | grep -iE "ptt|key"

# 获取设备信息
adb shell getprop ro.product.model
adb shell getprop ro.hardware
```

## 编译运行

### 环境要求

- Android Studio Ladybug 或更高版本
- JDK 8+
- Android SDK 35

### 命令行编译

```bash
./gradlew assembleDebug
```

APK 输出：`app/build/outputs/apk/debug/`

## NRL21 协议

本项目使用 NRL21 二进制协议与 NRLLink 服务端通信。

| Type | 名称 | 说明 |
|------|------|------|
| 1 | G.711 语音 | 8kHz, 20ms帧, 160字节 |
| 2 | 心跳包 | 1秒/次 |
| 5 | 文本消息 | 只接收, 支持 [text]/[loc]/[json] 子类型 |
| 7 | 设备操作 | 切换群组 |
| 8 | Opus 语音 | 16kHz, 6kbps, 20ms帧 |

详细协议文档：[NRLLink_NRL_完整接口文档.md](../NRLLink_NRL_完整接口文档.md)

## 权限

| 权限 | 用途 |
|------|------|
| `RECORD_AUDIO` | 录制语音 |
| `INTERNET` | 网络通信 |
| `FOREGROUND_SERVICE` | 后台对讲服务 |
| `WAKE_LOCK` | 息屏保持连接 |
| `RECEIVE_BOOT_COMPLETED` | 开机自启 |
| `POST_NOTIFICATIONS` | 通知提醒 |

## License

This project is private. All rights reserved.

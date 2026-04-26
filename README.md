# gptimage2

> 一个接入 [APIYI](https://api.apiyi.com) GPT Image 2 系列模型的 Android App。

OpenAI 在 2026/04 正式发布 ChatGPT Images 2.0（`gpt-image-2`）。APIYI 同步上线两条接入：

| 模型 ID | 计费 | 适用场景 |
|---------|------|----------|
| `gpt-image-2-all` | $0.03 / 次，包并发 | **本 App 默认**。反代版，多模态对话直出图 |
| `gpt-image-2` | 按 token（输入 $8/M、输出 $30/M），单图 ~$0.04–$0.35 | OpenAI 官方代理，原生 2K + 4K 上采样、文字渲染、`input_fidelity` 主体锁定 |

可在「设置 → 模型」里随时切换。三个端点全部可用：

| 功能 | 端点 | 说明 |
|------|------|------|
| 💬 多模态对话 | `POST /v1/chat/completions` | OpenAI 视觉消息格式，回复可含文字和图片（默认入口）|
| 📝 文生图 | `POST /v1/images/generations` | JSON，参数：`model / prompt / size / quality / n` |
| 🖼 图像编辑 | `POST /v1/images/edits` | multipart，1–4 张参考图（prompt 内以 `image 1` 引用），支持 `input_fidelity` |

附带：本地图库（一键保存到相册）、API Key + 端口 + 模型本地存储、4 个加速节点（vip / cf / api / b）随时切换。

---

## 快速开始

### 直接安装

1. 进入仓库 **Actions** → 选最新一次 `Build gptimage2 APK` 运行。
2. 在 Artifacts 区下载 `gptimage2-Debug-v*.apk` 并安装（开启「允许未知来源」）。
3. 打开 App → **设置** → 填 API Key（默认 Base URL 为 `https://api.apiyi.com`）→ 保存。

### 本地开发

```bash
git clone https://github.com/owjk123/gptimage2.git
cd gptimage2
# 用 Android Studio Hedgehog+ 打开；JDK 17；Android SDK 35
```

---

## 项目结构

```
app/src/main/java/com/gptimage2/
├── GptImage2App.kt              # Application（Room 初始化）
├── MainActivity.kt              # Compose 入口
├── data/
│   ├── local/Database.kt        # Room DB + GalleryDao
│   └── model/Models.kt          # 枚举、ChatMessage、GalleryImage
├── repository/
│   ├── ImageGenRepository.kt    # /v1/images/generations
│   ├── ImageEditRepository.kt   # /v1/images/edits（multipart，多图）
│   ├── ChatRepository.kt        # /v1/chat/completions（多模态）
│   └── GalleryRepository.kt     # 内部存储 + MediaStore 导出
├── util/
│   ├── ApiKeyManager.kt         # SharedPreferences
│   └── ImageCodec.kt            # Uri → bytes / base64
├── viewmodel/MainViewModel.kt   # 单一 ViewModel，按 Tab 分三组 state
└── ui/
    ├── theme/Theme.kt           # 黑金 Material3 主题
    ├── components/Components.kt # GoldButton、ChipSelector 等
    └── screens/
        ├── MainScreen.kt        # 底部 Tab 容器
        ├── TextToImageScreen.kt
        ├── ImageEditScreen.kt
        ├── ChatScreen.kt
        ├── GalleryScreen.kt
        └── SettingsScreen.kt
```

## API 约定

- 认证：`Authorization: Bearer <API_KEY>`
- Base URL 默认：`https://api.apiyi.com`（也支持 `vip.apiyi.com` / `api-cf.apiyi.com` / `b.apiyi.com`）
- Model：默认 `gpt-image-2-all`，也可切到官方 `gpt-image-2`
- `size`：`gpt-image-2` 接受任意分辨率，长边 ≤ 3840px，两边都是 16 的倍数（亦可传 `auto`）
- `quality`：`low / medium / high / auto`
- `input_fidelity`（仅 `/v1/images/edits`）：用于锁定原图主体进行精修
- 响应格式优先请求 `b64_json`，本地解码成 PNG 保存到 `filesDir/gallery/`
- `/v1/images/edits` 使用 multipart，重复追加字段名 `image` 实现多参考图；不要在 OpenAI SDK 的默认 size/n 下发起请求（参考 APIYI 文档说明），本 App 直接用 OkHttp，无此问题

## 注意事项

- API Key 保存在 SharedPreferences，仅存本机、不会上传。
- 生成的图片默认只写入内部图库，点击图库卡片才能「保存到相册」。
- Release APK 使用 debug signingConfig 以便 CI 免签名出包；如需发布到应用市场请替换正式签名。

## 许可

个人学习用途，API 额度按 APIYI 计费。

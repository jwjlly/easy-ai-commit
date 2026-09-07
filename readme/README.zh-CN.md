<p align="center">
  <img src="../branding/easy-ai-commit-logo.png" alt="Easy AI Commit 标志" width="112">
</p>

<h1 align="center">Easy AI Commit</h1>

<p align="center">
  简单好用的一键 AI 提交信息生成，支持 OpenAI Compatible 与 Anthropic Compatible 模型接入。
</p>

<p align="center">
  <a href="../README.md">English</a> ·
  <strong>简体中文</strong> ·
  <a href="README.ja.md">日本語</a> ·
  <a href="README.ko.md">한국어</a>
</p>

<p align="center">
  <a href="https://plugins.jetbrains.com/plugin/34058"><img src="https://img.shields.io/jetbrains/plugin/v/34058?label=Marketplace&logo=jetbrains" alt="JetBrains Marketplace"></a>
  <a href="https://plugins.jetbrains.com/plugin/34058"><img src="https://img.shields.io/jetbrains/plugin/d/34058" alt="下载量"></a>
  <a href="https://plugins.jetbrains.com/plugin/34058"><img src="https://img.shields.io/jetbrains/plugin/r/rating/34058" alt="评分"></a>
</p>

## 为什么做 Easy AI Commit？

现有的 AI 提交信息插件往往需要做出取舍：有的只支持 OpenAI 风格的接口，有的配置复杂，还有的不支持自定义提示词。Easy AI Commit 的初衷就是消除这些妥协，以简单的一键操作为核心，同时将 `OpenAI Compatible` 和 `Anthropic Compatible` 都作为一等支持的接入方式。

通过双协议支持，Easy AI Commit 可以接入大多数提供兼容接口的模型厂商、聚合服务和中转站，避免将用户绑定在单一供应商上。

## 功能特性

- 在原生 Commit 工具窗口中一键生成提交信息。
- 有已包含文件时使用这些文件，否则自动使用当前显示的全部待提交文件。
- 将生成结果直接写入现有的 Commit Message 编辑器。
- 支持自定义模型配置名称，便于区分多个兼容接口。
- 支持 `OpenAI Compatible` 和 `Anthropic Compatible` 两种协议。
- 提供一个可选择协议的 `Custom` 预设，以及 24 个具名服务商预设。
- Provider Preset 与 Protocol 自动联动，避免保存非法组合。
- 支持自定义 Base URL、模型 ID、提示词模板、输出语言和最大变更内容大小。
- API Key 存储在 JetBrains Password Safe 中，而不是插件设置 XML 中。
- 配置入口位于 `设置 → 其他设置 → Easy AI Commit`。

## 兼容性

- IntelliJ IDEA 2024.2 或更高版本（`since-build` 242）
- 提供非模态 Commit 工具窗口及所需 VCS API 的 JetBrains IDE

构建项目需要 Java 21。最终用户运行插件时使用 IDE 自带的 JetBrains Runtime，无需单独安装 Java 21。

## 安装

可从 [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/34058) 安装最新版本。

如需安装开发版本，也可以从源码构建：

```bash
./gradlew clean buildPlugin
```

可安装的 ZIP 位于 `build/distributions/`。在 IDE 中打开 `设置 → 插件`，选择**从磁盘安装插件**，然后选择该 ZIP 文件。

## 配置与使用

1. 打开 `设置 → 其他设置 → Easy AI Commit`。
2. 新建或选择一个具名模型配置。
3. 选择服务商预设及其支持的协议。
4. 填写 Base URL、模型 ID 和 API Key，然后测试连接。
5. 根据需要调整输出语言和提示词模板。
6. 打开 Commit 工具窗口，点击 Commit Message 编辑器上方的 Easy AI Commit Logo。

生成会立即开始。成功后，当前 Commit Message 内容会被生成结果替换。

## 代理与请求超时

每个模型配置都有独立的**网络代理**和**请求超时（秒）**设置，位于**测试连接**上方。

- **跟随 IDE 代理设置**：沿用 IDE 的代理、PAC、认证及绕过规则，可通过**打开 IDE 代理设置**进行配置。
- **不使用代理**（默认）：此模型直接连接，即使 IDE 配置了代理也不使用。
- **自定义代理**：填写域名/IP 和端口（`1–65535`），支持 HTTP（含 HTTPS CONNECT）、可选的 HTTP Basic 认证，以及无认证 SOCKS5。SOCKS5 将目标域名交给代理解析，暂不支持带认证 SOCKS5 和企业认证方式。
- **请求超时**：范围 `5–300` 秒，默认 `90` 秒；希望快速失败时可设为 `15` 或 `30` 秒。总时间包含 DNS、代理/TLS 连接、发送及读取完整响应，不含本地变更收集和凭据读取。新配置及未记录代理模式的旧配置默认不使用代理，已明确存储的代理选择保持不变；超时默认 90 秒。新的总时限比原来的 90 秒读取超时更严格。

自定义代理失败时不会回退直连。超时后取消请求、恢复生成按钮，并保留现有提交信息；迟到响应会被丢弃，插件不会自动重试请求。

**测试连接**使用当前表单值，显示所选连接方式和耗时。未保存配置测试成功不代表已保存：请点击**应用**或**确定**，重新打开设置，不重新输入凭据再测试，以检查已存储值。保存凭据时会回读校验；存储失败会保留输入，便于重试。

API Key 和代理密码分别存入 JetBrains Password Safe，沿用所选 Keychain/KeePass 后端。代理密码绑定模型配置及代理端点/账号；修改主机、端口、类型或用户名后，需要使用对应身份的密码，重命名模型配置不会丢失凭据。如果原生 Keychain 读写异常，请检查 IDE 的**密码**设置，也可使用 KeePass 后端。

## 支持的协议与服务商

Easy AI Commit 专注于兼容协议的 HTTP API：

- `OpenAI Compatible`
- `Anthropic Compatible`

`Custom` 预设同时支持两种协议，通过 Protocol 字段进行区分。具名服务商预设会为常见大模型供应商和聚合平台提供支持的协议、默认端点、模型及认证方式。

当前预设：

| 协议支持 | 预设 |
|---|---|
| `OpenAI Compatible` + `Anthropic Compatible` | `Custom`、DeepSeek、OpenRouter、GLM、Alibaba Bailian、Kimi、MiniMax、SiliconFlow、StepFun、Tencent TokenHub |
| `OpenAI Compatible` | OpenAI、Gemini、Grok、Volcano Ark、Baidu Qianfan、iFlytek Spark、Mistral AI、Groq、Together AI、Fireworks AI、Cerebras、Cohere、Perplexity、Hugging Face |
| `Anthropic Compatible` | Anthropic |

需要专有 IAM、OAuth、SigV4、部署资源或其他云平台专属配置的企业云平台，目前不在项目支持范围内。

## 国际化

插件 UI 会跟随当前 IDE 语言。英文为默认回退语言，并内置以下翻译：

| 语言 | Locale |
|---|---|
| English | 默认 |
| 简体中文 | `zh-CN` |
| 日本語 | `ja` |
| 한국어 | `ko` |

输出语言可以独立于界面语言设置。选择“自动”时，生成提交信息会跟随 IDE 语言；也可以从 18 种语言预设中手动选择。

如果预设列表中没有您需要的语言，可以在自定义提示词中直接指定目标语言。默认提示词通过 `{language}` 变量插入语言要求；若希望固定使用某种语言，请将包含 `{language}` 的指令改写为明确的语言要求，不要同时保留两种约束。

## 安全与隐私

- API Key 通过 JetBrains Password Safe 存储。
- 只有用户点击生成按钮后，插件才会发送变更内容。
- 请求仅发送到当前模型配置指定的端点。

请同时查阅所配置模型服务商或网关的隐私及数据处理条款。

## 开发

项目使用 Kotlin/JVM、IntelliJ Platform Gradle Plugin 2.x、Gradle 9.3.1，并以 IntelliJ IDEA 2024.2.6 为开发基线。

```bash
./gradlew test
./gradlew buildPlugin
./gradlew verifyPluginStructure
./gradlew runIde
```

也可以使用已安装的 IDE 进行本地验证，而不修改仓库中提交的基线版本：

```bash
./gradlew test buildPlugin verifyPlugin \
  -PlocalIdePath="/path/to/IntelliJ IDEA.app"
```

项目结构：

```text
src/main/kotlin/       插件实现
src/main/resources/    插件描述、图标及国际化资源
src/test/kotlin/       单元测试和集成测试
branding/              Logo 源文件及素材说明
readme/                多语言 README 文件
tools/                 开发和素材生成工具
```

## 参与贡献

欢迎提交 Issue 和 Pull Request。提交变更前请运行 `./gradlew test`。新增或修改可见文本时，请同步维护英文、简体中文、日文和韩文资源包。

## 开源协议

Easy AI Commit 使用 [MIT License](../LICENSE) 开源。

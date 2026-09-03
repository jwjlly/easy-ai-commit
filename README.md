<p align="center">
  <img src="branding/easy-ai-commit-logo.png" alt="Easy AI Commit logo" width="112">
</p>

<h1 align="center">Easy AI Commit</h1>

<p align="center">
  Simple, one-click AI-assisted commit messages with OpenAI Compatible and Anthropic Compatible model access.
</p>

<p align="center">
  <strong>English</strong> ·
  <a href="readme/README.zh-CN.md">简体中文</a> ·
  <a href="readme/README.ja.md">日本語</a> ·
  <a href="readme/README.ko.md">한국어</a>
</p>

<p align="center">
  <a href="https://plugins.jetbrains.com/plugin/34058"><img src="https://img.shields.io/jetbrains/plugin/v/34058?label=Marketplace&logo=jetbrains" alt="JetBrains Marketplace"></a>
  <a href="https://plugins.jetbrains.com/plugin/34058"><img src="https://img.shields.io/jetbrains/plugin/d/34058" alt="Downloads"></a>
  <a href="https://plugins.jetbrains.com/plugin/34058"><img src="https://img.shields.io/jetbrains/plugin/r/rating/34058" alt="Rating"></a>
</p>

## Why Easy AI Commit?

Existing AI commit-message plugins often force a trade-off: some support only OpenAI-style APIs, some require complex setup, and others do not allow custom prompts. Easy AI Commit was created to remove those compromises. It focuses on a simple one-click workflow while treating both `OpenAI Compatible` and `Anthropic Compatible` connections as first-class options.

This dual-protocol approach makes Easy AI Commit usable with a broad range of model providers, aggregation services, and compatible gateways or proxies—without locking users into a single vendor.

## Features

- Generates a commit message with one click from the native Commit tool window.
- Uses included files when present; otherwise falls back to all displayed pending files.
- Writes the generated message directly into the existing Commit Message editor.
- Supports named model profiles, so multiple compatible endpoints remain easy to distinguish.
- Supports both `OpenAI Compatible` and `Anthropic Compatible` protocols.
- Provides one `Custom` preset with protocol selection plus 24 named provider presets.
- Keeps provider presets and protocol choices synchronized to prevent invalid combinations.
- Supports custom Base URLs, model IDs, prompt templates, output languages, and change-size limits.
- Stores API keys in JetBrains Password Safe instead of the plugin settings XML.
- Places configuration under `Settings → Other Settings → Easy AI Commit`.

## Compatibility

- IntelliJ IDEA 2024.2 or later (`since-build` 242)
- JetBrains IDEs that provide the non-modal Commit tool window and required VCS APIs

Java 21 is required to build the project. End users run the plugin with the JetBrains Runtime bundled with their IDE.

## Installation

Install the latest release from [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/34058).

To install a development build instead, build the plugin from source:

```bash
./gradlew clean buildPlugin
```

The installable ZIP is created in `build/distributions/`. In the IDE, open `Settings → Plugins`, choose **Install Plugin from Disk**, and select that ZIP file.

## Configuration and usage

1. Open `Settings → Other Settings → Easy AI Commit`.
2. Create or select a named model profile.
3. Choose a provider preset and one of its supported protocols.
4. Enter the Base URL, model ID, and API key, then test the connection.
5. Optionally customize the output language and prompt template.
6. Open the Commit tool window and click the Easy AI Commit logo above the Commit Message editor.

Generation starts immediately. When it succeeds, the current Commit Message text is replaced with the generated result.

## Supported protocols and providers

Easy AI Commit intentionally focuses on compatible HTTP APIs:

- `OpenAI Compatible`
- `Anthropic Compatible`

The `Custom` preset supports both protocols and distinguishes them through the Protocol field. Named presets provide supported protocols, default endpoints, models, and authentication behavior for common providers and aggregation platforms.

Current presets:

| Protocol support | Presets |
|---|---|
| `OpenAI Compatible` + `Anthropic Compatible` | `Custom`, DeepSeek, OpenRouter, GLM, Alibaba Bailian, Kimi, MiniMax, SiliconFlow, StepFun, Tencent TokenHub |
| `OpenAI Compatible` | OpenAI, Gemini, Grok, Volcano Ark, Baidu Qianfan, iFlytek Spark, Mistral AI, Groq, Together AI, Fireworks AI, Cerebras, Cohere, Perplexity, Hugging Face |
| `Anthropic Compatible` | Anthropic |

Enterprise cloud platforms that require proprietary IAM, OAuth, SigV4, deployment-resource, or other cloud-specific configuration are currently outside the project scope.

## Localization

The plugin UI follows the active IDE locale. English is the fallback language, with bundled translations for:

| Language | Locale |
|---|---|
| English | default |
| 简体中文 | `zh-CN` |
| 日本語 | `ja` |
| 한국어 | `ko` |

Output language can be configured independently from the UI language. Auto follows the IDE locale when generating a commit message, or you can choose from 18 language presets.

If your preferred language is not available in the preset list, specify it directly in the custom prompt. The default template inserts the selected language through the `{language}` variable; to enforce a fixed language, replace the instruction containing `{language}` with an explicit language requirement instead of keeping both.

## Security and privacy

- API keys are stored through JetBrains Password Safe.
- Change content is sent only after the user clicks the generation button.
- Requests are sent to the endpoint configured in the selected model profile.

Review the privacy and data-handling terms of the model provider or gateway you configure.

## Development

The project uses Kotlin/JVM, IntelliJ Platform Gradle Plugin 2.x, Gradle 9.3.1, and an IntelliJ IDEA 2024.2.6 development baseline.

```bash
./gradlew test
./gradlew buildPlugin
./gradlew verifyPluginStructure
./gradlew runIde
```

An installed IDE can be used for local verification without changing the committed baseline:

```bash
./gradlew test buildPlugin verifyPlugin \
  -PlocalIdePath="/path/to/IntelliJ IDEA.app"
```

Project layout:

```text
src/main/kotlin/       Plugin implementation
src/main/resources/    Plugin descriptor, icons, and localization bundles
src/test/kotlin/       Unit and integration tests
branding/              Source logo and asset documentation
readme/                Localized README files
tools/                 Asset-generation utilities
```

## Contributing

Issues and pull requests are welcome. Please run `./gradlew test` before submitting a change. When adding or changing a visible string, keep the English, Simplified Chinese, Japanese, and Korean resource bundles in sync.

## License

Easy AI Commit is released under the [MIT License](LICENSE).

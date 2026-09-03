<p align="center">
  <img src="../branding/easy-ai-commit-logo.png" alt="Easy AI Commit 로고" width="112">
</p>

<h1 align="center">Easy AI Commit</h1>

<p align="center">
  OpenAI Compatible과 Anthropic Compatible 연결을 지원하는 간단하고 편리한 원클릭 AI 커밋 메시지 생성.
</p>

<p align="center">
  <a href="../README.md">English</a> ·
  <a href="README.zh-CN.md">简体中文</a> ·
  <a href="README.ja.md">日本語</a> ·
  <strong>한국어</strong>
</p>

<p align="center">
  <a href="https://plugins.jetbrains.com/plugin/34034"><img src="https://img.shields.io/jetbrains/plugin/v/34034?label=Marketplace&logo=jetbrains" alt="JetBrains Marketplace"></a>
  <a href="https://plugins.jetbrains.com/plugin/34034"><img src="https://img.shields.io/jetbrains/plugin/d/34034" alt="다운로드 수"></a>
  <a href="https://plugins.jetbrains.com/plugin/34034"><img src="https://img.shields.io/jetbrains/plugin/r/rating/34034" alt="평점"></a>
</p>

## Easy AI Commit을 만든 이유

기존 AI 커밋 메시지 플러그인은 종종 선택을 강요합니다. OpenAI 방식 API만 지원하거나, 설정이 복잡하거나, 사용자 지정 프롬프트를 지원하지 않는 경우가 있습니다. Easy AI Commit은 이러한 제약을 없애기 위해 간단한 원클릭 흐름을 중심으로 설계했으며 `OpenAI Compatible`과 `Anthropic Compatible` 연결을 모두 동등하게 지원합니다.

두 프로토콜을 지원하므로 특정 공급자에 종속되지 않고 호환 API를 제공하는 다양한 모델 공급자, 통합 서비스, 게이트웨이 및 프록시를 연결할 수 있습니다.

## 주요 기능

- 기본 Commit 도구 창에서 클릭 한 번으로 커밋 메시지를 생성합니다.
- 포함된 파일이 있으면 해당 파일을 사용하고, 없으면 현재 표시된 모든 보류 파일을 사용합니다.
- 생성된 결과를 기존 Commit Message 편집기에 바로 입력합니다.
- 여러 호환 엔드포인트를 쉽게 구분할 수 있도록 이름을 지정하는 모델 프로필을 지원합니다.
- `OpenAI Compatible` 및 `Anthropic Compatible` 프로토콜을 모두 지원합니다.
- 프로토콜을 선택할 수 있는 단일 `Custom` 프리셋과 24개의 명명된 공급자 프리셋을 제공합니다.
- Provider Preset과 Protocol을 연동하여 잘못된 조합을 방지합니다.
- Base URL, 모델 ID, 프롬프트 템플릿, 출력 언어 및 최대 변경 내용 크기를 사용자 지정할 수 있습니다.
- API 키를 플러그인 설정 XML이 아닌 JetBrains Password Safe에 저장합니다.
- 설정 화면은 `설정 → 기타 설정 → Easy AI Commit`에 있습니다.

## 호환성

- IntelliJ IDEA 2024.2 이상(`since-build` 242)
- 비모달 Commit 도구 창과 필요한 VCS API를 제공하는 JetBrains IDE

프로젝트 빌드에는 Java 21이 필요합니다. 최종 사용자는 IDE에 포함된 JetBrains Runtime으로 플러그인을 실행하므로 Java 21을 별도로 설치할 필요가 없습니다.

## 설치

최신 버전은 [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/34034)에서 설치할 수 있습니다.

개발 버전을 설치하려면 소스에서 플러그인을 빌드하세요.

```bash
./gradlew clean buildPlugin
```

설치 가능한 ZIP은 `build/distributions/`에 생성됩니다. IDE에서 `설정 → 플러그인`을 열고 **디스크에서 플러그인 설치**를 선택한 다음 해당 ZIP 파일을 지정하세요.

## 설정 및 사용

1. `설정 → 기타 설정 → Easy AI Commit`을 엽니다.
2. 이름이 지정된 모델 프로필을 만들거나 선택합니다.
3. Provider Preset과 해당 프리셋이 지원하는 Protocol을 선택합니다.
4. Base URL, 모델 ID 및 API 키를 입력한 다음 연결을 테스트합니다.
5. 필요에 따라 출력 언어와 프롬프트 템플릿을 조정합니다.
6. Commit 도구 창을 열고 Commit Message 편집기 위의 Easy AI Commit 로고를 클릭합니다.

생성은 즉시 시작됩니다. 성공하면 현재 Commit Message가 생성된 결과로 대체됩니다.

## 지원 프로토콜 및 공급자

Easy AI Commit은 호환 HTTP API에 집중합니다.

- `OpenAI Compatible`
- `Anthropic Compatible`

`Custom` 프리셋은 두 프로토콜을 모두 지원하며 Protocol 필드로 구분합니다. 명명된 프리셋은 일반적인 모델 공급자와 통합 플랫폼에 대해 지원 프로토콜, 기본 엔드포인트, 모델 및 인증 방식을 제공합니다.

현재 프리셋:

| 지원 프로토콜 | 프리셋 |
|---|---|
| `OpenAI Compatible` + `Anthropic Compatible` | `Custom`, DeepSeek, OpenRouter, GLM, Alibaba Bailian, Kimi, MiniMax, SiliconFlow, StepFun, Tencent TokenHub |
| `OpenAI Compatible` | OpenAI, Gemini, Grok, Volcano Ark, Baidu Qianfan, iFlytek Spark, Mistral AI, Groq, Together AI, Fireworks AI, Cerebras, Cohere, Perplexity, Hugging Face |
| `Anthropic Compatible` | Anthropic |

독자적인 IAM, OAuth, SigV4, 배포 리소스 또는 기타 클라우드 전용 설정이 필요한 엔터프라이즈 클라우드 플랫폼은 현재 지원 범위에 포함되지 않습니다.

## 현지화

플러그인 UI는 현재 IDE 로캘을 따릅니다. 영어가 기본 대체 언어이며 다음 번역이 포함되어 있습니다.

| 언어 | 로캘 |
|---|---|
| English | 기본값 |
| 简体中文 | `zh-CN` |
| 日本語 | `ja` |
| 한국어 | `ko` |

출력 언어는 UI 언어와 별도로 설정할 수 있습니다. 자동을 선택하면 커밋 메시지를 생성할 때 IDE 언어를 따르며, 필요하면 18개 언어 프리셋 중 하나를 직접 선택할 수 있습니다.

필요한 언어가 프리셋 목록에 없다면 사용자 지정 프롬프트에 대상 언어를 직접 지정할 수 있습니다. 기본 템플릿은 `{language}` 변수를 통해 언어 조건을 삽입합니다. 특정 언어를 항상 사용하려면 두 조건을 함께 유지하지 말고 `{language}`가 포함된 지시문을 명확한 언어 요구 사항으로 바꾸세요.

## 보안 및 개인정보 보호

- API 키는 JetBrains Password Safe를 통해 저장됩니다.
- 변경 내용은 사용자가 생성 버튼을 클릭한 경우에만 전송됩니다.
- 요청은 선택한 모델 프로필에 설정된 엔드포인트로 전송됩니다.

설정한 모델 공급자 또는 게이트웨이의 개인정보 보호 및 데이터 처리 약관도 확인하세요.

## 개발

이 프로젝트는 Kotlin/JVM, IntelliJ Platform Gradle Plugin 2.x, Gradle 9.3.1을 사용하며 IntelliJ IDEA 2024.2.6을 개발 기준으로 합니다.

```bash
./gradlew test
./gradlew buildPlugin
./gradlew verifyPluginStructure
./gradlew runIde
```

저장소의 기준 버전을 변경하지 않고 설치된 IDE를 로컬 검증에 사용할 수도 있습니다.

```bash
./gradlew test buildPlugin verifyPlugin \
  -PlocalIdePath="/path/to/IntelliJ IDEA.app"
```

프로젝트 구조:

```text
src/main/kotlin/       플러그인 구현
src/main/resources/    플러그인 설명자, 아이콘 및 현지화 리소스
src/test/kotlin/       단위 및 통합 테스트
branding/              원본 로고와 에셋 문서
readme/                다국어 README 파일
tools/                 개발 및 에셋 생성 유틸리티
```

## 기여

Issue와 Pull Request를 환영합니다. 변경 사항을 제출하기 전에 `./gradlew test`를 실행하세요. 화면에 표시되는 문자열을 추가하거나 변경할 때는 영어, 중국어 간체, 일본어 및 한국어 리소스 번들을 함께 업데이트해야 합니다.

## 라이선스

Easy AI Commit은 [MIT License](../LICENSE)에 따라 배포됩니다.

<p align="center">
  <img src="../branding/easy-ai-commit-logo.png" alt="Easy AI Commit ロゴ" width="112">
</p>


タイムアウト入力欄の横にある疑問符アイコンをクリックすると説明が表示されます。接続テストの成功は緑色、失敗は赤色のアイコンとテキストで表示されます。
<h1 align="center">Easy AI Commit</h1>

<p align="center">
  OpenAI Compatible と Anthropic Compatible に対応した、シンプルで使いやすいワンクリック AI コミットメッセージ生成。
</p>

<p align="center">
  <a href="../README.md">English</a> ·
  <a href="README.zh-CN.md">简体中文</a> ·
  <strong>日本語</strong> ·
  <a href="README.ko.md">한국어</a>
</p>

<p align="center">
  <a href="https://plugins.jetbrains.com/plugin/34058"><img src="https://img.shields.io/jetbrains/plugin/v/34058?label=Marketplace&logo=jetbrains" alt="JetBrains Marketplace"></a>
  <a href="https://plugins.jetbrains.com/plugin/34058"><img src="https://img.shields.io/jetbrains/plugin/d/34058" alt="ダウンロード数"></a>
  <a href="https://plugins.jetbrains.com/plugin/34058"><img src="https://img.shields.io/jetbrains/plugin/r/rating/34058" alt="評価"></a>
</p>

## Easy AI Commit を作った理由

既存の AI コミットメッセージプラグインでは、しばしば何かを諦める必要があります。OpenAI 形式の API にしか対応していないもの、設定が複雑なもの、カスタムプロンプトを使えないものがあります。Easy AI Commit はこうした妥協をなくすために、シンプルなワンクリック操作を中心に設計し、`OpenAI Compatible` と `Anthropic Compatible` の両方を第一級の接続方式としてサポートします。

2 つのプロトコルに対応することで、単一ベンダーに縛られることなく、互換 API を提供する幅広いモデルプロバイダー、集約サービス、ゲートウェイ、プロキシを利用できます。

## 主な機能

- ネイティブの Commit ツールウィンドウからワンクリックでコミットメッセージを生成します。
- 含まれているファイルがある場合はそれらを使用し、ない場合は表示中のすべての保留ファイルを使用します。
- 生成結果を既存の Commit Message エディターへ直接書き込みます。
- 複数の互換エンドポイントを区別できる、名前付きモデルプロファイルに対応します。
- `OpenAI Compatible` と `Anthropic Compatible` の両プロトコルに対応します。
- プロトコルを選択できる 1 つの `Custom` プリセットと、24 個の名前付きプロバイダープリセットを提供します。
- Provider Preset と Protocol を連動させ、不正な組み合わせを防ぎます。
- Base URL、モデル ID、プロンプトテンプレート、出力言語、変更内容の最大サイズをカスタマイズできます。
- API キーはプラグイン設定 XML ではなく JetBrains Password Safe に保存されます。
- 設定画面は `設定 → その他の設定 → Easy AI Commit` にあります。

## 互換性

- IntelliJ IDEA 2024.2 以降（`since-build` 242）
- 非モーダルの Commit ツールウィンドウと必要な VCS API を備えた JetBrains IDE

プロジェクトのビルドには Java 21 が必要です。利用者は IDE に同梱されている JetBrains Runtime でプラグインを実行するため、Java 21 を別途インストールする必要はありません。

## インストール

最新版は [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/34058) からインストールできます。

開発版をインストールする場合は、ソースからプラグインをビルドしてください。

```bash
./gradlew clean buildPlugin
```

インストール可能な ZIP は `build/distributions/` に生成されます。IDE で `設定 → プラグイン` を開き、**ディスクからプラグインをインストール**を選択して、その ZIP ファイルを指定してください。

## 設定と使い方

1. `設定 → その他の設定 → Easy AI Commit` を開きます。
2. 名前付きモデルプロファイルを作成または選択します。
3. Provider Preset と、それが対応する Protocol を選択します。
4. Base URL、モデル ID、API キーを入力し、接続をテストします。
5. 必要に応じて出力言語とプロンプトテンプレートを調整します。
6. Commit ツールウィンドウを開き、Commit Message エディター上部の Easy AI Commit ロゴをクリックします。

生成は直ちに始まります。成功すると、現在の Commit Message が生成結果に置き換わります。

Responses API を使うには、`OpenAI Compatible` を選択し、**Base URL** に `https://api.openai.com/v1/responses` などの完全なエンドポイント（またはゲートウェイの `/v1/responses` URL）を入力します。`/responses` で終わる URL では、接続テストを含めてこの API を自動的に使用します。`https://api.openai.com/v1` のようなベース URL や完全な `/chat/completions` URL は引き続き Chat Completions を使用します。接続先は選択した API に対応している必要があり、自動切り替えは行いません。応答が未完了または生成が拒否された場合、既存のコミットメッセージは保持されます。

## プロキシとリクエストのタイムアウト

各モデルプロファイルには、**接続をテスト**の上に独立した**ネットワークプロキシ**と**リクエストのタイムアウト（秒）**があります。

- **IDE のプロキシ設定を使用**：IDE のプロキシ、PAC、認証、除外ルールに従います。**IDE のプロキシ設定を開く**から変更できます。
- **プロキシを使用しない**（既定）：IDE にプロキシが設定されていても、このモデルは直接接続します。
- **カスタムプロキシ**：ホスト名/IP とポート（`1–65535`）を指定します。HTTP（HTTPS CONNECT を含む）、任意の HTTP Basic 認証、認証なし SOCKS5 に対応します。SOCKS5 は宛先ホスト名をプロキシ側で解決します。認証付き SOCKS5 や企業向け認証方式には対応していません。
- **リクエストのタイムアウト**：`5–300` 秒、既定値は `90` 秒です。早く失敗を検知したい場合は `15` または `30` 秒に設定してください。DNS、プロキシ/TLS 接続、送信、応答全体の読み取りを含み、ローカル差分の収集や認証情報の取得は含みません。新規プロファイルとプロキシモードが保存されていないプロファイルは、既定でプロキシを使用しません。明示的に保存されたプロキシ設定は維持されます。タイムアウトの既定値は 90 秒です。新しい全体の制限時間は、以前の 90 秒の読み取りタイムアウトより厳格です。

カスタムプロキシで失敗しても直接接続には切り替わりません。タイムアウト時はリクエストをキャンセルし、生成ボタンを復帰させ、既存のコミットメッセージを保持します。遅れて届いた応答は破棄され、プラグインによる自動再試行は行いません。

**接続をテスト**は現在の入力値を使用し、選択した接続方法と経過時間を表示します。未保存の設定でテストが成功しても保存はされません。**適用**または **OK** を押し、設定を開き直して認証情報を再入力せずにテストし、保存済みの値を確認してください。保存時には読み戻し検証を行い、失敗した場合は入力内容を保持します。

API キーとプロキシパスワードは JetBrains Password Safe に別々に保存され、設定済みの Keychain/KeePass を使用します。プロキシパスワードはモデルプロファイルとプロキシの接続先・アカウントに紐づきます。ホスト、ポート、種類、ユーザー名を変更した場合、その接続先のパスワードが必要です。プロファイル名を変更しても認証情報は失われません。Keychain の読み書きに失敗する場合は IDE の**パスワード**設定を確認してください。KeePass も利用できます。

## 対応プロトコルとプロバイダー

Easy AI Commit は、互換 HTTP API に対象を絞っています。

- `OpenAI Compatible`
- `Anthropic Compatible`

`Custom` プリセットは両方のプロトコルに対応し、Protocol フィールドで区別します。名前付きプリセットは、一般的なモデルプロバイダーや集約プラットフォーム向けに、対応プロトコル、既定のエンドポイント、モデル、認証方式を提供します。

現在のプリセット：

| 対応プロトコル | プリセット |
|---|---|
| `OpenAI Compatible` + `Anthropic Compatible` | `Custom`、DeepSeek、OpenRouter、GLM、Alibaba Bailian、Kimi、MiniMax、SiliconFlow、StepFun、Tencent TokenHub |
| `OpenAI Compatible` | OpenAI、Gemini、Grok、Volcano Ark、Baidu Qianfan、iFlytek Spark、Mistral AI、Groq、Together AI、Fireworks AI、Cerebras、Cohere、Perplexity、Hugging Face |
| `Anthropic Compatible` | Anthropic |

独自の IAM、OAuth、SigV4、デプロイリソース、その他クラウド固有の設定を必要とするエンタープライズクラウドプラットフォームは、現在の対象外です。

## ローカライズ

プラグイン UI は現在の IDE ロケールに従います。英語をフォールバック言語とし、次の翻訳を同梱しています。

| 言語 | ロケール |
|---|---|
| English | デフォルト |
| 简体中文 | `zh-CN` |
| 日本語 | `ja` |
| 한국어 | `ko` |

出力言語は UI 言語とは別に設定できます。「自動」を選ぶとコミットメッセージの生成時に IDE の言語に従い、必要に応じて 18 種類の言語プリセットから手動で選択することもできます。

必要な言語がプリセット一覧にない場合は、カスタムプロンプトで対象言語を直接指定できます。デフォルトテンプレートは `{language}` 変数を使って言語要件を挿入します。常に特定の言語を使用する場合は、両方の制約を残さず、`{language}` を含む指示を明確な言語指定に書き換えてください。

## セキュリティとプライバシー

- API キーは JetBrains Password Safe を通じて保存されます。
- 変更内容は、ユーザーが生成ボタンをクリックした場合にのみ送信されます。
- リクエストは、選択中のモデルプロファイルに設定されたエンドポイントへ送信されます。

設定するモデルプロバイダーまたはゲートウェイのプライバシーおよびデータ取り扱い条件も確認してください。

## 開発

このプロジェクトは Kotlin/JVM、IntelliJ Platform Gradle Plugin 2.x、Gradle 9.3.1 を使用し、IntelliJ IDEA 2024.2.6 を開発ベースラインとしています。

```bash
./gradlew test
./gradlew buildPlugin
./gradlew verifyPluginStructure
./gradlew runIde
```

コミット済みのベースラインを変更せず、インストール済み IDE をローカル検証に利用することもできます。

```bash
./gradlew test buildPlugin verifyPlugin \
  -PlocalIdePath="/path/to/IntelliJ IDEA.app"
```

プロジェクト構成：

```text
src/main/kotlin/       プラグイン実装
src/main/resources/    プラグイン記述子、アイコン、ローカライズリソース
src/test/kotlin/       単体テストと統合テスト
branding/              ロゴの元データとアセット資料
readme/                多言語 README ファイル
tools/                 開発・アセット生成ユーティリティ
```

## コントリビューション

Issue と Pull Request を歓迎します。変更を送信する前に `./gradlew test` を実行してください。表示テキストを追加または変更する場合は、英語、簡体字中国語、日本語、韓国語のリソースバンドルを同期してください。

## ライセンス

Easy AI Commit は [MIT License](../LICENSE) の下で公開されています。

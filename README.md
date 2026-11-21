# Pocket English（旅行・日常英会話アプリ）

Kotlin + Jetpack Compose（Material 3）製。現在は **ステップ1「フレーズ」タブ** まで実装済み。

## セットアップ
1. Android Studio（Ladybug 以降）でこのフォルダを開く
2. `local.properties.example` を参考に `local.properties` に `GEMINI_API_KEY=...` を追記（フレーズタブだけなら空でも動作。詳しくは下の「AI機能を使うビルド手順」）
3. 実機を接続して ▶ Run

## スマホに直接インストール（PC不要）
GitHub Actions が `main` へのpushとPRごとに debug APK をビルドします。
1. スマホのブラウザで GitHub にログインし、このリポジトリの **Actions** タブ →「Build debug APK」の最新の成功した実行（緑のチェック）を開く
2. 下部の **Artifacts** から `pocket-english-debug-apk` をダウンロード（zip形式）
3. ファイルアプリで zip を開き、中の `app-debug.apk` をタップ
4. 「提供元不明のアプリ」の許可を求められたら、ブラウザ（またはファイルアプリ）に許可を出してインストール
5. 初回は Play プロテクトの警告が出ることがあります。「詳細」→「インストールする」で続行

debug用の署名鍵（`app/debug.keystore`）を固定しているので、新しいAPKは上書きインストールでき、お気に入りも消えません。
※ 公開リポジトリのため、CIビルドのAPKにはAPIキーを入れていません（AI機能は手元のビルドで使う想定）。

## ディレクトリ構成
```
PocketEnglish/
├── settings.gradle.kts / build.gradle.kts / gradle.properties
├── gradle/libs.versions.toml          … 依存バージョン一元管理
├── local.properties.example           … APIキー設定の雛形（local.propertiesはGit管理外）
├── .github/workflows/android-debug-apk.yml … debug APK を自動ビルド
└── app/
    ├── build.gradle.kts               … BuildConfig に GEMINI_API_KEY / GEMINI_MODEL を注入
    ├── debug.keystore                 … debug専用の固定署名鍵（上書きインストール用）
    └── src/main/
        ├── AndroidManifest.xml
        ├── res/ (テーマ・アイコン・文字列)
        └── java/com/example/pocketenglish/
            ├── PocketEnglishApp.kt    … Application（AppContainer を保持）
            ├── AppContainer.kt        … 手動DI（DataStore / TTS）
            ├── MainActivity.kt
            ├── model/Phrase.kt        … Phrase / PhraseCategory
            ├── data/
            │   ├── PhraseData.kt      … 内蔵フレーズ 6カテゴリ×15
            │   ├── UserPreferencesRepository.kt … お気に入り・ゆっくり再生（DataStore）
            │   └── remote/            … ApiConfig / HttpClientProvider / GeminiClient（OkHttp で Gemini API を呼ぶ）
            ├── tts/TtsManager.kt      … TextToSpeech ラッパー（StateFlow公開）
            └── ui/
                ├── MainScreen.kt      … BottomNavigation 3タブ
                ├── phrase/            … PhraseScreen / PhraseViewModel
                ├── common/            … カテゴリアイコン・準備中画面
                └── theme/             … 高コントラスト配色（ライト/ダーク）
```
AI通信は `data/remote/GeminiClient.kt`（Gemini generateContent を OkHttp で呼ぶ共通クライアント）。各タブの画面・音声入力は `ui/chat/`（AI会話）と `ui/interpreter/`（瞬間通訳）

## 主な仕様（フレーズタブ）
- カテゴリ切替チップ・ゆっくり再生トグルは画面下部（親指で届く位置）
- カードをタップ → 英語を読み上げ（再生中に同じカードをタップで停止、別カードで即切替）
- 長押し or ★ → お気に入りのON/OFF（DataStoreに保存、再起動後も保持）
- 「お気に入り」チップでお気に入りだけを一覧表示
- 英語音声データが端末にない場合は警告と「設定」ボタンを表示
- アプリをバックグラウンドにすると読み上げ停止

## AI機能を使うビルド手順（APIキー入り・PCが必要）
AI会話／瞬間通訳は **Gemini API**（無料枠あり・クレジットカード登録不要）を使います。APIキーを入れて**自分のPCでビルド**します（ActionsのAPKにはキーが入っていません）。

1. **APIキーを作る**：PCのブラウザで https://aistudio.google.com に Google アカウントでログイン →「Get API key」→「Create API key」。表示された `AIza` で始まる文字列をコピー（支払い設定はしなくてOK＝無料枠のまま）
2. **Android Studio を入れる**：https://developer.android.com/studio からダウンロードしてインストール。初回起動時のウィザードは「Standard」で進め、SDK のダウンロード完了まで待つ
3. **コードを取得**：Android Studio の「File → New → Project from Version Control」に `https://github.com/satton2020py/pocket-english.git` を入れて Clone（または GitHub の「Code → Download ZIP」を展開して「File → Open」）
4. **キーを書く**：プロジェクト直下（`settings.gradle.kts` と同じ場所）の `local.properties` を開く（Android Studio が `sdk.dir=...` 入りで作ってくれます。左のファイル一覧を「Project」表示にすると見えます）。最後の行に次の1行を追加して保存
   ```
   GEMINI_API_KEY=AIzaここにコピーしたキー
   ```
   `"` で囲まない・前後に空白を入れないこと。このファイルは `.gitignore` 済みなので GitHub には上がりません
5. **反映**：上部の象アイコン「Sync Project with Gradle Files」を押す
6. **スマホをつなぐ**：スマホの「設定 → デバイス情報 → ビルド番号」を7回タップして開発者向けオプションを有効化 →「開発者向けオプション → USBデバッグ」をON → USBでPCに接続し、スマホに出る「USBデバッグを許可」で OK
7. **実行**：Android Studio 上部のデバイス欄にスマホが出たら ▶ Run。ActionsのAPKと同じ署名鍵なので、上書きインストールされお気に入りも残ります

うまくいかないとき
- AI会話で「APIキーが設定されていない」と出る → `local.properties` の場所・行の書き方を確認して、もう一度 Sync → Run
- 「APIキーが無効です」と出る → キーのコピー漏れ、または AI Studio でキーが削除されていないか確認
- 「利用回数の上限に達したか、混み合っています」と出る → 無料枠は1分あたり・1日あたりの回数に上限があります。1分ほど待つか、1日の上限なら太平洋時間の0時（日本時間の16〜17時ごろ）のリセット後に再度お試しください
- 使うモデルを変えたいとき → `local.properties` に `GEMINI_MODEL=モデル名` を追加（省略時は `gemini-3.5-flash-lite`。無料枠の回数が多い Flash-Lite を既定にしています）

無料枠では、送った内容が Google のサービス改善に使われることがあります。個人情報やパスワードは話さないでください。

ビルドしたAPKファイル自体が欲しいときは「Build → Build App Bundle(s) / APK(s) → Build APK(s)」。出来た `app-debug.apk` にはキーが入っているので、**人に渡したり公開したりしないでください**。

## セキュリティ注意
BuildConfig に入れたAPIキーはAPKを解析すると取り出せます。個人利用は問題ありませんが、ストア配布する場合は自前のプロキシサーバー経由に切り替えてください。

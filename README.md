# Pocket English（旅行・日常英会話アプリ）

Kotlin + Jetpack Compose（Material 3）製。現在は **ステップ1「フレーズ」タブ** まで実装済み。

## セットアップ
1. Android Studio（Ladybug 以降）でこのフォルダを開く
2. `local.properties.example` を参考に `local.properties` に `ANTHROPIC_API_KEY=...` を追記（フレーズタブだけなら空でも動作）
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
    ├── build.gradle.kts               … BuildConfig に ANTHROPIC_API_KEY を注入
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
            │   ├── PhraseData.kt      … 内蔵フレーズ 6カテゴリ×5
            │   ├── UserPreferencesRepository.kt … お気に入り・ゆっくり再生（DataStore）
            │   └── remote/            … ApiConfig / HttpClientProvider（OkHttp、2・3タブ用の下地）
            ├── tts/TtsManager.kt      … TextToSpeech ラッパー（StateFlow公開）
            └── ui/
                ├── MainScreen.kt      … BottomNavigation 3タブ
                ├── phrase/            … PhraseScreen / PhraseViewModel
                ├── common/            … カテゴリアイコン・準備中画面
                └── theme/             … 高コントラスト配色（ライト/ダーク）
```
今後追加予定: `speech/`（SpeechRecognizer）、`data/remote/ClaudeClient.kt`、`ui/chat/`、`ui/interpreter/`

## 主な仕様（フレーズタブ）
- カテゴリ切替チップ・ゆっくり再生トグルは画面下部（親指で届く位置）
- カードをタップ → 英語を読み上げ（再生中に同じカードをタップで停止、別カードで即切替）
- 長押し or ★ → お気に入りのON/OFF（DataStoreに保存、再起動後も保持）
- 「お気に入り」チップでお気に入りだけを一覧表示
- 英語音声データが端末にない場合は警告と「設定」ボタンを表示
- アプリをバックグラウンドにすると読み上げ停止

## セキュリティ注意
BuildConfig に入れたAPIキーはAPKを解析すると取り出せます。個人利用は問題ありませんが、ストア配布する場合は自前のプロキシサーバー経由に切り替えてください。

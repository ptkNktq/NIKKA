# NIKKA - 日課管理アプリ

ゲームのデイリータスクなど、日課をグループごとに管理するデスクトップアプリ。

## 機能

- **グループ管理**: ゲームタイトルごとにタスクをグループ化
- **タスク管理**: グループ内でタスクの追加・削除・完了チェック
- **自動リセット**: グループごとにリセット時刻を設定、起動時に自動リセット
- **手動リセット**: グループ単位でタスクの完了状態を一括リセット
- **ドラッグ&ドロップ**: グループ・タスクの並べ替え
- **折りたたみ**: グループを展開/折りたたみ（全タスク完了時に自動折りたたみ）
- **右クリックメニュー**: タスク削除、グループの日課追加・リセット時刻設定・削除
- **データ永続化**: JSON ファイルによるローカル保存 (`~/.nikka/data.json`)

## 技術スタック

| カテゴリ | 技術 |
|---|---|
| 言語 | Kotlin 2.1.10 |
| UI | Compose Multiplatform 1.7.3 (Desktop) |
| アーキテクチャ | MVVM + Repository パターン |
| DI | Koin 4.0.2 |
| シリアライズ | kotlinx-serialization 1.7.3 |
| フォント | Noto Sans JP |
| Lint | detekt + ktlint (detekt-formatting) |

## モジュール構成

```
:core:model     ← データクラス (DailyTask, TaskGroup)
:core:data      ← Repository インターフェース + JSON 実装
:core:ui        ← テーマ、共通コンポーネント、フォントリソース
:feature:home   ← ホーム画面 (HomeScreen, HomeViewModel)
:composeApp     ← エントリポイント、DI 設定、ウィンドウ制御
```

依存関係:

```
:core:model     → (なし)
:core:data      → :core:model
:core:ui        → (なし)
:feature:home   → :core:model, :core:data, :core:ui
:composeApp     → 全モジュール
```

## 開発環境

- **ビルド**: Gradle 8.12 (Wrapper)
- **動作環境**: Windows 11 (JVM Desktop)
- **開発環境**: WSL2 または Windows

## ビルド・実行

```bash
# ビルド
./gradlew assemble                    # WSL
gradlew.bat assemble                  # Windows

# 実行
DISPLAY=:0 ./gradlew composeApp:run   # WSL (X11 転送)
gradlew.bat composeApp:run            # Windows

# テスト
./gradlew :feature:home:desktopTest :core:data:desktopTest   # WSL
gradlew.bat :feature:home:desktopTest :core:data:desktopTest # Windows

# Lint
./gradlew detekt                      # WSL
gradlew.bat detekt                    # Windows

# Lint 自動修正
./gradlew detekt --auto-correct       # WSL
gradlew.bat detekt --auto-correct     # Windows
```

## データ保存先

`~/.nikka/data.json` にグループとタスクのデータを JSON で保存。
Windows 実行時は `C:\Users\<ユーザー名>\.nikka\data.json`。

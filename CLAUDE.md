# NIKKA - Project Instructions

## Build & Test

```bash
# ビルド
./gradlew assemble

# テスト
./gradlew :feature:home:desktopTest

# Lint チェック
./gradlew detekt

# Lint 自動修正 (import順序等)
./gradlew detekt --auto-correct

# アプリ実行 (WSL)
DISPLAY=:0 ./gradlew composeApp:run
```

## Architecture

- **MVVM + Repository**: ViewModel が StateFlow で UI 状態を管理、Repository がデータ永続化を担当
- **Feature-based Gradle modules**: `:core:model`, `:core:data`, `:core:ui`, `:feature:home`, `:feature:settings`, `:feature:license`, `:composeApp`
- **DI**: Koin (`composeApp/src/commonMain/kotlin/com/nikka/di/AppModule.kt`)

## Conventions

- UI 言語は日本語
- Composable 関数は PascalCase (`detekt.yml` の `FunctionNaming` で許可済み)
- `LongParameterList` の閾値は 14 (Composable のコールバックが多くなるため)
- import 順序の修正は `./gradlew detekt --auto-correct` に任せる
- ターゲットは `jvm("desktop")` のみ (commonMain で JVM API 使用可)
- テーマカラーはダーク系ラベンダー (`core/ui/src/commonMain/kotlin/com/nikka/core/ui/theme/Color.kt`)

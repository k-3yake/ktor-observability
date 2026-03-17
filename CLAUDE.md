# Project Overview

Ktor + Datadog のサンプルアプリケーション。
O
## Tech Stack
- Kotlin 2.3.0 / Java 21
- Ktor 3.4.0 (Server: Netty, Client: Java HttpClient)
- kotlinx-serialization
- Logback + LogstashEncoder (JSON ログ)
- Datadog dd-java-agent 1.60.1
- テスト: JUnit 5, WireMock 3.9.1

## Build
- Java / Maven は **mise** で管理（system PATH にない）
- `export PATH="$HOME/.local/share/mise/shims:$PATH" && export JAVA_HOME=$(mise where java 2>/dev/null)`
- Maven は `~/.m2` への書き込みがあるため sandbox 無効が必要

## Module Structure
単一 Maven モジュール `sample-app/`

```
sample-app/src/main/kotlin/com/example/
├── Application.kt          # エントリポイント、プラグイン設定、DI 配線
├── client/
│   └── ExternalApiClient.kt  # 外部 API 呼び出し (Java HttpClient)
├── model/
│   ├── User.kt             # ドメインモデル
│   ├── Email.kt            # 値オブジェクト
│   └── PhoneNumber.kt      # 値オブジェクト
├── repository/
│   └── UserRepository.kt   # インメモリ保存 + Request/Response DTO
├── routes/
│   ├── UserRoute.kt        # POST /api/users
│   └── ProxyRoute.kt       # GET /api/proxy (トレース伝播検証用)
├── service/
│   └── UserService.kt      # ビジネスロジック
└── validator/
    └── UserValidator.kt    # バリデーション
```

## Endpoints
| Method | Path | 概要 |
|--------|------|------|
| POST | `/api/users` | ユーザー作成 (バリデーション → サービス → リポジトリ) |
| GET | `/api/proxy` | 外部 API へのプロキシ (Dispatchers.IO 経由でトレース伝播を検証) |

## Test Structure
- `UserRouteTest` — 統合テスト (デシリアライズエラー)
- `UserServiceTest` — ユニットテスト
- `UserValidatorTest` — ユニットテスト
- `TraceContextPropagationTest` — 統合テスト (WireMock, DD トレースヘッダ伝播検証)

テストは dd-java-agent 付きで実行される (`-javaagent` in surefire argLine)。

## Logging
- `logback.xml`: LogstashEncoder で JSON 出力 (STDOUT)
- MDC フィールドは LogstashEncoder が自動的に JSON に含める

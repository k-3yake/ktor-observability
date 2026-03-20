# ktorログ実装

## 解決する課題
- 機微情報のマスキング
- 外部から受け取ってすぐにログを出す
- trace_id,span_id,parent_idを出す
- gcloudを想定してhttpRequestフィールドを出す
- カラーコードが出る

## 機微情報のマスキング
### 案1 annotation
#### 概要
@Sensitiveを作成し、それがついてるtoStringをオーバーライド

#### 実装
``` 
│ 1. kotlin-reflect 依存追加                                                                                                                                                                                                                 │
│                                                                                                                                                                                                                                            │
│ - sample-app/pom.xml に追加（findAnnotation に必要）                                                                                                                                                                                       │
│                                                                                                                                                                                                                                            │
│ 2. @Sensitive アノテーション作成                                                                                                                                                                                                           │
│                                                                                                                                                                                                                                            │
│ - 新規: com.example.logging.Sensitive                                                                                                                                                                                                      │
│ - @Target(PROPERTY), @Retention(RUNTIME)                                                                                                                                                                                                   │
│                                                                                                                                                                                                                                            │
│ 3. toSafeString() ユーティリティ作成                                                                                                                                                                                                       │
│                                                                                                                                                                                                                                            │
│ - 新規: com.example.logging.SafeToString.kt                                                                                                                                                                                                │
│ - Any.toSafeString() 拡張関数                                                                                                                                                                                                              │
│ - @Sensitive 付きプロパティの値を ali***com のようにマスク                                                                                                                                                                                 │
│ - 短い値（4文字以下）は *** に置換                                                                                                                                                                                                         │
│                                                                                                                                                                                                                                            │
│ 4. User に適用                                                                                                                                                                                                                             │
│                                                                                                                                                                                                                                            │
│ - email, phoneNumber に @Sensitive 付与                                                                                                                                                                                                    │
│ - override fun toString() = toSafeString()         
```

#### 課題
- Kotlinぽくない
- リフレクション部分がなんか複雑
- もっとシンプルにやりようがある気が
- ドメインじゃないロジックがドメイン層に紛れ込む
- 使ってるところに全部アノテーションいれないといけない

### 案2 toStringを直接オーバーライド
#### 実装
```kotlin
data class Email(val value: String) {
  override fun toString() = "***"
}
```
#### 感想
- シンプル
- アノテーションよりよく見える

### 案3 abstract class で潰す
#### 実装
```kotlin
abstract class SensitiveValue {
    // final にして data class の自動生成も封じる
    final override fun toString(): String = redacted()
    abstract fun redacted(): String
}

data class Email(val value: String) : SensitiveValue() {
    override fun redacted() = value.take(2) + "***@" + value.substringAfter("@")
}

println(Email("alice@example.com"))
// al***@example.com
```
#### 感想
- ドメインの継承枠がつぶされるのが痛すぎる

### 案4 ドメイン型の内部で Sensitive を持つ
#### 実装
```kotlin
data class Email(private val _value: Sensitive) {
    
    //リファクタリングしてこっちのコンストラクタは潰してもいい
    constructor(raw: String) : this(Sensitive(raw))

    val value: String get() = _value.unwrap()
}
```
#### 感想
- 「メールは機微」という知識が Email 型の中に閉じる
- 既存の `Email("alice@example.com")` や `.value` がそのまま動く
- Sensitive の存在が外から見えない


## デシリアライズ失敗ログ
```kotlin
    val log = log
    install(StatusPages) {
        exception<BadRequestException> { call, cause ->
            log.warn("Request deserialization failed: ${cause.message}", cause)
            call.respond(HttpStatusCode.BadRequest, ErrorResponse(cause.message ?: "Bad Request"))
        }
    }
```

## リクエストメタ情報の出力(Ktor CallLogging プラグイン + MDC)
- Ktorの標準プラグイン CallLogging を使い、リクエスト情報をMDCに載せる
- ボディは対象外（ヘッダ・パス・パラメータのみ）
- 向き: パス・メソッド・ステータスなどメタ情報のログで十分な場合

## リクエストボディのログ出力
```
案1: receive 後にルートハンドラ内で明示ログ
各ルートで call.receive<T>() した直後に logger.info(...) を書く。デシリアライズ済みオブジェクトを扱えるため、copy() でのマスク等が容易。ルートごとにログ内容を変えたい場合やルート数が少ない場合に向く。ルートが増えるとログコードが散在するのが欠点。

案2: DoubleReceive + 横断的ボディログ
全エンドポイント共通でボディを記録したい場合。Ktor 2.x以降は DoubleReceive プラグインをインストールすれば二重読み取り問題が解消され、CallLoggingの format 内で call.receiveText() してボディをログ出力できる。より高度な制御（マスク・フィルタ等）が必要なら createApplicationPlugin で独自プラグインを作る方法もある。ただし DoubleReceive はexperimental APIである点に注意。
```
→そもそも安易に全部だすようなものではない

## parent_idの出力

### 実装
```kotlin
    intercept(ApplicationCallPipeline.Monitoring) {
        val parentId = call.request.headers["x-datadog-parent-id"] ?: "0"
        org.slf4j.MDC.putCloseable("dd.parent_id", parentId).use { proceed() }
    }
```

### 前提知識
#### ktorリクエストパイプライン
```
  1. Setup — 初期化処理（属性のセットアップなど）                                                                                                                                                                                                                                                                   
  2. Monitoring — 横断的関心事（ログ、メトリクス、例外ハンドリング）
  3. Plugins — プラグインの処理（認証、セッション、CORS など）
  4. Call — ルーティング解決・レスポンス生成
```
### 仕組み
- MonitoringフェーズでMDCに埋め込むことにより、プラグインフェーズ以降で利用可能となる
- プラグインには認証等の処理のログが入ることがあるためここに入れるのが一般的
- Setupフェーズでも動くが、ktorの設計意図とずれる
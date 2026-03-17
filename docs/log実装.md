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

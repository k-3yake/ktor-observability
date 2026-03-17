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

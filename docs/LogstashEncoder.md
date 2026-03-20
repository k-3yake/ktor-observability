# LogstashEncoder

## 概要
- 構造化ログを出力
- MDCの値をトップレベルに出力
- スタックトレースも構造化される

## MDCによるカスタムフィールドの追加
```kotlin
  MDC.put("userId", "12345")
  logger.info("処理完了")  // → JSON に "userId": "12345" が自動で含まれる
  MDC.remove("userId")
```
## 呼び出し引数によるログの違い
### logger.info("注文完了")
stringがmessageに入る
```json
{"message": "注文完了", "@timestamp": "...", "level": "INFO"}
```
### logger.info("注文完了 {}", map)
map.toStringがmessageのプレースホルダに入る
```json
{"message": "注文完了 {userId=123, orderId=456}", "@timestamp": "...", "level": "INFO"}
```

### logger.info("注文完了 {}", StructuredArguments.entries(map))
map.toStringがmessageのプレースホルダに入る。さらにmapがトップレベルのフィールドになる。
```json
 {"message": "注文完了 userId=123, orderId=456", "userId": "123", "orderId": "456", "@timestamp": "...", "level": "INFO"}
```

### logger.info("注文完了", map)
プレースホルダは無いのでメッセージはそのまま。結果、mapは捨てられることになる。
```json
 {"message": "注文完了", "@timestamp": "...", "level": "INFO"}
```

### logger.info("注文完了", StructuredArguments.entries(map))
プレースホルダは無いのでメッセージはそのまま。mapがトップレベルのフィールドになる。
```json
 {"message": "注文完了", "userId": "123", "orderId": "456", "@timestamp": "...", "level": "INFO"}
```
### まとめ
- 基本プレースホルダーは使う
- トップに出したいときはStructuredArguments.entries(map)でそうじゃないときはそのままmap

#### StructuredArguments.entriesmap
- StructuredArgumentインターフェース。後はLogstashEncoderがいい感じにしてくれてる。
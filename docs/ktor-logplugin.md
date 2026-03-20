# Ktorログ

## ログの基本


## LogPlugin
### サーバサイド
#### 出来ること
受信したHTTPリクエストの情報をログ出力
- 出力エンドポイントのフィルター
- MDCへのパラメータ格納（後のログ出力で使えるように）

### クライアントサイド
未調査


### トレースログの出し方例
```kotlin
import datadog.trace.api.DDTags
import io.opentracing.Tracer
import io.opentracing.util.GlobalTracer

suspend fun <T> traced(operationName: String, block: suspend () -> T): T {
val tracer = GlobalTracer.get()
val parent = tracer.activeSpan()
val span = tracer.buildSpan(operationName)
.apply { if (parent != null) asChildOf(parent) }
.start()
return try {
// spanをactivateしてブロックを実行
tracer.activateSpan(span).use {
block()
}
} catch (e: Exception) {
span.setTag(DDTags.ERROR_MSG, e.message)
span.setTag("error", true)
throw e
} finally {
span.finish()
}
}
使い方：
kotlinclass UserRepository(private val db: Database) {
suspend fun findById(id: Long): User? = traced("UserRepository.findById") {
db.query("SELECT ...")
}
}
```
とりあえずコード例を生成してみただけ。これで良いのかもよくわかってない。
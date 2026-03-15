# ktor-with-dd

## 目的
- krotにDataDogを使用し、idが正しく設定されるかを確認する

### 方針
- ktorで他のapiを呼び出す
- エンジンはnetty
- corutinをまたがってコンテキストが落ちないか
- 検証はe2eの形式で行う

## Ktor + Datadog 注意点

### MDCはコルーチンのスレッド切替で消失する

dd-java-agentはMDCに`dd.trace_id`/`dd.span_id`を自動設定するが、MDCは`ThreadLocal`ベースのため`withContext(Dispatchers.IO)`等でスレッドが切り替わると値が`null`になる。

```kotlin
// Dispatchers.IO のスレッドでは MDC.get("dd.trace_id") → null
withContext(Dispatchers.IO) {
    MDC.get("dd.trace_id") // null
}
```

### 分散トレーシング自体は影響を受けない

dd-java-agentによるHTTPヘッダー伝搬（`x-datadog-trace-id`）はagent内部のコンテキストで管理されるため、コルーチンのスレッド切替の影響を受けない。

| 伝搬の種類 | 仕組み | スレッド切替の影響 |
|---|---|---|
| HTTPヘッダー (`x-datadog-trace-id`) | dd-java-agentがOkHttpを自動計装 | なし |
| MDC (`dd.trace_id`) | `ThreadLocal` | 消失する |

### 対策

ログにトレースIDを出力する等でMDCが必要な場合は、`kotlinx-coroutines-slf4j`の`MDCContext()`を使用してコルーチン間でMDCを伝搬させる。

```kotlin
// 依存追加: org.jetbrains.kotlinx:kotlinx-coroutines-slf4j
withContext(Dispatchers.IO + MDCContext()) {
    MDC.get("dd.trace_id") // 値が引き継がれる
}
```

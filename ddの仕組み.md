# trace_idの採番
- nettyをフック。x-datadog-trace-idがなければ採番。
- DDSpanContextに格納。


# span_idの採番
下記のようにインストゥルメントライブラリの呼び出し箇所でspanが生成される。
```
Netty の channelRead → 自動
Ktor の Routing → 自動
JDBC の executeQuery → 自動
OkHttp / Ktor HttpClient の送信 → 自動
自分で書いた userService.findById() → 自動ではない
```
- springはktorより自動でspanされる箇所が多い
- フックしたメソッドの終了でspanが終了(tryで囲みfinallyで処理)


# 別API呼び出しのトレーシング
```
アプリコード: httpClient.get("http://other-service/api")
  │
  ▼
dd-trace-java が送信メソッドをフック
  │
  │  1. 現在のアクティブスパンを ScopeManager (ThreadLocal) から取得
  │  2. そのスパンの trace_id と span_id を取り出す
  │  3. HTTPリクエストのヘッダーに注入:
  │       x-datadog-trace-id: <trace_id>
  │       x-datadog-parent-id: <現在の span_id>
  │       x-datadog-sampling-priority: 1
  │  4. 新しい子スパン (http.request) を生成
  │
  ▼
実際のHTTP送信
```


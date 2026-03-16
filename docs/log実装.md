# ktorログ実装

## 解決する課題
- 機微情報のマスキング
- 外部から受け取ってすぐにログを出す
- trace_id,span_id,parent_idを出す
- gcloudを想定してhttpRequestフィールドを出す
- カラーコードが出る

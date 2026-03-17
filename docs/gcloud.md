# gcloudについて
- Cloud Logging のログビューアが logging.googleapis.com/trace を認識すると、同一 trace のログをグルーピングして表示してくれる
  - 1:N呼び出しの場合効果的 
- logging.googleapis.com/spanIdも同様。traceより活躍する場面は低いかも
- httpRequestがあると、ログビューアーで見やすくなる
#!/bin/bash
PORT=${1:-8080}

echo "Sending 3 concurrent POST /api/users requests to localhost:$PORT ..."

for i in 1 2 3; do
  curl -s -X POST "http://localhost:$PORT/api/users" \
    -H "Content-Type: application/json" \
    -H "x-datadog-trace-id: ${i}000" \
    -H "x-datadog-parent-id: ${i}00" \
    -d "{\"name\":\"user$i\",\"email\":\"u$i@example.com\",\"phoneNumber\":\"090-1234-567$i\",\"age\":20}" &
done
wait

echo ""
echo "Done."

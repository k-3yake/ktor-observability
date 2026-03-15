#!/bin/bash
cat << 'EOF' >> ~/.bashrc

notify() {
    curl -s -d "${1:-done}" "ntfy.sh/${NTFY_TOPIC}"
}
EOF

#!/usr/bin/env bash
# ==============================================================================
# Dettle Live Full-Fidelity Monitor
# Streams colorized logs for:
#   - Every User Tap / Click / Touch
#   - Every Navigation Screen Change
#   - Every Outbound & Inbound HTTP / AI API Request & Response Body
#   - Every Tool execution & Agent reasoning step
#   - Every Crash, Exception, and Error
# ==============================================================================

set -eo pipefail

APP_PACKAGE="com.dettle.app"

# Find ADB target
DEVICE_IP="10.253.66.66:5555"

# Auto-connect if not already connected
if ! adb devices | grep -q "$DEVICE_IP"; then
    echo "Attempting wireless connect to $DEVICE_IP..."
    adb connect "$DEVICE_IP" >/dev/null 2>&1 || true
    sleep 1
fi

TARGET_SERIAL=""
if adb devices | grep "$DEVICE_IP" | grep -q "device"; then
    TARGET_SERIAL="$DEVICE_IP"
else
    # Fallback to any attached device
    TARGET_SERIAL=$(adb devices | grep -v "List" | grep "device" | awk '{print $1}' | head -n 1)
fi

if [[ -z "$TARGET_SERIAL" ]]; then
    echo "❌ No device found. Make sure Wi-Fi or USB is connected."
    exit 1
fi

ADB="adb -s $TARGET_SERIAL"

echo "=================================================================="
echo "       DETTLE FULL-FIDELITY LIVE MONITOR (Target: $TARGET_SERIAL)"
echo "  [CLICKS]  [NAVIGATION]  [FULL API BODIES]  [AGENTS]  [CRASHES]  "
echo "=================================================================="

# Ensure clean start if requested
if [[ "$1" == "-c" || "$1" == "--clear" ]]; then
    echo "Clearing logcat buffer..."
    $ADB logcat -c
fi

echo "Streaming live logs... (Press Ctrl+C to stop)"
echo "------------------------------------------------------------------"

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
MAGENTA='\033[0;35m'
CYAN='\033[0;36m'
WHITE='\033[1;37m'
BOLD='\033[1m'
DIM='\033[2m'
NC='\033[0m' # No Color

$ADB logcat -v time \
    AndroidRuntime:E \
    FATAL:E \
    DettleClick:V \
    DettleTouch:V \
    DettleNav:V \
    DettleHttp:V \
    AgentLogger:V \
    DettleDev:V \
    Dettle:V \
    Agent:V \
    WebViewSession:V \
    BackupManager:V \
    ModeRouter:V \
    ReActLoop:V \
    *:S | while read -r line; do
        if [[ "$line" =~ "FATAL" || "$line" =~ "AndroidRuntime" || "$line" =~ " E " || "$line" =~ "E/" ]]; then
            echo -e "${RED}${BOLD}$line${NC}"
        elif [[ "$line" =~ "ModeRouter" ]]; then
            echo -e "${GREEN}${BOLD}$line${NC}"
        elif [[ "$line" =~ "DettleClick" ]]; then
            echo -e "${GREEN}${BOLD}$line${NC}"
        elif [[ "$line" =~ "DettleNav" ]]; then
            echo -e "${CYAN}${BOLD}$line${NC}"
        elif [[ "$line" =~ "DettleHttp" ]]; then
            echo -e "${BLUE}$line${NC}"
        elif [[ "$line" =~ "AgentLogger" || "$line" =~ "ReActLoop" ]]; then
            echo -e "${MAGENTA}$line${NC}"
        elif [[ "$line" =~ "DettleTouch" ]]; then
            echo -e "${DIM}$line${NC}"
        elif [[ "$line" =~ " W " || "$line" =~ "W/" ]]; then
            echo -e "${YELLOW}$line${NC}"
        else
            echo "$line"
        fi
    done

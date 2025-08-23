#!/bin/bash

# WearInterval 24-Hour Battery Monitoring Script
# Collects battery data every 4 hours

LOG_DIR="/home/david/Insync/dmlerner@gmail.com/Google Drive/coding/wearinterval2/battery_logs"
mkdir -p "$LOG_DIR"

TIMESTAMP=$(date '+%Y%m%d_%H%M%S')
REPORT_FILE="$LOG_DIR/battery_report_$TIMESTAMP.txt"

echo "=== WearInterval Battery Report - $TIMESTAMP ===" > "$REPORT_FILE"
echo "Generated at: $(date)" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"

# Check ADB connection
if ! adb devices | grep -q "device$"; then
    echo "ERROR: No device connected via ADB" >> "$REPORT_FILE"
    exit 1
fi

# Collect battery status
echo "=== BATTERY STATUS ===" >> "$REPORT_FILE"
adb shell dumpsys battery >> "$REPORT_FILE" 2>&1

echo "" >> "$REPORT_FILE"
echo "=== WEARINTERVAL APP STATS ===" >> "$REPORT_FILE"
adb shell dumpsys batterystats com.wearinterval >> "$REPORT_FILE" 2>&1

echo "" >> "$REPORT_FILE"
echo "=== POWER MANAGER STATE ===" >> "$REPORT_FILE"
adb shell dumpsys power | head -50 >> "$REPORT_FILE" 2>&1

echo "" >> "$REPORT_FILE"
echo "=== RUNNING SERVICES ===" >> "$REPORT_FILE"
adb shell dumpsys activity services | grep -A5 -B5 "wearinterval" >> "$REPORT_FILE" 2>&1

echo "Battery data collected: $REPORT_FILE"
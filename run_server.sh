#!/bin/bash
# UniMap Persistent Background Server Supervisor
while true; do
  echo "[$(date)] Starting UniMap Web Server..." >> /var/log/unimap.log 2>&1
  node /app/applet/server.js >> /var/log/unimap.log 2>&1
  echo "[$(date)] Server process exited ($?). Restarting in 1 second..." >> /var/log/unimap.log 2>&1
  sleep 1
done

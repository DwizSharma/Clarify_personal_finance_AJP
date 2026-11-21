#!/bin/bash

echo "==============================="
echo "  Starting Clarify Finance App "
echo "==============================="

# Kill any previous instances
pkill -f "personal-finance-manager" 2>/dev/null
pkill -f "ngrok" 2>/dev/null
sleep 2

# Start the Spring Boot app in background
export DB_USERNAME=root
export DB_PASSWORD="OKmlg@298"
nohup java -jar /home/dwiz/Desktop/AJP_Project/project/target/personal-finance-manager-1.0.0.jar \
  > /tmp/clarify-app.log 2>&1 &

echo "⏳ Waiting for app to start..."
until curl -s http://localhost:8082/login > /dev/null 2>&1; do
  sleep 2
done
echo "✅ App is running!"

# Start ngrok tunnel
nohup /tmp/ngrok http 8082 --log=stdout > /tmp/ngrok.log 2>&1 &
sleep 4

# Get and display the live URL
URL=$(curl -s http://localhost:4040/api/tunnels | python3 -c "import sys,json; d=json.load(sys.stdin); print(d['tunnels'][0]['public_url'])")

echo ""
echo "==============================="
echo "  🌐 YOUR LIVE URL:"
echo "  $URL"
echo "==============================="
echo ""
echo "Login: Dwiz Vrat Sharma / OKmlg@298"
echo ""
echo "To stop everything: pkill -f 'personal-finance-manager' && pkill -f ngrok"

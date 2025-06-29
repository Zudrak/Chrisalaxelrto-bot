#!/bin/bash
set -e

# Start tailscaled in background with minimal options
echo "Starting tailscaled..."
tailscaled --tun=userspace-networking --socks5-server=localhost:1055 &
TAILSCALED_PID=$!

# Wait for tailscaled to start
echo "Waiting for tailscaled to start..."
sleep 10

# Check if tailscaled process is still running
if ! kill -0 $TAILSCALED_PID 2>/dev/null; then
    echo "ERROR: tailscaled process died"
    exit 1
fi

echo "Connecting to Tailscale..."
tailscale up --auth-key=$TAILSCALE_AUTH_KEY --accept-routes
echo "Connected to Tailscale"

# Start the .NET application
echo "Starting .NET application..."
exec dotnet Chrisalaxelrto.MusicStreamer.dll
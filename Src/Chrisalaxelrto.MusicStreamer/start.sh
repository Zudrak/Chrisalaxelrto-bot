#!/bin/bash
set -e

# Start tailscaled in background with proper permissions
tailscaled --state-dir=/home/app/.local/share/tailscale --socket=/var/run/tailscale/tailscaled.sock --tun=userspace-networking --socks5-server=localhost:1055 &

# Wait for tailscaled to start and create socket
echo "Waiting for tailscaled to start..."
for i in {1..30}; do
    if [ -S /var/run/tailscale/tailscaled.sock ]; then
        echo "tailscaled socket created"
        break
    fi
    sleep 1
done

# Check if socket exists
if [ ! -S /var/run/tailscale/tailscaled.sock ]; then
    echo "ERROR: tailscaled socket not found after 30 seconds"
    exit 1
fi

echo "Connecting to Tailscale..."
tailscale up --auth-key=$TAILSCALE_AUTH_KEY --accept-routes
echo "Connected to Tailscale"

# Start the .NET application
echo "Starting .NET application..."
exec dotnet Chrisalaxelrto.MusicStreamer.dll
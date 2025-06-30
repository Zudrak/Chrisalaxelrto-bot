#!/bin/bash
set -e

# Start tailscaled in the background
tailscaled --state=mem: --socket=/var/run/tailscale/tailscaled.sock --socks5-server=localhost:1055 &
sleep 2

# Authenticate with Tailscale
tailscale up --authkey=${TAILSCALE_AUTH_KEY}

# Start the .NET application
echo "Starting .NET application..."
exec dotnet Chrisalaxelrto.MusicStreamer.dll
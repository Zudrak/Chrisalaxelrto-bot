#!/bin/bash
set -e

# Start tailscaled in background
tailscaled --tun=userspace-networking --socks5-server=localhost:1055 &

# Wait for tailscaled to start
sleep 5

tailscale up --auth-key=$TAILSCALE_AUTH_KEY --accept-routes

# Start the .NET application
exec dotnet Chrisalaxelrto.MusicStreamer.dll
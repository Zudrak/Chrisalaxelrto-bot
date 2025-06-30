#!/bin/bash
set -e

# Start tailscaled in the background
tailscaled --state-dir=/home/app/.local/share/tailscale --socket=/var/run/tailscale/tailscaled.sock &
sleep 2

# Authenticate with Tailscale
tailscale up --authkey=${TAILSCALE_AUTH_KEY}

# Start the .NET application
echo "Starting .NET application..."
exec dotnet Chrisalaxelrto.MusicStreamer.dll
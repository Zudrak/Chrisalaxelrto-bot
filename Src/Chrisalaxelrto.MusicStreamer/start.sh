#!/bin/bash
set -e

echo "Connecting to Tailscale..."
tailscale up --auth-key=$TAILSCALE_AUTH_KEY
echo "Connected to Tailscale"

# Start the .NET application
echo "Starting .NET application..."
exec dotnet Chrisalaxelrto.MusicStreamer.dll
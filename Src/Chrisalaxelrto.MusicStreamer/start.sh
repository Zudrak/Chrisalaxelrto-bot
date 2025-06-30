#!/bin/bash
set -e

# Start tailscaled in the background
tailscaled --state=mem: --socket=/var/run/tailscale/tailscaled.sock --outbound-http-proxy-listen=localhost:1055 --tun=userspace-networking &
sleep 2

# Authenticate with Tailscale
tailscale up --authkey=${TAILSCALE_AUTH_KEY}

# Wait for Tailscale to be ready
echo "Waiting for Tailscale to be ready..."
timeout 30 bash -c 'until tailscale status --json | grep -q "BackendState.*Running"; do sleep 1; done'

# Configure HTTP proxy for .NET Core
export HTTP_PROXY=http://localhost:1055
export HTTPS_PROXY=http://localhost:1055

# Start the .NET application
echo "Starting .NET application with HTTP proxy..."
exec dotnet Chrisalaxelrto.MusicStreamer.dll
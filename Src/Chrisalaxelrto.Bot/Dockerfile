#See https://aka.ms/customizecontainer to learn how to customize your debug container and how Visual Studio uses this Dockerfile to build your images for faster debugging.

FROM mcr.microsoft.com/dotnet/aspnet:9.0 AS base
WORKDIR /app
USER app
EXPOSE 8080
EXPOSE 8081

FROM mcr.microsoft.com/dotnet/sdk:9.0 AS build
ARG BUILD_CONFIGURATION=Release
WORKDIR /src

COPY ["Src/Chrisalaxelrto.Bot/", "Chrisalaxelrto.Bot/"]
COPY ["Src/Chrisalaxelrto.Core/", "Chrisalaxelrto.Core/"]
WORKDIR "/src/Chrisalaxelrto.Bot"

RUN dotnet restore "./Chrisalaxelrto.Bot.csproj"
RUN dotnet build "./Chrisalaxelrto.Bot.csproj" -c $BUILD_CONFIGURATION -o /app/build

FROM build AS publish
ARG BUILD_CONFIGURATION=Release
RUN dotnet publish "./Chrisalaxelrto.Bot.csproj" -c $BUILD_CONFIGURATION -o /app/publish /p:UseAppHost=false

FROM base AS final
WORKDIR /app
USER root

# Install native dependencies and debugging tools
RUN apt-get update && apt-get install -y \
    libopus0 \
    libopus-dev \
    libsodium23 \
    libsodium-dev \
    ffmpeg \
    strace \
    && rm -rf /var/lib/apt/lists/* \
    && ldconfig

# Copy the published app first
COPY --from=publish /app/publish .

# Instead of trying to place libraries everywhere, let's try a different approach
# Remove the existing opus and libsodium packages from NuGet and rely on system libraries
RUN rm -f /app/opus.dll /app/libsodium.dll

# Create a wrapper script that sets up the environment and runs with debugging
RUN echo '#!/bin/bash' > /app/run-with-debug.sh \
    && echo 'echo "=== Library Debug Info ==="' >> /app/run-with-debug.sh \
    && echo 'ls -la /usr/lib/x86_64-linux-gnu/libopus*' >> /app/run-with-debug.sh \
    && echo 'ls -la /usr/lib/x86_64-linux-gnu/libsodium*' >> /app/run-with-debug.sh \
    && echo 'echo "=== LD_LIBRARY_PATH ==="' >> /app/run-with-debug.sh \
    && echo 'echo $LD_LIBRARY_PATH' >> /app/run-with-debug.sh \
    && echo 'echo "=== Running with LD_DEBUG ==="' >> /app/run-with-debug.sh \
    && echo 'export LD_DEBUG=libs' >> /app/run-with-debug.sh \
    && echo 'export LD_LIBRARY_PATH="/usr/lib/x86_64-linux-gnu:/lib/x86_64-linux-gnu:$LD_LIBRARY_PATH"' >> /app/run-with-debug.sh \
    && echo 'dotnet Chrisalaxelrto.Bot.dll' >> /app/run-with-debug.sh \
    && chmod +x /app/run-with-debug.sh

# Set environment variables
ENV LD_LIBRARY_PATH="/usr/lib/x86_64-linux-gnu:/lib/x86_64-linux-gnu"

USER app
ENTRYPOINT ["/app/run-with-debug.sh"]
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

# Install native dependencies required for audio processing
RUN apt-get update && apt-get install -y \
    libopus0 \
    libopus-dev \
    libsodium23 \
    libsodium-dev \
    ffmpeg \
    && rm -rf /var/lib/apt/lists/* \
    && ldconfig

# Copy the published app first
COPY --from=publish /app/publish .

# Create symlinks in standard library locations that .NET can find
RUN mkdir -p /app/runtimes/linux-x64/native \
    && ln -sf /usr/lib/x86_64-linux-gnu/libopus.so.0 /usr/lib/x86_64-linux-gnu/libopus.so \
    && ln -sf /usr/lib/x86_64-linux-gnu/libopus.so.0 /usr/lib/x86_64-linux-gnu/opus.so \
    && ln -sf /usr/lib/x86_64-linux-gnu/libsodium.so.23 /usr/lib/x86_64-linux-gnu/libsodium.so \
    && ln -sf /usr/lib/x86_64-linux-gnu/libsodium.so.23 /usr/lib/x86_64-linux-gnu/sodium.so \
    && ln -sf /usr/lib/x86_64-linux-gnu/libopus.so.0 /app/runtimes/linux-x64/native/opus.so \
    && ln -sf /usr/lib/x86_64-linux-gnu/libopus.so.0 /app/runtimes/linux-x64/native/libopus.so \
    && ln -sf /usr/lib/x86_64-linux-gnu/libsodium.so.23 /app/runtimes/linux-x64/native/sodium.so \
    && ln -sf /usr/lib/x86_64-linux-gnu/libsodium.so.23 /app/runtimes/linux-x64/native/libsodium.so \
    && chown -R app:app /app/runtimes \
    && ldconfig

# Set multiple environment variables to help with library loading
ENV LD_LIBRARY_PATH="/usr/lib/x86_64-linux-gnu:/lib/x86_64-linux-gnu:/app/runtimes/linux-x64/native"
ENV OPUS_PATH="/usr/lib/x86_64-linux-gnu/libopus.so.0"
ENV LIBSODIUM_PATH="/usr/lib/x86_64-linux-gnu/libsodium.so.23"

USER app
ENTRYPOINT ["dotnet", "Chrisalaxelrto.Bot.dll"]
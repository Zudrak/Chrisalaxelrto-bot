#See https://aka.ms/customizecontainer to learn how to customize your debug container and how Visual Studio uses this Dockerfile to build your images for faster debugging.

FROM mcr.microsoft.com/dotnet/sdk:9.0-alpine AS build
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

FROM alpine:3.20 AS final
WORKDIR /app

# Install dependencies first
RUN apk add --no-cache \
    bash \
    curl \
    icu-libs \
    libgcc \
    libstdc++ \
    opus \
    opus-dev \
    libsodium \
    libsodium-dev \
    ffmpeg \
    krb5-libs \
    libintl \
    openssl \
    zlib

# Install .NET 9 runtime manually
RUN curl -sSL https://dot.net/v1/dotnet-install.sh -o dotnet-install.sh \
    && chmod +x dotnet-install.sh \
    && ./dotnet-install.sh --version latest --runtime dotnet --install-dir /usr/share/dotnet \
    && ./dotnet-install.sh --version latest --runtime aspnetcore --install-dir /usr/share/dotnet \
    && ln -s /usr/share/dotnet/dotnet /usr/bin/dotnet \
    && rm dotnet-install.sh

# Copy the published app first
COPY --from=publish /app/publish .

# Create all the directories and symlinks that .NET is looking for
RUN mkdir -p /app/runtimes/linux-x64/native \
    && mkdir -p /usr/share/dotnet/shared/Microsoft.NETCore.App/9.0.6 \
    && find /usr/lib -name "libopus.so*" -exec echo "Found opus: {}" \; \
    && find /usr/lib -name "libsodium.so*" -exec echo "Found sodium: {}" \; \
    && ln -sf /usr/lib/libopus.so.0 /app/runtimes/linux-x64/native/opus.so \
    && ln -sf /usr/lib/libopus.so.0 /app/runtimes/linux-x64/native/libopus.so \
    && ln -sf /usr/lib/libopus.so.0 /app/opus.so \
    && ln -sf /usr/lib/libopus.so.0 /app/libopus.so \
    && ln -sf /usr/lib/libopus.so.0 /usr/share/dotnet/shared/Microsoft.NETCore.App/9.0.6/opus.so \
    && ln -sf /usr/lib/libopus.so.0 /usr/share/dotnet/shared/Microsoft.NETCore.App/9.0.6/libopus.so \
    && ln -sf /usr/lib/libsodium.so.26 /app/runtimes/linux-x64/native/sodium.so \
    && ln -sf /usr/lib/libsodium.so.26 /app/runtimes/linux-x64/native/libsodium.so \
    && ln -sf /usr/lib/libsodium.so.26 /app/sodium.so \
    && ln -sf /usr/lib/libsodium.so.26 /app/libsodium.so

# Create app user and set permissions
RUN addgroup -g 1001 app && \
    adduser -D -u 1001 -G app app \
    && chown -R app:app /app

# Set library path for Alpine
ENV LD_LIBRARY_PATH="/usr/lib:/lib"

USER app
EXPOSE 8080
EXPOSE 8081

ENTRYPOINT ["dotnet", "Chrisalaxelrto.Bot.dll"]
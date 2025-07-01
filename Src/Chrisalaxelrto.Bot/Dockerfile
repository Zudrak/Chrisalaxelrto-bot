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
    && ldconfig \
    && mkdir -p /app/runtimes/linux-x64/native \
    && cp /usr/lib/x86_64-linux-gnu/libopus.so.0 /app/runtimes/linux-x64/native/opus.so \
    && cp /usr/lib/x86_64-linux-gnu/libopus.so.0 /app/runtimes/linux-x64/native/libopus.so \
    && cp /usr/lib/x86_64-linux-gnu/libsodium.so.23 /app/runtimes/linux-x64/native/sodium.so \
    && cp /usr/lib/x86_64-linux-gnu/libsodium.so.23 /app/runtimes/linux-x64/native/libsodium.so \
    && chmod 755 /app/runtimes/linux-x64/native/*.so \
    && chown -R app:app /app/runtimes
    
COPY --from=publish /app/publish .
USER app
ENTRYPOINT ["dotnet", "Chrisalaxelrto.Bot.dll"]
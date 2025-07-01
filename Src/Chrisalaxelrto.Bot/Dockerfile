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

# Install .NET runtime and native dependencies
RUN apk add --no-cache \
    dotnet9-runtime \
    aspnetcore9-runtime \
    opus \
    opus-dev \
    libsodium \
    libsodium-dev \
    ffmpeg \
    icu-libs \
    libgcc \
    libstdc++

# Create app user
RUN addgroup -g 1001 app && \
    adduser -D -u 1001 -G app app

# Copy the published app
COPY --from=publish /app/publish .
RUN chown -R app:app /app

USER app
EXPOSE 8080
EXPOSE 8081

ENTRYPOINT ["dotnet", "Chrisalaxelrto.Bot.dll"]
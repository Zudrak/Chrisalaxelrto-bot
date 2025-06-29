#See https://aka.ms/customizecontainer to learn how to customize your debug container and how Visual Studio uses this Dockerfile to build your images for faster debugging.

FROM mcr.microsoft.com/dotnet/aspnet:9.0 AS base
USER app
WORKDIR /app
EXPOSE 8080
EXPOSE 8081

FROM mcr.microsoft.com/dotnet/sdk:9.0 AS build
ARG BUILD_CONFIGURATION=Release
WORKDIR /src

COPY ["Src/Chrisalaxelrto.MusicStreamer/", "Chrisalaxelrto.MusicStreamer/"]
COPY ["Src/Chrisalaxelrto.Core/", "Chrisalaxelrto.Core/"]
WORKDIR "/src/Chrisalaxelrto.MusicStreamer"

RUN dotnet restore "./Chrisalaxelrto.MusicStreamer.csproj"
RUN dotnet build "./Chrisalaxelrto.MusicStreamer.csproj" -c $BUILD_CONFIGURATION -o /app/build

RUN curl -fsSL https://tailscale.com/install.sh | sh
ARG TAILSCALE_AUTH_KEY
RUN tailscale up --auth-key=$TAILSCALE_AUTH_KEY

FROM build AS publish
ARG BUILD_CONFIGURATION=Release
RUN dotnet publish "./Chrisalaxelrto.MusicStreamer.csproj" -c $BUILD_CONFIGURATION -o /app/publish /p:UseAppHost=false

FROM base AS final
WORKDIR /app
COPY --from=publish /app/publish .
ENTRYPOINT ["dotnet", "Chrisalaxelrto.MusicStreamer.dll"]
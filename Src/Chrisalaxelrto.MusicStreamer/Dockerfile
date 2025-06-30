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

FROM build AS publish
ARG BUILD_CONFIGURATION=Release
RUN dotnet publish "./Chrisalaxelrto.MusicStreamer.csproj" -c $BUILD_CONFIGURATION -o /app/publish /p:UseAppHost=false

FROM build AS final
USER root
# Declare the build argument and convert to environment variable
ARG TAILSCALE_AUTH_KEY
ENV TAILSCALE_AUTH_KEY=$TAILSCALE_AUTH_KEY
ENV TS_USERSPACE=0

# Install Tailscale
RUN curl -fsSL https://tailscale.com/install.sh | sh

# Create Tailscale directories with proper permissions
RUN mkdir -p /var/run/tailscale && \
    mkdir -p /home/app/.local/share/tailscale

    # Copy startup script
COPY --from=build /src/Chrisalaxelrto.MusicStreamer/start.sh /app/start.sh
RUN chmod +x /app/start.sh

WORKDIR /app
COPY --from=publish /app/publish .
ENTRYPOINT ["/app/start.sh"]
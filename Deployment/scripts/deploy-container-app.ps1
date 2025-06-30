#!/usr/bin/env pwsh

param(
    [Parameter(Mandatory=$true)]
    [string]$ProjectName,
    
    [Parameter(Mandatory=$true)]
    [string]$DockerfilePath,
    
    [string]$Version = "latest",
    [string]$Environment = "prod",
    [string]$ApplicationName = "chrisalaxelrto",
    [string]$ResourceGroup = "",
    [string]$Registry = "",
    [string]$BuildContext = ".",
    [boolean]$UpdateContainerApp = $true
)

# Set defaults based on environment
if ([string]::IsNullOrEmpty($ResourceGroup)) {
    $ResourceGroup = "$ApplicationName-rg-$Environment"
}

if ([string]::IsNullOrEmpty($Registry)) {
    $Registry = "$ApplicationName" + "acr$Environment"
}

$ContainerApp = "$ApplicationName-$ProjectName-app-$Environment"
$ImageName = $ProjectName.ToLower()

Write-Host "=== Container App Deployment ===" -ForegroundColor Cyan
Write-Host "Project: $ProjectName" -ForegroundColor White
Write-Host "Version: $Version" -ForegroundColor White
Write-Host "Environment: $Environment" -ForegroundColor White
Write-Host "Resource Group: $ResourceGroup" -ForegroundColor White
Write-Host "Container App: $ContainerApp" -ForegroundColor White
Write-Host "Registry: $Registry" -ForegroundColor White
Write-Host "Image: ${ImageName}:$Version" -ForegroundColor White
Write-Host "Dockerfile: $DockerfilePath" -ForegroundColor White
Write-Host "Build Context: $BuildContext" -ForegroundColor White
Write-Host "=================================" -ForegroundColor Cyan

# Verify Azure CLI is logged in
Write-Host "Checking Azure CLI login status..." -ForegroundColor Yellow
$loginStatus = az account show --query "user.name" -o tsv 2>$null
if ($LASTEXITCODE -ne 0) {
    Write-Error "Not logged in to Azure CLI. Please run 'az login' first."
    exit 1
}
Write-Host "Logged in as: $loginStatus" -ForegroundColor Green

# Login to Container Registry
Write-Host "Logging into Container Registry..." -ForegroundColor Yellow
az acr login --name $Registry
if ($LASTEXITCODE -ne 0) {
    Write-Error "Failed to login to Container Registry: $Registry"
    exit 1
}

# Build and push image
Write-Host "Building and pushing image..." -ForegroundColor Yellow
$fullImageName = "$ImageName`:$Version"

# Build the image with build arguments
az acr build --registry $Registry --image $fullImageName --file $DockerfilePath $BuildContext


if ($LASTEXITCODE -ne 0) {
    Write-Error "Image build failed"
    exit 1
}

Write-Host "Image built successfully: $Registry.azurecr.io/$fullImageName" -ForegroundColor Green

if ($UpdateContainerApp -eq $false) {
    Write-Host "Skipping container app update as UpdateContainerApp is set to false." -ForegroundColor Yellow
    exit 0
}

# Update container app
Write-Host "Updating container app..." -ForegroundColor Yellow
$fullImagePath = "$Registry.azurecr.io/$fullImageName"
az containerapp update `
    --name $ContainerApp `
    --resource-group $ResourceGroup `
    --image $fullImagePath

if ($LASTEXITCODE -ne 0) {
    Write-Error "Container app update failed"
    exit 1
}

Write-Host "Container app updated successfully!" -ForegroundColor Green

# Show current revisions
Write-Host "Current revisions:" -ForegroundColor Cyan
az containerapp revision list `
    --name $ContainerApp `
    --resource-group $ResourceGroup `
    --query "[].{Name:name, Active:properties.active, CreatedTime:properties.createdTime, Image:properties.template.containers[0].image}" `
    --output table

# Get the app URL if it has ingress enabled
$appUrl = az containerapp show `
    --name $ContainerApp `
    --resource-group $ResourceGroup `
    --query "properties.configuration.ingress.fqdn" `
    --output tsv 2>$null

if ($appUrl -and $appUrl -ne "null") {
    Write-Host "App URL: https://$appUrl" -ForegroundColor Cyan
}

Write-Host "Deployment completed successfully!" -ForegroundColor Green
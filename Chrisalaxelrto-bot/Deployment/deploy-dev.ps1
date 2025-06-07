# Deploy Chrisalaxelrto Discord Bot to Azure - Dev Environment
# This script deploys the bot infrastructure using Azure Bicep

param(
    [Parameter(Mandatory=$true)]
    [string]$SubscriptionId,
    
    [Parameter(Mandatory=$true)]
    [string]$ResourceGroupName,
    
    [Parameter(Mandatory=$true)]
    [SecureString]$DiscordBotToken,
    
    [Parameter(Mandatory=$false)]
    [string]$Location = "East US 2",
    
    [Parameter(Mandatory=$false)]
    [string]$ContainerImage = "mcr.microsoft.com/dotnet/runtime:8.0"
)

# Set error action preference
$ErrorActionPreference = "Stop"

Write-Host "🚀 Starting deployment of Chrisalaxelrto Discord Bot - Dev Environment" -ForegroundColor Green

try {
    # Set Azure subscription
    Write-Host "Setting Azure subscription..." -ForegroundColor Yellow
    az account set --subscription $SubscriptionId

    # Create resource group if it doesn't exist
    Write-Host "Creating resource group if it doesn't exist..." -ForegroundColor Yellow
    az group create --name $ResourceGroupName --location $Location

    # Convert SecureString to plain text for deployment
    $BotTokenPlainText = [System.Runtime.InteropServices.Marshal]::PtrToStringAuto([System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($DiscordBotToken))

    # Deploy the Bicep template
    Write-Host "Deploying Bicep template..." -ForegroundColor Yellow
    $deploymentResult = az deployment group create `
        --resource-group $ResourceGroupName `
        --template-file "main.bicep" `
        --parameters "parameters/main.parameters.dev.simple.json" `
        --parameters discordBotToken=$BotTokenPlainText `
        --parameters containerImage=$ContainerImage `
        --output json | ConvertFrom-Json

    if ($deploymentResult) {
        Write-Host "✅ Deployment completed successfully!" -ForegroundColor Green
        Write-Host "Deployment Details:" -ForegroundColor Cyan
        Write-Host "  Resource Group: $ResourceGroupName" -ForegroundColor White
        Write-Host "  Container App Name: $($deploymentResult.properties.outputs.containerAppName.value)" -ForegroundColor White
        Write-Host "  Key Vault Name: $($deploymentResult.properties.outputs.keyVaultName.value)" -ForegroundColor White
        Write-Host "  Storage Account Name: $($deploymentResult.properties.outputs.storageAccountName.value)" -ForegroundColor White
        
        if ($deploymentResult.properties.outputs.containerAppUrl) {
            Write-Host "  Container App URL: $($deploymentResult.properties.outputs.containerAppUrl.value)" -ForegroundColor White
        }
    }
    else {
        Write-Host "❌ Deployment failed!" -ForegroundColor Red
        exit 1
    }
}
catch {
    Write-Host "❌ Error during deployment: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}
finally {
    # Clear the plain text token from memory
    $BotTokenPlainText = $null
}

Write-Host "🎉 Deployment process completed!" -ForegroundColor Green

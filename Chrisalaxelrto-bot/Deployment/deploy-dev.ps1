# Deploy Chrisalaxelrto Discord Bot to Azure - Dev Environment
# This script deploys the bot infrastructure using Azure CLI and Bicep

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
    # Check if Azure CLI is installed and user is logged in
    Write-Host "Checking Azure CLI status..." -ForegroundColor Yellow
    $azVersion = az version --output json 2>$null | ConvertFrom-Json
    if (-not $azVersion) {
        throw "Azure CLI is not installed or not accessible. Please install Azure CLI first."
    }
    
    $accountInfo = az account show --output json 2>$null | ConvertFrom-Json
    if (-not $accountInfo) {
        Write-Host "You are not logged in to Azure CLI. Please run 'az login' first." -ForegroundColor Red
        throw "Not authenticated with Azure CLI"
    }
    
    Write-Host "✅ Azure CLI version: $($azVersion.'azure-cli')" -ForegroundColor Green
    Write-Host "✅ Current account: $($accountInfo.user.name)" -ForegroundColor Green

    # Set Azure subscription
    Write-Host "Setting Azure subscription..." -ForegroundColor Yellow
    az account set --subscription $SubscriptionId
    
    # Verify subscription was set correctly
    $currentSub = az account show --query "id" --output tsv
    if ($currentSub -ne $SubscriptionId) {
        throw "Failed to set subscription to $SubscriptionId"
    }
    Write-Host "✅ Using subscription: $(az account show --query "name" --output tsv)" -ForegroundColor Green

    # Create resource group if it doesn't exist
    Write-Host "Creating resource group if it doesn't exist..." -ForegroundColor Yellow
    $rgResult = az group create --name $ResourceGroupName --location $Location --output json | ConvertFrom-Json
    if ($rgResult.properties.provisioningState -eq "Succeeded") {
        Write-Host "✅ Resource group '$ResourceGroupName' is ready" -ForegroundColor Green
    }    # Convert SecureString to plain text for deployment
    $BotTokenPlainText = [System.Runtime.InteropServices.Marshal]::PtrToStringAuto([System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($DiscordBotToken))

    # Deploy the Bicep template
    Write-Host "Deploying Bicep template..." -ForegroundColor Yellow
    Write-Host "  📁 Template: main.bicep" -ForegroundColor Cyan
    Write-Host "  📄 Parameters: parameters/main.parameters.dev.json" -ForegroundColor Cyan
    Write-Host "  🎯 Resource Group: $ResourceGroupName" -ForegroundColor Cyan
    
    $deploymentName = "chrisalaxelrto-bot-deployment-$(Get-Date -Format 'yyyyMMdd-HHmmss')"
    $deploymentResult = az deployment group create `
        --resource-group $ResourceGroupName `
        --name $deploymentName `
        --template-file "main.bicep" `
        --parameters "parameters/main.parameters.dev.json" `
        --parameters discordBotToken=$BotTokenPlainText `
        --parameters containerImage=$ContainerImage `
        --output json | ConvertFrom-Json

    if ($deploymentResult -and $deploymentResult.properties.provisioningState -eq "Succeeded") {        Write-Host "✅ Deployment completed successfully!" -ForegroundColor Green
        Write-Host "📋 Deployment Details:" -ForegroundColor Cyan
        Write-Host "  🏷️  Deployment Name: $deploymentName" -ForegroundColor White
        Write-Host "  📦 Resource Group: $ResourceGroupName" -ForegroundColor White
        Write-Host "  🌐 Location: $Location" -ForegroundColor White
        
        # Display output values if available
        if ($deploymentResult.properties.outputs) {
            Write-Host "📊 Deployed Resources:" -ForegroundColor Cyan
            if ($deploymentResult.properties.outputs.containerAppName) {
                Write-Host "  🐳 Container App: $($deploymentResult.properties.outputs.containerAppName.value)" -ForegroundColor White
            }
            if ($deploymentResult.properties.outputs.keyVaultName) {
                Write-Host "  🔐 Key Vault: $($deploymentResult.properties.outputs.keyVaultName.value)" -ForegroundColor White
            }
            if ($deploymentResult.properties.outputs.storageAccountName) {
                Write-Host "  💾 Storage Account: $($deploymentResult.properties.outputs.storageAccountName.value)" -ForegroundColor White
            }
        }
        
        # Get deployment URL for monitoring
        $portalUrl = "https://portal.azure.com/#@/resource/subscriptions/$SubscriptionId/resourceGroups/$ResourceGroupName/deployments"
        Write-Host "🔗 Monitor deployment: $portalUrl" -ForegroundColor Blue
    }
    else {
        Write-Host "❌ Deployment failed!" -ForegroundColor Red
        Write-Host "Check the Azure portal for detailed error information." -ForegroundColor Yellow
        exit 1
    }
}
catch {
    Write-Host "❌ Error during deployment: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "💡 Troubleshooting tips:" -ForegroundColor Yellow
    Write-Host "  • Ensure you have the necessary permissions for the subscription" -ForegroundColor White
    Write-Host "  • Check that the resource group name is valid and unique" -ForegroundColor White
    Write-Host "  • Verify the Discord bot token is correct" -ForegroundColor White
    Write-Host "  • Check Azure service limits in your region" -ForegroundColor White
    exit 1
}
finally {
    # Clear the plain text token from memory for security
    if ($BotTokenPlainText) {
        $BotTokenPlainText = $null
        [System.GC]::Collect()
    }
}

Write-Host "🎉 Deployment process completed!" -ForegroundColor Green
Write-Host "📝 Next steps:" -ForegroundColor Cyan
Write-Host "  1. Verify the bot is running in the Container App" -ForegroundColor White
Write-Host "  2. Check logs for any startup issues" -ForegroundColor White
Write-Host "  3. Test Discord bot functionality" -ForegroundColor White

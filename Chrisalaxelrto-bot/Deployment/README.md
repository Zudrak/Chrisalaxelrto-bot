# Chrisalaxelrto Discord Bot - Azure Deployment

This folder contains Azure Bicep templates for deploying the Chrisalaxelrto Discord Bot infrastructure to Azure.

## Architecture

The deployment creates the following Azure resources:

- **Azure Container App**: Hosts the .NET 8 Discord bot application
- **Azure Container Apps Environment**: Provides the hosting environment for the container app
- **Azure Key Vault**: Stores sensitive configuration like Discord bot tokens
- **Azure Storage Account**: Provides blob storage for logs, data, and backups
- **Log Analytics Workspace**: Collects logs and metrics from the container app

## Security & Permissions

The Container App is assigned a system-managed identity with the following permissions:

- **Key Vault Secrets User**: Read access to secrets in the Key Vault
- **Storage Blob Data Contributor**: Read/write access to storage account blobs
- **Storage Account Contributor**: Management access to the storage account

## File Structure

```
Deployment/
├── main.bicep                           # Main orchestration template
├── modules/
│   ├── container-app.bicep             # Container App deployment
│   ├── key-vault.bicep                 # Key Vault deployment
│   └── storage-account.bicep           # Storage Account deployment
├── parameters/
│   ├── main.parameters.dev.json        # Dev environment parameters (with Key Vault reference)
│   └── main.parameters.dev.simple.json # Dev environment parameters (simple)
├── deploy-dev.ps1                      # PowerShell deployment script
└── README.md                           # This file
```

## Prerequisites

1. **Azure CLI**: Install and configure Azure CLI
2. **Azure Subscription**: Active Azure subscription with appropriate permissions
3. **Discord Bot Token**: Discord application bot token

## Deployment Options

### Option 1: Using PowerShell Script (Recommended)

```powershell
# Navigate to the Deployment folder
cd Deployment

# Run the deployment script
.\deploy-dev.ps1 -SubscriptionId "your-subscription-id" -ResourceGroupName "chrisalaxelrto-bot-dev-rg" -DiscordBotToken (Read-Host -AsSecureString "Enter Discord Bot Token")
```

### Option 2: Using Azure CLI Directly

```bash
# Create resource group
az group create --name "chrisalaxelrto-bot-dev-rg" --location "East US 2"

# Deploy using simple parameters (you'll be prompted for the Discord bot token)
az deployment group create \
  --resource-group "chrisalaxelrto-bot-dev-rg" \
  --template-file "main.bicep" \
  --parameters "@parameters/main.parameters.dev.simple.json" \
  --parameters discordBotToken="your-discord-bot-token"
```

### Option 3: Using Existing Key Vault Reference

If you already have a Key Vault with the Discord bot token:

1. Update the `main.parameters.dev.json` file with your Key Vault details
2. Deploy using:

```bash
az deployment group create \
  --resource-group "chrisalaxelrto-bot-dev-rg" \
  --template-file "main.bicep" \
  --parameters "@parameters/main.parameters.dev.json"
```

## Environment Variables

The Container App will be configured with the following environment variables:

- `ASPNETCORE_ENVIRONMENT`: Set to the environment name (dev, test, prod)
- `DISCORD_BOT_TOKEN`: Discord bot token (from Key Vault)
- `AZURE_STORAGE_ACCOUNT_NAME`: Name of the created storage account
- `AZURE_KEY_VAULT_NAME`: Name of the created Key Vault

## Storage Account Containers

The following blob containers are created:

- `logs`: For application logs
- `data`: For application data
- `backups`: For backup files

## Monitoring

- **Log Analytics**: Container app logs are sent to Log Analytics workspace
- **Azure Monitor**: Built-in monitoring for all Azure resources
- **Application Insights**: Can be added for detailed application telemetry

## Scaling

The Container App is configured with:

- **Dev Environment**: 1-2 replicas
- **CPU-based scaling**: Scales when CPU utilization exceeds 70%
- **Resource limits**: 0.25 CPU cores, 0.5 GiB memory per replica

## Customization

### Parameters

You can customize the deployment by modifying the parameters files:

- `environmentName`: dev, test, or prod
- `location`: Azure region
- `applicationName`: Base name for resources
- `containerImage`: Docker image to deploy
- `minReplicas`/`maxReplicas`: Scaling configuration

### Adding New Environments

1. Copy `main.parameters.dev.simple.json` to `main.parameters.{env}.json`
2. Update the parameters for your environment
3. Create a deployment script similar to `deploy-dev.ps1`

## Security Considerations

- All secrets are stored in Azure Key Vault
- Storage account disables public blob access
- TLS 1.2 minimum for all communications
- RBAC-based access control
- System-assigned managed identities (no stored credentials)

## Troubleshooting

### Common Issues

1. **Deployment fails with permissions error**: Ensure you have Contributor role on the subscription
2. **Container app fails to start**: Check that the Discord bot token is valid
3. **Key Vault access denied**: Verify the managed identity role assignments

### Useful Commands

```bash
# Check deployment status
az deployment group show --resource-group "chrisalaxelrto-bot-dev-rg" --name "main"

# View container app logs
az containerapp logs show --name "chrisalaxelrto-bot-dev-app" --resource-group "chrisalaxelrto-bot-dev-rg"

# List all resources in the resource group
az resource list --resource-group "chrisalaxelrto-bot-dev-rg" --output table
```

## Cost Optimization

- **Dev Environment**: Uses Standard_LRS storage and minimal Container App resources
- **Auto-scaling**: Scales down to 1 replica when not in use
- **Log retention**: Set to 30 days to control costs

## Next Steps

1. Deploy the infrastructure using one of the methods above
2. Build and push your Docker image to a container registry
3. Update the Container App with your custom image
4. Configure any additional environment variables needed by your bot

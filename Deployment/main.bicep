@description('Main deployment template for Chrisalaxelrto Discord Bot')

// Parameters
@description('The environment name (dev, test, prod)')
@allowed(['dev', 'test', 'prod'])
param environmentName string = 'dev'

@description('The Azure region for resource deployment')
param location string = resourceGroup().location

@description('The name of the application')
param applicationName string = 'chrisalaxelrto-bot'

@description('The Docker image for the container app')
param containerImage string = 'mcr.microsoft.com/dotnet/runtime:8.0'

@description('Discord bot token (will be stored in Key Vault)')
@secure()
param discordBotToken string

@description('The minimum number of replicas for the container app')
@minValue(0)
@maxValue(10)
param minReplicas int = 1

@description('The maximum number of replicas for the container app')
@minValue(1)
@maxValue(30)
param maxReplicas int = 3

// Variables
var containerAppName = '${applicationName}-app-${environmentName}'
var containerEnvironmentName = '${applicationName}-env-${environmentName}'
var keyVaultName = take('${applicationName}-kv-${environmentName}-${uniqueString(resourceGroup().id)}', 24)
var storageAccountName = take(replace('${applicationName}storage${environmentName}${uniqueString(resourceGroup().id)}', '-', ''), 24)
var logAnalyticsWorkspaceName = '${applicationName}-logs-${environmentName}'

// Common tags
var commonTags = {
  Environment: environmentName
  Application: applicationName
  ManagedBy: 'Bicep'
}

// Log Analytics Workspace for Container Apps
resource logAnalyticsWorkspace 'Microsoft.OperationalInsights/workspaces@2023-09-01' = {
  name: logAnalyticsWorkspaceName
  location: location
  tags: commonTags
  properties: {
    sku: {
      name: 'PerGB2018'
    }
    retentionInDays: 30
    features: {
      enableLogAccessUsingOnlyResourcePermissions: true
    }
  }
}

// Container Apps Environment
resource containerAppsEnvironment 'Microsoft.App/managedEnvironments@2024-03-01' = {
  name: containerEnvironmentName
  location: location
  tags: commonTags
  properties: {
    appLogsConfiguration: {
      destination: 'log-analytics'
      logAnalyticsConfiguration: {
        customerId: logAnalyticsWorkspace.properties.customerId
        sharedKey: logAnalyticsWorkspace.listKeys().primarySharedKey
      }
    }
  }
}

// Deploy Key Vault
module keyVault 'modules/key-vault.bicep' = {
  name: 'keyVault-deployment'
  params: {
    keyVaultName: keyVaultName
    location: location
    tags: commonTags
    discordBotToken: discordBotToken
  }
}

// Deploy Storage Account
module storageAccount 'modules/storage-account.bicep' = {
  name: 'storageAccount-deployment'
  params: {
    storageAccountName: storageAccountName
    location: location
    tags: commonTags
    environmentName: environmentName
  }
}

// Deploy Container App
module containerApp 'modules/container-app.bicep' = {
  name: 'containerApp-deployment'
  params: {
    containerAppName: containerAppName
    location: location
    tags: commonTags
    containerAppsEnvironmentId: containerAppsEnvironment.id
    containerImage: containerImage
    keyVaultName: keyVault.outputs.keyVaultName
    storageAccountName: storageAccount.outputs.storageAccountName
    minReplicas: minReplicas
    maxReplicas: maxReplicas
    environmentName: environmentName
  }
  dependsOn: [
    keyVault
    storageAccount
  ]
}

// Outputs
output resourceGroupName string = resourceGroup().name
output containerAppName string = containerApp.outputs.containerAppName
output containerAppUrl string = containerApp.outputs.containerAppUrl
output keyVaultName string = keyVault.outputs.keyVaultName
output storageAccountName string = storageAccount.outputs.storageAccountName
output containerAppPrincipalId string = containerApp.outputs.principalId

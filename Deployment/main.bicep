@description('Main deployment template for Chrisalaxelrto Discord Bot')

@allowed(['dev', 'test', 'prod'])
param environmentName string = 'dev'

@description('The Azure region for resource deployment')
param location string = resourceGroup().location

@description('The name of the application')
param applicationName string = 'chrisalaxelrto'

@description('The minimum number of replicas for the container app')
@minValue(0)
@maxValue(10)
param botMinReplicas int = 0

@description('The maximum number of replicas for the container app')
@minValue(1)
@maxValue(30)
param botMaxReplicas int = 1

@description('The minimum number of replicas for the music streamer container app')
@minValue(0)
@maxValue(10)
param musicStreamerMinReplicas int = 0

@description('The maximum number of replicas for the music streamer container app')
@minValue(1)
@maxValue(30)
param musicStreamerMaxReplicas int = 1

@description('The Discord bot token for the application')
@secure()
param discordBotToken string

@description('The Discord bot dev token for the application')
@secure()
param discordBotTokenDev string

@description('The address prefix for the Virtual Network')
param vnetAddressPrefix string = '10.0.0.0/16'

@description('The address prefix for the Container Apps subnet')
param containerAppsSubnetAddressPrefix string = '10.0.2.0/23'

@description('The address prefix for the private endpoints subnet')
param privateEndpointsSubnetAddressPrefix string = '10.0.6.0/23'

@description('The SKU for the Container Registry')
@allowed(['Basic', 'Standard', 'Premium'])
param containerRegistrySku string = 'Basic'

@description('Enable private endpoint for Container Registry (requires Premium SKU)')
param enableContainerRegistryPrivateEndpoint bool = false

// Variables
var containerAppName = '${applicationName}-bot-app-${environmentName}'
var musicStreamerContainerAppName = '${applicationName}-music-app-${environmentName}'
var containerEnvironmentName = '${applicationName}-env-${environmentName}'
var keyVaultName = '${applicationName}-kv-${environmentName}'
var storageAccountName = '${applicationName}sa${environmentName}'
var logAnalyticsWorkspaceName = '${applicationName}-logs-${environmentName}'
var vnetName = '${applicationName}-vnet-${environmentName}'
var containerRegistryName = '${applicationName}acr${environmentName}'
var appManagedIdentityName = '${applicationName}-mi-${environmentName}'

// Common tags
var commonTags = {
  Environment: environmentName
  Application: applicationName
  ManagedBy: 'Bicep'
}

// Virtual Network for private networking
module virtualNetwork 'modules/virtual-network.bicep' = {
  name: 'virtualNetwork-deployment'
  params: {
    vnetName: vnetName
    location: location
    tags: commonTags
    vnetAddressPrefix: vnetAddressPrefix
    containerAppsSubnetAddressPrefix: containerAppsSubnetAddressPrefix
    privateEndpointsSubnetAddressPrefix: privateEndpointsSubnetAddressPrefix
  }
}

// Container Registry for Docker images
module containerRegistry 'modules/container-registry.bicep' = {
  name: 'containerRegistry-deployment'
  params: {
    containerRegistryName: containerRegistryName
    location: location
    tags: commonTags
    sku: containerRegistrySku
    publicNetworkAccess: !enableContainerRegistryPrivateEndpoint
    privateEndpointsSubnetId: enableContainerRegistryPrivateEndpoint ? virtualNetwork.outputs.privateEndpointsSubnetId : ''
    enablePrivateEndpoint: enableContainerRegistryPrivateEndpoint
  }
}

// Log Analytics Workspace for Container Apps (cheapest tier)
resource logAnalyticsWorkspace 'Microsoft.OperationalInsights/workspaces@2023-09-01' = {
  name: logAnalyticsWorkspaceName
  location: location
  tags: commonTags
  properties: {
    sku: {
      name: 'PerGB2018' // Pay-per-GB is most cost-effective for small usage
    }
    features: {
      enableLogAccessUsingOnlyResourcePermissions: true
    }
    workspaceCapping: {
      dailyQuotaGb: 1 // Limit daily ingestion to 1GB to control costs
    }
  }
}

resource containerAppsEnvironment 'Microsoft.App/managedEnvironments@2025-01-01' = {
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
    vnetConfiguration: {
      infrastructureSubnetId: virtualNetwork.outputs.containerAppsSubnetId
      internal: true
    }
  }
}

module keyVault 'modules/key-vault.bicep' = {
  name: 'keyVault-deployment'
  params: {
    keyVaultName: keyVaultName
    location: location
    tags: commonTags
    discordBotToken: discordBotToken
    discordBotTokenDev: discordBotTokenDev
  }
}

module storageAccount 'modules/storage-account.bicep' = {
  name: 'storageAccount-deployment'
  params: {
    storageAccountName: storageAccountName
    location: location
    tags: commonTags
  }
}

module appManagedIdentity 'modules/managed-identity.bicep' = {
  name: 'appManagedIdentity-deployment'
  params: {
    appManagedIdentityName: appManagedIdentityName
    location: location
    keyVaultName: keyVault.outputs.keyVaultName
    storageAccountName: storageAccount.outputs.storageAccountName
    containerRegistryName: containerRegistry.outputs.containerRegistryName
    tags: commonTags
  }
}

module botContainerApp 'modules/container-app.bicep' = {
  name: 'botContainerApp-deployment'
  params: {
    containerAppName: containerAppName
    location: location
    tags: commonTags
    containerAppsEnvironmentId: containerAppsEnvironment.id
    minReplicas: botMinReplicas
    maxReplicas: botMaxReplicas
    environmentName: environmentName
    enableIngress: true
    targetPort: 0
    enableExternalIngress: true
    containerRegistryName: containerRegistry.outputs.containerRegistryName
    appManagedIdentityName: appManagedIdentity.outputs.managedIdentityName
  }
}

module musicStreamerContainerApp 'modules/container-app.bicep' = {
  name: 'musicStreamerContainerApp-deployment'
  params: {
    containerAppName: musicStreamerContainerAppName
    location: location
    tags: commonTags
    containerAppsEnvironmentId: containerAppsEnvironment.id
    minReplicas: musicStreamerMinReplicas
    maxReplicas: musicStreamerMaxReplicas
    environmentName: environmentName
    enableIngress: true
    targetPort: 8080
    enableExternalIngress: true  // Internal only - accessible only within VNet
    containerRegistryName: containerRegistry.outputs.containerRegistryName
    appManagedIdentityName: appManagedIdentity.outputs.managedIdentityName
  }
}

// Outputs
output resourceGroupName string = resourceGroup().name
output containerAppName string = botContainerApp.outputs.containerAppName
output containerAppUrl string = botContainerApp.outputs.containerAppUrl
output keyVaultName string = keyVault.outputs.keyVaultName
output storageAccountName string = storageAccount.outputs.storageAccountName
output musicStreamerUrl string = musicStreamerContainerApp.outputs.containerAppUrl
output vnetId string = virtualNetwork.outputs.vnetId
output vnetName string = virtualNetwork.outputs.vnetName
output containerAppsSubnetId string = virtualNetwork.outputs.containerAppsSubnetId
output privateEndpointsSubnetId string = virtualNetwork.outputs.privateEndpointsSubnetId
output musicStreamerInternalFqdn string = musicStreamerContainerApp.outputs.fqdn
output containerRegistryName string = containerRegistry.outputs.containerRegistryName
output containerRegistryLoginServer string = containerRegistry.outputs.loginServer
output containerRegistryId string = containerRegistry.outputs.containerRegistryId

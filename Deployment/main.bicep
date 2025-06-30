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
param botMinReplicas int = 1

@description('The maximum number of replicas for the container app')
@minValue(1)
@maxValue(30)
param botMaxReplicas int = 1

@description('The Discord bot token for the application')
@secure()
param discordBotToken string

@description('The Discord bot dev token for the application')
@secure()
param discordBotTokenDev string

@description('The youtube cookies for the application')
@secure()
param youtubeCookies string

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

@description('Resource group name of the existing VM')
param existingVmResourceGroup string = ''

@description('Name of the existing VNet where the VM is located')
param existingVnetName string = ''

// Variables
var containerAppName = '${applicationName}-bot-app-${environmentName}'
var containerEnvironmentName = '${applicationName}-env-${environmentName}'
var keyVaultName = '${applicationName}-kv-${environmentName}'
var storageAccountName = '${applicationName}sa${environmentName}'
var logAnalyticsWorkspaceName = '${applicationName}-logs-${environmentName}'
var vnetName = '${applicationName}-vnet-${environmentName}'
var containerRegistryName = '${applicationName}acr${environmentName}'
var appManagedIdentityName = '${applicationName}-mi-${environmentName}'
var networkInterfaceName = '${applicationName}-vm-nic-${environmentName}'

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

// Application Insights for telemetry and monitoring
resource applicationInsights 'Microsoft.Insights/components@2020-02-02' = {
  name: '${applicationName}-ai-${environmentName}'
  location: location
  tags: commonTags
  kind: 'web'
  properties: {
    Application_Type: 'web'
    WorkspaceResourceId: logAnalyticsWorkspace.id
    IngestionMode: 'LogAnalytics'
    publicNetworkAccessForIngestion: 'Enabled'
    publicNetworkAccessForQuery: 'Enabled'
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
      internal: false
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
    youtubeCookies: youtubeCookies
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
    enableIngress: true
    targetPort: 0
    enableExternalIngress: true
    containerRegistryName: containerRegistry.outputs.containerRegistryName
    appManagedIdentityName: appManagedIdentity.outputs.managedIdentityName
    environmentVariables: {
      ASPNETCORE_ENVIRONMENT: environmentName
      ConnectionStrings__ApplicationInsights: applicationInsights.properties.ConnectionString
      ManagedIdentityClientId: appManagedIdentity.outputs.clientId
    }
  }
}

// Reference an existing VNet in another resource group
resource existingVNet 'Microsoft.Network/virtualNetworks@2023-09-01' existing = {
  name: existingVnetName
  scope: resourceGroup(existingVmResourceGroup)
}

// Create VNet Peering if enabled
module vnetPeeringToExisting 'modules/vnet-peering.bicep' = {
  name: 'vnet-peering-to-existing-deployment'
  params: {
    peeringName: '${vnetName}-to-${existingVnetName}'
    localVNetId: virtualNetwork.outputs.vnetId
    remoteVNetId: existingVNet.id
    allowForwardedTraffic: true
    allowVirtualNetworkAccess: true
  }
}

module vnetPeeringFromExisting 'modules/vnet-peering.bicep'= {
  name: 'vnet-peering-from-existing-deployment'
  scope: resourceGroup(existingVmResourceGroup)
  params: {
    peeringName: '${existingVnetName}-to-${vnetName}'
    localVNetId: existingVNet.id
    remoteVNetId: virtualNetwork.outputs.vnetId
    allowForwardedTraffic: true
    allowVirtualNetworkAccess: true
  }
}


// Outputs
output resourceGroupName string = resourceGroup().name
output containerAppName string = botContainerApp.outputs.containerAppName
output containerAppUrl string = botContainerApp.outputs.containerAppUrl
output keyVaultName string = keyVault.outputs.keyVaultName
output storageAccountName string = storageAccount.outputs.storageAccountName
output vnetId string = virtualNetwork.outputs.vnetId
output vnetName string = virtualNetwork.outputs.vnetName
output containerAppsSubnetId string = virtualNetwork.outputs.containerAppsSubnetId
output privateEndpointsSubnetId string = virtualNetwork.outputs.privateEndpointsSubnetId
output containerRegistryName string = containerRegistry.outputs.containerRegistryName
output containerRegistryLoginServer string = containerRegistry.outputs.loginServer
output containerRegistryId string = containerRegistry.outputs.containerRegistryId
output applicationInsightsInstrumentationKey string = applicationInsights.properties.InstrumentationKey

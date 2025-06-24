@description('The name of the Container Registry')
param containerRegistryName string

@description('The Azure region for resource deployment')
param location string = resourceGroup().location

@description('Resource tags')
param tags object = {}

@description('The SKU for the Container Registry')
@allowed(['Basic', 'Standard', 'Premium'])
param sku string = 'Basic'

@description('Enable admin user for the Container Registry')
param adminUserEnabled bool = true

@description('Enable public network access')
param publicNetworkAccess bool = true

@description('The resource ID of the private endpoints subnet (required for Premium SKU with private endpoints)')
param privateEndpointsSubnetId string = ''

@description('Enable private endpoint for the Container Registry')
param enablePrivateEndpoint bool = false

// Container Registry
resource containerRegistry 'Microsoft.ContainerRegistry/registries@2023-07-01' = {
  name: containerRegistryName
  location: location
  tags: tags
  sku: {
    name: sku
  }
  properties: {
    adminUserEnabled: adminUserEnabled
    publicNetworkAccess: publicNetworkAccess ? 'Enabled' : 'Disabled'
    networkRuleBypassOptions: 'AzureServices'
    policies: {
      retentionPolicy: {
        status: 'enabled'
        days: 7 // Keep images for 7 days to reduce storage costs
      }
      trustPolicy: {
        status: 'disabled'
      }
    }
  }
}

// Private Endpoint (only for Premium SKU)
resource privateEndpoint 'Microsoft.Network/privateEndpoints@2023-09-01' = if (enablePrivateEndpoint && sku == 'Premium' && !empty(privateEndpointsSubnetId)) {
  name: '${containerRegistryName}-pe'
  location: location
  tags: tags
  properties: {
    subnet: {
      id: privateEndpointsSubnetId
    }
    privateLinkServiceConnections: [
      {
        name: '${containerRegistryName}-pe-connection'
        properties: {
          privateLinkServiceId: containerRegistry.id
          groupIds: [
            'registry'
          ]
        }
      }
    ]
  }
}

// Private DNS Zone (only if private endpoint is enabled)
resource privateDnsZone 'Microsoft.Network/privateDnsZones@2020-06-01' = if (enablePrivateEndpoint && sku == 'Premium' && !empty(privateEndpointsSubnetId)) {
  name: 'privatelink.azurecr.io'
  location: 'global'
  tags: tags
}

// Private DNS Zone Group (only if private endpoint is enabled)
resource privateDnsZoneGroup 'Microsoft.Network/privateEndpoints/privateDnsZoneGroups@2023-09-01' = if (enablePrivateEndpoint && sku == 'Premium' && !empty(privateEndpointsSubnetId)) {
  name: 'default'
  parent: privateEndpoint
  properties: {
    privateDnsZoneConfigs: [
      {
        name: 'privatelink-azurecr-io'
        properties: {
          privateDnsZoneId: privateDnsZone.id
        }
      }
    ]
  }
}

// Outputs
output containerRegistryId string = containerRegistry.id
output containerRegistryName string = containerRegistry.name
output loginServer string = containerRegistry.properties.loginServer
output adminUsername string = containerRegistry.properties.adminUserEnabled ? containerRegistry.name : ''
output adminPassword string = containerRegistry.properties.adminUserEnabled ? containerRegistry.listCredentials().passwords[0].value : ''
output resourceId string = containerRegistry.id

// Parameters
@description('The name of the Key Vault')
param keyVaultName string

@description('The Azure region for resource deployment')
param location string = resourceGroup().location

@description('Resource tags')
param tags object = {}

@description('Discord bot token to store in Key Vault')
@secure()
param discordBotToken string

@description('Discord bot token for dev to store in Key Vault')
@secure()
param discordBotTokenDev string

@description('YouTube cookies to store in Key Vault')
@secure()
param youtubeCookies string

@description('The tenant ID for Key Vault access policies')
param tenantId string = subscription().tenantId

// Key Vault
resource keyVault 'Microsoft.KeyVault/vaults@2023-07-01' = {
  name: keyVaultName
  location: location
  tags: tags
  properties: {
    tenantId: tenantId
    sku: {
      family: 'A'
      name: 'standard'
    }
    enabledForDeployment: false
    enabledForDiskEncryption: true
    enabledForTemplateDeployment: true
    enableSoftDelete: true
    softDeleteRetentionInDays: 7
    enablePurgeProtection: true
    enableRbacAuthorization: true
    publicNetworkAccess: 'Enabled'
    accessPolicies: []
  }
}

// Store Discord bot token as a secret
resource discordBotTokenSecret 'Microsoft.KeyVault/vaults/secrets@2024-11-01' = {
  parent: keyVault
  name: 'discord-bot-token'
  properties: {
    value: discordBotToken
    contentType: 'text/plain'
    attributes: {
      enabled: true
    }
  }
}

resource discordBotTokenDevSecret 'Microsoft.KeyVault/vaults/secrets@2024-11-01' = {
  parent: keyVault
  name: 'discord-bot-token-dev'
  properties: {
    value: discordBotTokenDev
    contentType: 'text/plain'
    attributes: {
      enabled: true
    }
  }
}

resource youtubeCookiesSecret 'Microsoft.KeyVault/vaults/secrets@2024-11-01' = {
  parent: keyVault
  name: 'youtube-cookies'
  properties: {
    value: youtubeCookies
    contentType: 'text/plain'
    attributes: {
      enabled: true
    }
  }
}

// Outputs
output keyVaultName string = keyVault.name
output keyVaultId string = keyVault.id
output keyVaultUri string = keyVault.properties.vaultUri

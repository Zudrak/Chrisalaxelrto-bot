// Parameters
@description('The name of the Container App')
param containerAppName string

@description('The Azure region for resource deployment')
param location string = resourceGroup().location

@description('Resource tags')
param tags object = {}

@description('The resource ID of the Container Apps Environment')
param containerAppsEnvironmentId string

@description('The Docker image for the container')
param containerImage string

@description('The name of the Key Vault')
param keyVaultName string

@description('The name of the Storage Account')
param storageAccountName string

@description('The minimum number of replicas')
param minReplicas int = 1

@description('The maximum number of replicas')
param maxReplicas int = 3

@description('The environment name')
param environmentName string

// Container App
resource containerApp 'Microsoft.App/containerApps@2024-03-01' = {
  name: containerAppName
  location: location
  tags: tags
  identity: {
    type: 'SystemAssigned'
  }
  properties: {
    managedEnvironmentId: containerAppsEnvironmentId
    configuration: {
      activeRevisionsMode: 'Single'
      secrets: [
        {
          name: 'discord-bot-token'
          keyVaultUrl: 'https://${keyVaultName}${environment().suffixes.keyvaultDns}/secrets/discord-bot-token'
          identity: 'system'
        }
      ]
      registries: []
    }
    template: {
      containers: [
        {
          name: 'chrisalaxelrto-bot'
          image: containerImage
          env: [
            {
              name: 'ASPNETCORE_ENVIRONMENT'
              value: environmentName
            }
            {
              name: 'DISCORD_BOT_TOKEN'
              secretRef: 'discord-bot-token'
            }
            {
              name: 'AZURE_STORAGE_ACCOUNT_NAME'
              value: storageAccountName
            }
            {
              name: 'AZURE_KEY_VAULT_NAME'
              value: keyVaultName
            }
          ]
          resources: {
            cpu: json('0.25')
            memory: '0.5Gi'
          }
          probes: []
        }
      ]
      scale: {
        minReplicas: minReplicas
        maxReplicas: maxReplicas
        rules: [
          {
            name: 'cpu-scaling'
            custom: {
              type: 'cpu'
              metadata: {
                type: 'Utilization'
                value: '70'
              }
            }
          }
        ]
      }
    }
  }
}

// Get reference to existing Key Vault
resource keyVault 'Microsoft.KeyVault/vaults@2023-07-01' existing = {
  name: keyVaultName
}

// Get reference to existing Storage Account
resource storageAccount 'Microsoft.Storage/storageAccounts@2023-01-01' existing = {
  name: storageAccountName
}

// Role assignments for Container App managed identity

// Key Vault Secrets User role
resource keyVaultSecretsUserRoleAssignment 'Microsoft.Authorization/roleAssignments@2022-04-01' = {
  name: guid(keyVault.id, containerApp.id, '4633458b-17de-408a-b874-0445c86b69e6')
  scope: keyVault
  properties: {
    roleDefinitionId: subscriptionResourceId('Microsoft.Authorization/roleDefinitions', '4633458b-17de-408a-b874-0445c86b69e6') // Key Vault Secrets User
    principalId: containerApp.identity.principalId
    principalType: 'ServicePrincipal'
  }
}

// Storage Blob Data Contributor role
resource storageBlobDataContributorRoleAssignment 'Microsoft.Authorization/roleAssignments@2022-04-01' = {
  name: guid(storageAccount.id, containerApp.id, 'ba92f5b4-2d11-453d-a403-e96b0029c9fe')
  scope: storageAccount
  properties: {
    roleDefinitionId: subscriptionResourceId('Microsoft.Authorization/roleDefinitions', 'ba92f5b4-2d11-453d-a403-e96b0029c9fe') // Storage Blob Data Contributor
    principalId: containerApp.identity.principalId
    principalType: 'ServicePrincipal'
  }
}

// Storage Account Contributor role (for managing storage account operations)
resource storageAccountContributorRoleAssignment 'Microsoft.Authorization/roleAssignments@2022-04-01' = {
  name: guid(storageAccount.id, containerApp.id, '17d1049b-9a84-46fb-8f53-869881c3d3ab')
  scope: storageAccount
  properties: {
    roleDefinitionId: subscriptionResourceId('Microsoft.Authorization/roleDefinitions', '17d1049b-9a84-46fb-8f53-869881c3d3ab') // Storage Account Contributor
    principalId: containerApp.identity.principalId
    principalType: 'ServicePrincipal'
  }
}

// Outputs
output containerAppName string = containerApp.name
output containerAppId string = containerApp.id
output containerAppUrl string = '' // No ingress for Discord bot
output principalId string = containerApp.identity.principalId

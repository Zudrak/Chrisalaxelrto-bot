// Parameters
@description('The name of the Container App')
param containerAppName string

@description('The Azure region for resource deployment')
param location string = resourceGroup().location

@description('Resource tags')
param tags object = {}

@description('The resource ID of the Container Apps Environment')
param containerAppsEnvironmentId string

@description('The minimum number of replicas')
param minReplicas int = 0

@description('The maximum number of replicas')
param maxReplicas int = 1

@description('Enable ingress for the container app')
param enableIngress bool = false

@description('Target port for ingress (required if enableIngress is true)')
param targetPort int = 8080

@description('Enable external ingress (true for external, false for internal only)')
param enableExternalIngress bool = false

@description('The name of the Container Registry')
param containerRegistryName string

@description('The name of the Container Registry')
param appManagedIdentityName string

@description('Environment variables for the container app')
param environmentVariables object = {}

// Get reference to existing Container Registry
resource containerRegistry 'Microsoft.ContainerRegistry/registries@2023-07-01' existing = {
  name: containerRegistryName
}

resource appManagedIdentity 'Microsoft.ManagedIdentity/userAssignedIdentities@2023-01-31' existing = {
  name: appManagedIdentityName
}

// Container App
resource containerApp 'Microsoft.App/containerApps@2024-03-01' = {
  name: containerAppName
  location: location
  tags: tags
  identity: {
    type: 'UserAssigned'
    userAssignedIdentities: {
      '${appManagedIdentity.id}': {}
    }
  }
  properties: {
    managedEnvironmentId: containerAppsEnvironmentId
    configuration: {
      activeRevisionsMode: 'Single'
      ingress: enableIngress ? {
        external: enableExternalIngress
        targetPort: targetPort
        allowInsecure: false
        traffic: [
          {
            weight: 100
            latestRevision: true
          }
        ]
      } : null
      registries: [
        {
          server: containerRegistry.properties.loginServer
          identity: appManagedIdentity.id
        }
      ]
    }
    template: {
      containers: [
        {
          name: containerAppName
          image: 'chrisalaxelrtoacrprod.azurecr.io/samples/hello-world:latest'
          env: [for envVar in items(environmentVariables): {
            name: envVar.key
            value: envVar.value
          }]
          resources: {
            // Minimum resources for cost optimization
            cpu: json('0.25')
            memory: '0.5Gi'
          }
        }
      ]
      scale: {
        minReplicas: minReplicas
        maxReplicas: maxReplicas
      }
    }
  }
}

// Outputs
output containerAppName string = containerApp.name
output containerAppId string = containerApp.id
output containerAppUrl string = enableIngress ? 'https://${containerApp.properties.configuration.ingress.fqdn}' : ''
output fqdn string = enableIngress ? containerApp.properties.configuration.ingress.fqdn : ''

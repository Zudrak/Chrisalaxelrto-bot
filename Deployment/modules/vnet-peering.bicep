@description('Name of the VNet peering connection')
param peeringName string

@description('ID of the local VNet')
param localVNetId string

@description('ID of the remote VNet')
param remoteVNetId string

@description('Allow forwarded traffic in the peering')
param allowForwardedTraffic bool = true

@description('Allow gateway transit in the peering')
param allowGatewayTransit bool = false

@description('Allow virtual network access in the peering')
param allowVirtualNetworkAccess bool = true

@description('Use remote gateways in the peering')
param useRemoteGateways bool = false

// VNet Peering resource
resource vnetPeering 'Microsoft.Network/virtualNetworks/virtualNetworkPeerings@2023-09-01' = {
  name: peeringName
  properties: {
    allowForwardedTraffic: allowForwardedTraffic
    allowGatewayTransit: allowGatewayTransit
    allowVirtualNetworkAccess: allowVirtualNetworkAccess
    useRemoteGateways: useRemoteGateways
    remoteVirtualNetwork: {
      id: remoteVNetId
    }
  }
  parent: existingLocalVNet
}

// Reference to existing local VNet
resource existingLocalVNet 'Microsoft.Network/virtualNetworks@2023-09-01' existing = {
  name: split(localVNetId, '/')[8]
}

// Output
output peeringId string = vnetPeering.id
output peeringName string = vnetPeering.name

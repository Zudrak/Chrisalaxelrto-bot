@description('The name of the Network Interface Card')
param nicName string

@description('The Azure region for resource deployment')
param location string = resourceGroup().location

@description('Resource tags')
param tags object = {}

@description('The subnet ID to attach the NIC to')
param subnetId string

@description('Private IP address allocation method')
@allowed(['Dynamic', 'Static'])
param privateIpAllocationMethod string = 'Dynamic'

@description('Static private IP address (if allocation method is Static)')
param privateIpAddress string = ''

// Network Interface Card
resource networkInterface 'Microsoft.Network/networkInterfaces@2023-09-01' = {
  name: nicName
  location: location
  tags: tags
  properties: {
    ipConfigurations: [
      {
        name: 'ipconfig1'
        properties: {
          subnet: {
            id: subnetId
          }
          privateIPAllocationMethod: privateIpAllocationMethod
          privateIPAddress: privateIpAllocationMethod == 'Static' ? privateIpAddress : null
        }
      }
    ]
    enableAcceleratedNetworking: false
    enableIPForwarding: false
    dnsSettings: {
      dnsServers: []
    }
  }
}

// Outputs
output nicId string = networkInterface.id
output nicName string = networkInterface.name
output privateIpAddress string = networkInterface.properties.ipConfigurations[0].properties.privateIPAddress

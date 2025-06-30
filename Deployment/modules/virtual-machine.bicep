@description('The name of the Virtual Machine')
param vmName string

@description('The Azure region for resource deployment')
param location string = resourceGroup().location

@description('Resource tags')
param tags object = {}

@description('The Subnet ID where the VM will be deployed')
param subnetId string

@description('The CIDR range of the subnet (for NSG rule)')
param subnetAddressPrefix string

@description('The size of the VM')
param vmSize string = 'Standard_B1ls'

@description('The admin username for the VM')
param adminUsername string

@description('The SSH public key for authentication')
@secure()
param sshPublicKey string

@description('The port for music streaming service')
param musicStreamingPort int = 8080

// Network Security Group for VM
resource networkSecurityGroup 'Microsoft.Network/networkSecurityGroups@2023-09-01' = {
  name: '${vmName}-nsg'
  location: location
  tags: tags
  properties: {
    securityRules: [
      {
        name: 'AllowSSH'
        properties: {
          priority: 100
          direction: 'Inbound'
          access: 'Allow'
          protocol: 'Tcp'
          sourcePortRange: '*'
          destinationPortRange: '22'
          sourceAddressPrefix: '*'
          destinationAddressPrefix: '*'
          description: 'Allow SSH'
        }
      }
      {
        name: 'Allow${musicStreamingPort}FromSubnet'
        properties: {
          priority: 110
          direction: 'Inbound'
          access: 'Allow'
          protocol: 'Tcp'
          sourcePortRange: '*'
          destinationPortRange: '${musicStreamingPort}'
          sourceAddressPrefix: subnetAddressPrefix
          destinationAddressPrefix: '*'
          description: 'Allow port ${musicStreamingPort} connections from within the subnet'
        }
      }
      {
        name: 'DenyAllInbound'
        properties: {
          priority: 4096
          direction: 'Inbound'
          access: 'Deny'
          protocol: '*'
          sourcePortRange: '*'
          destinationPortRange: '*'
          sourceAddressPrefix: '*'
          destinationAddressPrefix: '*'
          description: 'Deny all other inbound traffic'
        }
      }
    ]
  }
}

// Public IP Address for the VM
resource publicIPAddress 'Microsoft.Network/publicIPAddresses@2023-09-01' = {
  name: '${vmName}-pip'
  location: location
  tags: tags
  properties: {
    publicIPAllocationMethod: 'Dynamic'
    dnsSettings: {
      domainNameLabel: toLower('${vmName}-${uniqueString(resourceGroup().id)}')
    }
  }
}

// Network interface for the VM
resource networkInterface 'Microsoft.Network/networkInterfaces@2023-09-01' = {
  name: '${vmName}-nic'
  location: location
  tags: tags
  properties: {
    networkSecurityGroup: {
      id: networkSecurityGroup.id
    }
    ipConfigurations: [
      {
        name: 'ipconfig1'
        properties: {
          subnet: {
            id: subnetId
          }
          privateIPAllocationMethod: 'Dynamic'
          publicIPAddress: {
            id: publicIPAddress.id
          }
        }
      }
    ]
  }
}

// Virtual Machine
resource virtualMachine 'Microsoft.Compute/virtualMachines@2023-09-01' = {
  name: vmName
  location: location
  tags: tags
  properties: {
    hardwareProfile: {
      vmSize: vmSize
    }
    osProfile: {
      computerName: vmName
      adminUsername: adminUsername
      linuxConfiguration: {
        disablePasswordAuthentication: true
        ssh: {
          publicKeys: [
            {
              path: '/home/${adminUsername}/.ssh/authorized_keys'
              keyData: sshPublicKey
            }
          ]
        }
        provisionVMAgent: true
      }
    }
    storageProfile: {
      imageReference: {
        publisher: 'Canonical'
        offer: 'UbuntuServer'
        sku: '22_04-lts-minimal'
        version: 'latest'
      }
      osDisk: {
        createOption: 'FromImage'
        caching: 'ReadWrite'
        managedDisk: {
          storageAccountType: 'Standard_LRS' // Cheapest storage option
        }
        diskSizeGB: 30 // Minimum required size
      }
    }
    networkProfile: {
      networkInterfaces: [
        {
          id: networkInterface.id
        }
      ]
    }
    priority: 'Spot' // Use spot instances for additional cost savings
    evictionPolicy: 'Deallocate'
    billingProfile: {
      maxPrice: -1 // -1 means not to evict based on price
    }
  }
}

resource vmExtension 'Microsoft.Compute/virtualMachines/extensions@2023-09-01' = {
  parent: virtualMachine
  name: 'ConfigureNetworking'
  location: location
  properties: {
    publisher: 'Microsoft.Azure.Extensions'
    type: 'CustomScript'
    typeHandlerVersion: '2.1'
    autoUpgradeMinorVersion: true
    settings: {
      skipDos2Unix: false
      fileUris: []
    }
    protectedSettings: {
      commandToExecute: 'sudo apt-get update && sudo apt-get install -y ufw && sudo ufw allow 22/tcp && sudo ufw allow from ${subnetAddressPrefix} to any port ${musicStreamingPort} && sudo ufw enable'
    }
  }
}
// Outputs
output vmId string = virtualMachine.id
output vmName string = virtualMachine.name
output privateIPAddress string = networkInterface.properties.ipConfigurations[0].properties.privateIPAddress
output publicIPAddress string = publicIPAddress.properties.ipAddress
output fqdn string = publicIPAddress.properties.dnsSettings.fqdn
output musicStreamerUrl string = 'http://${networkInterface.properties.ipConfigurations[0].properties.privateIPAddress}:${musicStreamingPort}'

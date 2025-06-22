# GitHub Actions Deployment Guide

This repository contains GitHub Actions workflows for deploying the Chrisalaxelrto Discord Bot infrastructure to Azure using Bicep templates.

## Workflow Structure

### Reusable Workflow
- **`.github/workflows/deploy-infrastructure.yml`** - The main reusable workflow that handles Azure deployment

### Environment-Specific Workflows
- **`.github/workflows/deploy-dev.yml`** - Deploys to development environment
- **`.github/workflows/deploy-prod.yml`** - Deploys to production environment

## Prerequisites

### 1. Azure Service Principal Setup

Create an Azure Service Principal with the necessary permissions:

```bash
# Create a service principal
az ad sp create-for-rbac --name "chrisalaxelrto-bot-github" --role Contributor --scopes /subscriptions/{subscription-id}
```

This will output values you'll need for GitHub secrets:
- `appId` (use as AZURE_CLIENT_ID)
- `password` (use as AZURE_CLIENT_SECRET if using password-based auth)
- `tenant` (use as AZURE_TENANT_ID)

### 2. Configure Federated Identity (Recommended)

For enhanced security, configure OpenID Connect (OIDC) federation instead of using passwords:

```bash
# Get your GitHub repository information
GITHUB_REPO="your-username/your-repo-name"
SUBSCRIPTION_ID="your-subscription-id"
APP_ID="your-app-id-from-step-1"

# Create federated credentials for main branch (prod)
az ad app federated-credential create \
  --id $APP_ID \
  --parameters '{
    "name": "github-prod",
    "issuer": "https://token.actions.githubusercontent.com",
    "subject": "repo:'$GITHUB_REPO':ref:refs/heads/main",
    "audiences": ["api://AzureADTokenExchange"]
  }'
```

## GitHub Repository Setup

### 1. Repository Secrets

Add the following secrets to your GitHub repository (Settings → Secrets and variables → Actions):

#### Required for Azure Authentication:
- `AZURE_CLIENT_ID` - Your Azure Service Principal Client ID
- `AZURE_TENANT_ID` - Your Azure Tenant ID  
- `AZURE_SUBSCRIPTION_ID` - Your Azure Subscription ID

#### Required for Application:
- `DISCORD_BOT_TOKEN_DEV` - Discord bot token for development
- `DISCORD_BOT_TOKEN_PROD` - Discord bot token for production

### 2. Environment Protection (Optional but Recommended)

Configure GitHub environments for additional security:

1. Go to Settings → Environments
2. Create environments: `dev` and `prod`
3. For `prod` environment, configure:
   - Required reviewers
   - Deployment branches (restrict to `main` branch)
   - Environment secrets (if you want environment-specific values)

## Deployment Triggers

### Automatic Deployments

#### Development Environment
Automatically deploys when:
- Pushing to `develop` branch
- Pushing to any `feature/*` branch
- Changes made to `Deployment/**` or `Chrisalaxelrto-bot/**` directories

#### Production Environment  
Automatically deploys when:
- Pushing to `main` branch
- Changes made to `Deployment/**` or `Chrisalaxelrto-bot/**` directories

### Manual Deployments

Both environments support manual deployment via workflow_dispatch:

1. Go to Actions tab in GitHub
2. Select "Deploy to Development" or "Deploy to Production"
3. Click "Run workflow"
4. Optionally specify a custom container image

## Environment Configuration

The workflows use environment-specific parameter files:

- **Development**: `Deployment/parameters/main.parameters.dev.json`
- **Production**: `Deployment/parameters/main.parameters.prod.json`

### Adding New Environments

To add a new environment (e.g., `test`):

1. Create parameter file: `Deployment/parameters/main.parameters.test.json`
2. Create workflow file: `.github/workflows/deploy-test.yml`
3. Follow the same pattern as existing environment workflows
4. Add environment-specific secrets if needed

## Workflow Features

### Resource Management
- Automatically creates Azure Resource Groups
- Uses consistent naming conventions
- Applies appropriate tags

### Security
- Uses Azure OIDC for secure authentication
- Stores secrets in GitHub Secrets
- Discord bot tokens are environment-specific

### Monitoring
- Validates parameter files before deployment
- Provides deployment summaries
- Shows resource URLs and names in workflow output

### Customization
- Supports custom container images via workflow inputs
- Environment-specific scaling settings
- Configurable Azure regions

## Troubleshooting

### Common Issues

1. **Authentication Failures**
   - Verify Azure Service Principal has correct permissions
   - Check that federated credentials are configured correctly
   - Ensure GitHub secrets are set properly

2. **Parameter File Errors**
   - Verify parameter files exist for the target environment
   - Check JSON syntax in parameter files
   - Ensure required parameters are provided

3. **Resource Naming Conflicts**
   - Azure resource names must be globally unique
   - The template uses `uniqueString()` for resource naming
   - Check for existing resources with same names

### Debugging

Enable verbose logging by:
1. Re-running failed workflow
2. Check the "Deploy Bicep template" step for detailed error messages
3. Review Azure Activity Log in the portal

## Security Best Practices

1. **Use Environment Protection**: Configure required reviewers for production
2. **Rotate Secrets Regularly**: Update Discord bot tokens and Azure credentials periodically
3. **Principle of Least Privilege**: Ensure Service Principal has minimum required permissions
4. **Monitor Deployments**: Review deployment logs and Azure resource changes

## Cost Management

- Development environment uses minimal replicas (1-2)
- Production environment scales appropriately (2-10)
- Log Analytics retention set to 30 days
- Consider using Azure Cost Management alerts

#!/bin/bash
# Script de deploy automatizado para Consent Manager
# Uso: ./deploy.sh <environment>
# Ejemplo: ./deploy.sh dev

set -e

ENV=${1:-dev}
REGION="sa-east-1"
VALID_ENVS=("dev" "sandbox" "prod")

# Validar ambiente
if [[ ! " ${VALID_ENVS[@]} " =~ " ${ENV} " ]]; then
    echo "❌ Invalid environment: $ENV"
    echo "Valid environments: ${VALID_ENVS[*]}"
    exit 1
fi

echo "================================================"
echo "  Consent Manager - Deploy Infrastructure"
echo "  Environment: $ENV"
echo "  Region: $REGION"
echo "================================================"
echo ""

# Confirmar en prod
if [ "$ENV" == "prod" ]; then
    echo "⚠️  WARNING: You are deploying to PRODUCTION"
    echo "Press Enter to continue or Ctrl+C to cancel..."
    read
fi

cd "$(dirname "$0")/../environments/$ENV"

# Init
echo "📦 Step 1/4: Initializing Terraform..."
terraform init -upgrade

# Validate
echo "✅ Step 2/4: Validating configuration..."
terraform validate

# Plan
echo "📋 Step 3/4: Creating execution plan..."
terraform plan -out=plan.out

# Confirm
echo ""
echo "Review the plan above."
echo "Press Enter to apply or Ctrl+C to cancel..."
read

# Apply
echo "🚀 Step 4/4: Applying infrastructure..."
terraform apply plan.out

# Cleanup plan file
rm -f plan.out

echo ""
echo "================================================"
echo "  ✅ Infrastructure deployed successfully!"
echo "================================================"
echo ""
echo "Next steps:"
echo "  1. Configure kubectl:"
echo "     $(terraform output -raw kubeconfig_command)"
echo ""
echo "  2. Deploy microservices:"
echo "     helm install consent-manager ../../helm/consent-manager -n consent-manager-$ENV"
echo ""
echo "  3. Verify:"
echo "     kubectl get pods -n consent-manager-$ENV"

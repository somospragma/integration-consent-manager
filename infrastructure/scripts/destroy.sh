#!/bin/bash
# Script para destruir infraestructura
# Uso: ./destroy.sh <environment>
# NUNCA ejecutar en prod sin aprobación explícita

set -e

ENV=${1:-dev}
REGION="sa-east-1"

echo "================================================"
echo "  ⚠️  Consent Manager - DESTROY Infrastructure"
echo "  Environment: $ENV"
echo "  Region: $REGION"
echo "================================================"
echo ""

# Bloquear destrucción de prod sin flag explícito
if [ "$ENV" == "prod" ]; then
    if [ "$2" != "--confirm-prod-destroy" ]; then
        echo "❌ BLOCKED: Cannot destroy production without explicit confirmation."
        echo ""
        echo "If you REALLY want to destroy production, run:"
        echo "  ./destroy.sh prod --confirm-prod-destroy"
        echo ""
        echo "This action is IRREVERSIBLE and will cause DOWNTIME."
        exit 1
    fi
    echo "⚠️  DESTROYING PRODUCTION INFRASTRUCTURE"
    echo "You have 10 seconds to cancel (Ctrl+C)..."
    sleep 10
fi

cd "$(dirname "$0")/../environments/$ENV"

echo "🗑️  Destroying infrastructure for $ENV..."
terraform init
terraform destroy -auto-approve

echo ""
echo "✅ Infrastructure destroyed for $ENV"

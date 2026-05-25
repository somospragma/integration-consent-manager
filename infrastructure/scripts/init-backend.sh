#!/bin/bash
# Script para crear el backend de Terraform (S3 + DynamoDB)
# Ejecutar solo la primera vez

set -e

REGION="sa-east-1"
BUCKET_NAME="pragma-consent-manager-tfstate"
DYNAMO_TABLE="pragma-consent-manager-tflocks"
ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)

echo "================================================"
echo "  Consent Manager - Terraform Backend Setup"
echo "================================================"
echo "Region: $REGION"
echo "Account: $ACCOUNT_ID"
echo "Bucket: $BUCKET_NAME"
echo "DynamoDB: $DYNAMO_TABLE"
echo ""

# Crear S3 bucket para state
echo "📦 Creating S3 bucket for Terraform state..."
aws s3api create-bucket \
  --bucket "$BUCKET_NAME" \
  --region "$REGION" \
  --create-bucket-configuration LocationConstraint="$REGION" \
  2>/dev/null || echo "Bucket already exists"

# Habilitar versionado
aws s3api put-bucket-versioning \
  --bucket "$BUCKET_NAME" \
  --versioning-configuration Status=Enabled

# Habilitar cifrado
aws s3api put-bucket-encryption \
  --bucket "$BUCKET_NAME" \
  --server-side-encryption-configuration '{
    "Rules": [{"ApplyServerSideEncryptionByDefault": {"SSEAlgorithm": "aws:kms"}}]
  }'

# Bloquear acceso público
aws s3api put-public-access-block \
  --bucket "$BUCKET_NAME" \
  --public-access-block-configuration '{
    "BlockPublicAcls": true,
    "IgnorePublicAcls": true,
    "BlockPublicPolicy": true,
    "RestrictPublicBuckets": true
  }'

# Crear DynamoDB table para locking
echo "🔒 Creating DynamoDB table for state locking..."
aws dynamodb create-table \
  --table-name "$DYNAMO_TABLE" \
  --attribute-definitions AttributeName=LockID,AttributeType=S \
  --key-schema AttributeName=LockID,KeyType=HASH \
  --billing-mode PAY_PER_REQUEST \
  --region "$REGION" \
  2>/dev/null || echo "Table already exists"

echo ""
echo "✅ Backend created successfully!"
echo ""
echo "Add this to your Terraform backend config:"
echo ""
echo '  backend "s3" {'
echo "    bucket         = \"$BUCKET_NAME\""
echo '    key            = "<env>/terraform.tfstate"'
echo "    region         = \"$REGION\""
echo "    dynamodb_table = \"$DYNAMO_TABLE\""
echo '    encrypt        = true'
echo '  }'

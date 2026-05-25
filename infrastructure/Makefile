# Makefile para gestión de infraestructura Consent Manager
# Uso: make plan ENV=dev | make apply ENV=prod | make destroy ENV=sandbox

ENV ?= dev
REGION ?= sa-east-1
TF_DIR = environments/$(ENV)

.PHONY: init plan apply destroy output fmt validate

## Inicializar Terraform
init:
	@echo "🔧 Initializing Terraform for $(ENV)..."
	cd $(TF_DIR) && terraform init -upgrade

## Validar configuración
validate:
	@echo "✅ Validating Terraform for $(ENV)..."
	cd $(TF_DIR) && terraform validate

## Formatear archivos
fmt:
	@echo "📝 Formatting Terraform files..."
	terraform fmt -recursive .

## Plan de ejecución
plan: init validate
	@echo "📋 Planning infrastructure for $(ENV)..."
	cd $(TF_DIR) && terraform plan -out=plan.out

## Aplicar cambios
apply:
	@echo "🚀 Applying infrastructure for $(ENV)..."
	cd $(TF_DIR) && terraform apply plan.out
	@echo "✅ Infrastructure deployed for $(ENV)"

## Destruir ambiente
destroy:
	@echo "⚠️  Destroying infrastructure for $(ENV)..."
	@echo "Press Ctrl+C to cancel, Enter to continue..."
	@read
	cd $(TF_DIR) && terraform destroy -auto-approve
	@echo "🗑️  Infrastructure destroyed for $(ENV)"

## Mostrar outputs
output:
	@cd $(TF_DIR) && terraform output

## Inicializar backend S3 (solo primera vez)
init-backend:
	@echo "🪣 Creating Terraform backend..."
	chmod +x scripts/init-backend.sh
	./scripts/init-backend.sh

## Mostrar costos estimados (requiere infracost)
cost:
	@cd $(TF_DIR) && infracost breakdown --path .

## Help
help:
	@echo "Consent Manager Infrastructure"
	@echo ""
	@echo "Usage: make <target> ENV=<environment>"
	@echo ""
	@echo "Environments: dev, sandbox, prod"
	@echo ""
	@echo "Targets:"
	@echo "  init          Initialize Terraform"
	@echo "  plan          Create execution plan"
	@echo "  apply         Apply infrastructure changes"
	@echo "  destroy       Destroy infrastructure"
	@echo "  output        Show Terraform outputs"
	@echo "  validate      Validate configuration"
	@echo "  fmt           Format Terraform files"
	@echo "  init-backend  Create S3 backend (first time only)"
	@echo "  cost          Estimate costs (requires infracost)"

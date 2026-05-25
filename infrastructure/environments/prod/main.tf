###############################################################################
# Environment: Production
# Consent Manager — HA con costo controlado (~$650/mes)
#
# Estrategia:
# - Aurora Serverless v2 con read replica (escala automático)
# - ElastiCache Serverless (límites más altos)
# - MSK Serverless
# - EKS con On-Demand (estabilidad) + autoscaling
# - Multi-AZ NAT Gateway (resiliencia)
# - Deletion protection en todo
###############################################################################

terraform {
  required_version = ">= 1.7.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.50"
    }
  }

  backend "s3" {
    bucket         = "pragma-consent-manager-tfstate"
    key            = "prod/terraform.tfstate"
    region         = "sa-east-1"
    dynamodb_table = "pragma-consent-manager-tflocks"
    encrypt        = true
  }
}

provider "aws" {
  region = var.region

  default_tags {
    tags = {
      Environment = "prod"
      Project     = "consent-manager-pragma"
      Team        = "integration-chapter"
      ManagedBy   = "terraform"
      Compliance  = "open-finance-co"
    }
  }
}

variable "region" {
  type    = string
  default = "sa-east-1"
}

variable "alert_email" {
  type    = string
  default = "oncall-integration@pragma.com.co"
}

locals {
  environment  = "prod"
  cluster_name = "consent-manager-prod"
}

###############################################################################
# Networking (Multi-AZ, NAT redundante)
###############################################################################

module "networking" {
  source = "../../modules/networking"

  environment        = local.environment
  region             = var.region
  vpc_cidr           = "10.20.0.0/16"
  availability_zones = ["sa-east-1a", "sa-east-1b", "sa-east-1c"]
  single_nat_gateway = false # Multi-AZ NAT para resiliencia
}

###############################################################################
# EKS Cluster (On-Demand, más nodos)
###############################################################################

module "eks" {
  source = "../../modules/eks-cluster"

  environment         = local.environment
  cluster_name        = local.cluster_name
  vpc_id              = module.networking.vpc_id
  private_subnet_ids  = module.networking.private_subnet_ids
  kubernetes_version  = "1.30"
  node_instance_types = ["m6i.large", "m6a.large"]
  node_min_size       = 3
  node_max_size       = 10
  node_desired_size   = 3
  use_spot_instances  = false # On-Demand para estabilidad en prod
}

###############################################################################
# Database (Aurora Serverless v2 — HA con read replica)
###############################################################################

module "database" {
  source = "../../modules/database"

  environment            = local.environment
  cluster_name           = "consent-manager"
  vpc_id                 = module.networking.vpc_id
  database_subnet_ids    = module.networking.database_subnet_ids
  min_capacity           = 1    # Mínimo más alto para prod
  max_capacity           = 16   # Escala hasta 16 ACU bajo carga
  database_name          = "consent_manager"
  deletion_protection    = true
  backup_retention_days  = 30
}

###############################################################################
# Cache (ElastiCache Serverless — límites más altos)
###############################################################################

module "cache" {
  source = "../../modules/cache"

  environment        = local.environment
  cache_name         = "consent-manager"
  vpc_id             = module.networking.vpc_id
  private_subnet_ids = module.networking.private_subnet_ids
}

###############################################################################
# Messaging (MSK Serverless)
###############################################################################

module "messaging" {
  source = "../../modules/messaging"

  environment        = local.environment
  cluster_name       = "consent-manager"
  vpc_id             = module.networking.vpc_id
  private_subnet_ids = module.networking.private_subnet_ids
}

###############################################################################
# Secrets
###############################################################################

module "secrets" {
  source = "../../modules/secrets"

  environment  = local.environment
  project_name = "consent-manager"
}

###############################################################################
# Observability (con alertas)
###############################################################################

module "observability" {
  source = "../../modules/observability"

  environment  = local.environment
  cluster_name = local.cluster_name
  alert_email  = var.alert_email
}

###############################################################################
# Outputs
###############################################################################

output "vpc_id" {
  value = module.networking.vpc_id
}

output "eks_cluster_name" {
  value = module.eks.cluster_name
}

output "database_endpoint" {
  value     = module.database.endpoint
  sensitive = true
}

output "database_reader_endpoint" {
  value     = module.database.reader_endpoint
  sensitive = true
}

output "cache_endpoint" {
  value     = module.cache.endpoint
  sensitive = true
}

output "kubeconfig_command" {
  value = "aws eks update-kubeconfig --name ${local.cluster_name} --region ${var.region}"
}

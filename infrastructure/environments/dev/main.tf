###############################################################################
# Environment: Development
# Consent Manager — Costo mínimo (~$180/mes)
#
# Estrategia:
# - Aurora Serverless v2 (0.5 ACU mínimo)
# - ElastiCache Serverless (paga por uso)
# - MSK Serverless (paga por throughput)
# - EKS con Spot instances (60-90% ahorro)
# - Single NAT Gateway (ahorro $45/mes vs multi-AZ)
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
    key            = "dev/terraform.tfstate"
    region         = "sa-east-1"
    dynamodb_table = "pragma-consent-manager-tflocks"
    encrypt        = true
  }
}

provider "aws" {
  region = var.region

  default_tags {
    tags = {
      Environment = "dev"
      Project     = "consent-manager-pragma"
      Team        = "integration-chapter"
      ManagedBy   = "terraform"
    }
  }
}

variable "region" {
  type    = string
  default = "sa-east-1"
}

locals {
  environment  = "dev"
  cluster_name = "consent-manager-dev"
}

###############################################################################
# Networking
###############################################################################

module "networking" {
  source = "../../modules/networking"

  environment        = local.environment
  region             = var.region
  vpc_cidr           = "10.10.0.0/16"
  availability_zones = ["sa-east-1a", "sa-east-1b"]
  single_nat_gateway = true # Ahorro: 1 NAT en vez de 2
}

###############################################################################
# EKS Cluster
###############################################################################

module "eks" {
  source = "../../modules/eks-cluster"

  environment         = local.environment
  cluster_name        = local.cluster_name
  vpc_id              = module.networking.vpc_id
  private_subnet_ids  = module.networking.private_subnet_ids
  kubernetes_version  = "1.30"
  node_instance_types = ["t3.medium", "t3a.medium"] # Múltiples para Spot
  node_min_size       = 1
  node_max_size       = 4
  node_desired_size   = 2
  use_spot_instances  = true # 60-90% ahorro
}

###############################################################################
# Database (Aurora Serverless v2)
###############################################################################

module "database" {
  source = "../../modules/database"

  environment            = local.environment
  cluster_name           = "consent-manager"
  vpc_id                 = module.networking.vpc_id
  database_subnet_ids    = module.networking.database_subnet_ids
  min_capacity           = 0.5  # Mínimo posible (~$45/mes idle)
  max_capacity           = 2    # Suficiente para dev
  database_name          = "consent_manager"
  deletion_protection    = false
  backup_retention_days  = 3
}

###############################################################################
# Cache (ElastiCache Serverless)
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
# Observability
###############################################################################

module "observability" {
  source = "../../modules/observability"

  environment  = local.environment
  cluster_name = local.cluster_name
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

output "eks_cluster_endpoint" {
  value     = module.eks.cluster_endpoint
  sensitive = true
}

output "database_endpoint" {
  value     = module.database.endpoint
  sensitive = true
}

output "cache_endpoint" {
  value     = module.cache.endpoint
  sensitive = true
}

output "kubeconfig_command" {
  value = "aws eks update-kubeconfig --name ${local.cluster_name} --region ${var.region}"
}

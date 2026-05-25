###############################################################################
# Module: Database
# Aurora Serverless v2 PostgreSQL — Costo-eficiente
# - Escala de 0.5 ACU a 16 ACU automáticamente
# - En dev con poco tráfico: ~$45/mes
# - En prod con carga: escala sin intervención
###############################################################################

variable "environment" {
  type = string
}

variable "cluster_name" {
  type    = string
  default = "consent-manager"
}

variable "vpc_id" {
  type = string
}

variable "database_subnet_ids" {
  type = list(string)
}

variable "allowed_security_group_ids" {
  description = "Security groups allowed to connect to DB"
  type        = list(string)
  default     = []
}

variable "min_capacity" {
  description = "Minimum ACUs (0.5 = mínimo posible)"
  type        = number
  default     = 0.5
}

variable "max_capacity" {
  description = "Maximum ACUs"
  type        = number
  default     = 4
}

variable "database_name" {
  type    = string
  default = "consent_manager"
}

variable "deletion_protection" {
  type    = bool
  default = false
}

variable "backup_retention_days" {
  type    = number
  default = 7
}

locals {
  tags = {
    Environment = var.environment
    Project     = "consent-manager-pragma"
    ManagedBy   = "terraform"
  }
}

# DB Subnet Group
resource "aws_db_subnet_group" "main" {
  name       = "${var.cluster_name}-${var.environment}"
  subnet_ids = var.database_subnet_ids
  tags       = local.tags
}

# Security Group for Aurora
resource "aws_security_group" "aurora" {
  name_prefix = "${var.cluster_name}-aurora-"
  vpc_id      = var.vpc_id
  description = "Security group for Aurora Serverless v2"

  ingress {
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = var.allowed_security_group_ids
    description     = "PostgreSQL from EKS nodes"
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = local.tags
}

# Aurora Serverless v2 Cluster
resource "aws_rds_cluster" "main" {
  cluster_identifier = "${var.cluster_name}-${var.environment}"
  engine             = "aurora-postgresql"
  engine_mode        = "provisioned"
  engine_version     = "16.4"
  database_name      = var.database_name
  master_username    = "consent_admin"

  manage_master_user_password = true # AWS manages password in Secrets Manager

  db_subnet_group_name   = aws_db_subnet_group.main.name
  vpc_security_group_ids = [aws_security_group.aurora.id]

  serverlessv2_scaling_configuration {
    min_capacity = var.min_capacity
    max_capacity = var.max_capacity
  }

  storage_encrypted       = true
  deletion_protection     = var.deletion_protection
  backup_retention_period = var.backup_retention_days
  skip_final_snapshot     = var.environment != "prod"

  tags = local.tags
}

# Aurora Serverless v2 Instance
resource "aws_rds_cluster_instance" "main" {
  cluster_identifier = aws_rds_cluster.main.id
  instance_class     = "db.serverless"
  engine             = aws_rds_cluster.main.engine
  engine_version     = aws_rds_cluster.main.engine_version

  tags = local.tags
}

# Read replica solo en prod
resource "aws_rds_cluster_instance" "reader" {
  count              = var.environment == "prod" ? 1 : 0
  cluster_identifier = aws_rds_cluster.main.id
  instance_class     = "db.serverless"
  engine             = aws_rds_cluster.main.engine
  engine_version     = aws_rds_cluster.main.engine_version

  tags = merge(local.tags, { Role = "reader" })
}

# Outputs
output "endpoint" {
  value = aws_rds_cluster.main.endpoint
}

output "reader_endpoint" {
  value = aws_rds_cluster.main.reader_endpoint
}

output "port" {
  value = aws_rds_cluster.main.port
}

output "database_name" {
  value = aws_rds_cluster.main.database_name
}

output "security_group_id" {
  value = aws_security_group.aurora.id
}

###############################################################################
# Module: Messaging
# MSK Serverless (Kafka) — Costo-eficiente
# - Paga por uso (throughput + storage)
# - Sin brokers que administrar
# - En dev: ~$15-30/mes vs $200+/mes con cluster fijo
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

variable "private_subnet_ids" {
  type = list(string)
}

variable "allowed_security_group_ids" {
  type    = list(string)
  default = []
}

locals {
  tags = {
    Environment = var.environment
    Project     = "consent-manager-pragma"
    ManagedBy   = "terraform"
  }
}

# Security Group
resource "aws_security_group" "msk" {
  name_prefix = "${var.cluster_name}-msk-"
  vpc_id      = var.vpc_id
  description = "Security group for MSK Serverless"

  ingress {
    from_port       = 9098
    to_port         = 9098
    protocol        = "tcp"
    security_groups = var.allowed_security_group_ids
    description     = "Kafka from EKS nodes"
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = local.tags
}

# MSK Serverless Cluster
resource "aws_msk_serverless_cluster" "main" {
  cluster_name = "${var.cluster_name}-${var.environment}"

  vpc_config {
    subnet_ids         = var.private_subnet_ids
    security_group_ids = [aws_security_group.msk.id]
  }

  client_authentication {
    sasl {
      iam {
        enabled = true
      }
    }
  }

  tags = local.tags
}

# Outputs
output "cluster_arn" {
  value = aws_msk_serverless_cluster.main.arn
}

output "bootstrap_brokers" {
  value = aws_msk_serverless_cluster.main.cluster_uuid
}

output "security_group_id" {
  value = aws_security_group.msk.id
}

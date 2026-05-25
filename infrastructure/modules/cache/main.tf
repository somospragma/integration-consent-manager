###############################################################################
# Module: Cache
# ElastiCache Serverless Redis — Costo-eficiente
# - Paga por uso (GB almacenado + ECPUs)
# - En dev: ~$5-10/mes vs $70/mes con nodo fijo
# - Escala automáticamente sin intervención
###############################################################################

variable "environment" {
  type = string
}

variable "cache_name" {
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
resource "aws_security_group" "redis" {
  name_prefix = "${var.cache_name}-redis-"
  vpc_id      = var.vpc_id
  description = "Security group for ElastiCache Serverless"

  ingress {
    from_port       = 6379
    to_port         = 6379
    protocol        = "tcp"
    security_groups = var.allowed_security_group_ids
    description     = "Redis from EKS nodes"
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = local.tags
}

# ElastiCache Serverless
resource "aws_elasticache_serverless_cache" "main" {
  engine = "redis"
  name   = "${var.cache_name}-${var.environment}"

  cache_usage_limits {
    data_storage {
      maximum = var.environment == "prod" ? 10 : 2
      unit    = "GB"
    }
    ecpu_per_second {
      maximum = var.environment == "prod" ? 10000 : 1000
    }
  }

  subnet_ids         = var.private_subnet_ids
  security_group_ids = [aws_security_group.redis.id]

  tags = local.tags
}

# Outputs
output "endpoint" {
  value = aws_elasticache_serverless_cache.main.endpoint[0].address
}

output "port" {
  value = aws_elasticache_serverless_cache.main.endpoint[0].port
}

output "security_group_id" {
  value = aws_security_group.redis.id
}

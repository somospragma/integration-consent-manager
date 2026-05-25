###############################################################################
# Module: Observability
# Monitoring costo-eficiente:
# - CloudWatch para logs (ya incluido en AWS)
# - Prometheus + Grafana en K8s (open source, sin costo de licencia)
# - Alertas vía SNS (casi gratis)
###############################################################################

variable "environment" {
  type = string
}

variable "cluster_name" {
  type = string
}

variable "alert_email" {
  description = "Email for critical alerts"
  type        = string
  default     = ""
}

locals {
  tags = {
    Environment = var.environment
    Project     = "consent-manager-pragma"
    ManagedBy   = "terraform"
  }
}

# CloudWatch Log Group (para logs de EKS)
resource "aws_cloudwatch_log_group" "eks" {
  name              = "/eks/${var.cluster_name}/consent-manager"
  retention_in_days = var.environment == "prod" ? 90 : 14

  tags = local.tags
}

# CloudWatch Log Group (para audit trail - retención larga)
resource "aws_cloudwatch_log_group" "audit" {
  name              = "/consent-manager/${var.environment}/audit"
  retention_in_days = 1827 # 5 años (regulatorio)

  tags = local.tags
}

# SNS Topic para alertas
resource "aws_sns_topic" "alerts" {
  name = "consent-manager-${var.environment}-alerts"
  tags = local.tags
}

resource "aws_sns_topic_subscription" "email" {
  count     = var.alert_email != "" ? 1 : 0
  topic_arn = aws_sns_topic.alerts.arn
  protocol  = "email"
  endpoint  = var.alert_email
}

# CloudWatch Alarms (solo las críticas)
resource "aws_cloudwatch_metric_alarm" "high_error_rate" {
  count               = var.environment == "prod" ? 1 : 0
  alarm_name          = "consent-manager-${var.environment}-high-error-rate"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 3
  metric_name         = "5XXError"
  namespace           = "consent-manager"
  period              = 60
  statistic           = "Sum"
  threshold           = 10
  alarm_description   = "High 5XX error rate in consent-manager"
  alarm_actions       = [aws_sns_topic.alerts.arn]

  tags = local.tags
}

# Outputs
output "log_group_name" {
  value = aws_cloudwatch_log_group.eks.name
}

output "audit_log_group_name" {
  value = aws_cloudwatch_log_group.audit.name
}

output "alerts_topic_arn" {
  value = aws_sns_topic.alerts.arn
}

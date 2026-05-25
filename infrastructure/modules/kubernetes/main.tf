###############################################################################
# Module: Kubernetes Namespace & Resources
# Configura namespace, service accounts, network policies para consent-manager
###############################################################################

variable "environment" {
  description = "Environment: dev, sandbox, staging, prod"
  type        = string
}

variable "namespace" {
  description = "Kubernetes namespace"
  type        = string
  default     = "consent-manager"
}

###############################################################################
# Namespace
###############################################################################

resource "kubernetes_namespace" "consent_manager" {
  metadata {
    name = "${var.namespace}-${var.environment}"
    labels = {
      app         = "consent-manager"
      environment = var.environment
      "istio-injection" = "enabled"
    }
  }
}

###############################################################################
# Network Policy - Deny All by Default
###############################################################################

resource "kubernetes_network_policy" "deny_all" {
  metadata {
    name      = "deny-all"
    namespace = kubernetes_namespace.consent_manager.metadata[0].name
  }

  spec {
    pod_selector {}
    policy_types = ["Ingress", "Egress"]
  }
}

###############################################################################
# Network Policy - Allow internal communication
###############################################################################

resource "kubernetes_network_policy" "allow_internal" {
  metadata {
    name      = "allow-consent-manager-internal"
    namespace = kubernetes_namespace.consent_manager.metadata[0].name
  }

  spec {
    pod_selector {
      match_labels = {
        app = "consent-manager"
      }
    }

    ingress {
      from {
        namespace_selector {
          match_labels = {
            app = "consent-manager"
          }
        }
      }
    }

    egress {
      to {
        namespace_selector {
          match_labels = {
            app = "consent-manager"
          }
        }
      }
    }

    policy_types = ["Ingress", "Egress"]
  }
}

###############################################################################
# Resource Quotas
###############################################################################

resource "kubernetes_resource_quota" "consent_manager" {
  metadata {
    name      = "consent-manager-quota"
    namespace = kubernetes_namespace.consent_manager.metadata[0].name
  }

  spec {
    hard = {
      "requests.cpu"    = var.environment == "prod" ? "16" : "4"
      "requests.memory" = var.environment == "prod" ? "32Gi" : "8Gi"
      "limits.cpu"      = var.environment == "prod" ? "32" : "8"
      "limits.memory"   = var.environment == "prod" ? "64Gi" : "16Gi"
      pods              = var.environment == "prod" ? "50" : "20"
    }
  }
}

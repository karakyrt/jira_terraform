resource "kubernetes_persistent_volume_claim" "jira-pvc" {
  metadata {
    name      = "jira-pvc"
    namespace = "${var.namespace}"

    labels {
      app = "jira-deployment"
    }
  }

  spec {
    access_modes = ["ReadWriteOnce"]
    resources {
      requests {
        storage = "10Gi"
      }
    }
    storage_class_name = "standard"
  }
}
resource "kubernetes_deployment" "jira-deployment" {
  metadata {
    name      = "jira-deployment"
    namespace = "${var.namespace}"

    labels {
      app = "jira-deployment"
    }
  }
  spec {
    replicas = 1

    selector {
        match_labels {
            app = "jira-deployment"
        }
    }

    template {
      metadata {
        labels {
          app = "jira-deployment"
        }
      }

      spec {
        volume {
          name = "jira-pvc"

          persistent_volume_claim {
            claim_name = "jira-pvc"
          }
        }

        container {
          name  = "jira-container"
          image = "fsadykov/docker-jira"

          port {
            name           = "jira-http"
            container_port = 8081
          }
          port {
            name           = "docker-repo"
            container_port = 8085
          }

          env {
            name  = "INSTALL4J_ADD_VM_PARAMS"
            value = "-Xms1200M -Xmx1200M -XX:MaxDirectMemorySize=2G -XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap"
          }

          resources {
            requests {
              memory = "4800Mi"
              cpu    = "500m"
            }
          }

          volume_mount {
            name       = "jira-pvc"
            mount_path = "/var/lib/jira"
          }
          image_pull_policy = "IfNotPresent"
        }
      }
    }
  }
}
resource "kubernetes_service" "jira-service" {
  metadata {
    name      = "jira-service"
    namespace = "${var.namespace}"
  }
  spec {
    port {
      name        = "http"
      protocol    = "TCP"
      port        = 80
      target_port = 8081
    }
    port {
      name        = "docker-repo"
      protocol    = "TCP"
      port        = 8085
      target_port = 8085
    }
    selector {
      app = "jira-deployment"
    }
    type = "LoadBalancer"
  }
}
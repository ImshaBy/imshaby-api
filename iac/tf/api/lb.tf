# resource "yandex_alb_target_group" "reddit-app-target-group" {
#   name = "reddit-app-target-group"

#   dynamic "target" {
#     for_each = yandex_compute_instance.app
#     content {
#       subnet_id  = var.subnet_id
#       ip_address = target.value.network_interface.0.ip_address
#     }
#   }
# }

data "yandex_vpc_subnet" "default" {
  name = var.subnet_name
}

resource "yandex_alb_target_group" "api_alb_target_group" {
  name           = "prod-api-alb-target-group" // var.api_target_group_name

  target {
    subnet_id    = data.yandex_vpc_subnet.default.id
    ip_address   = yandex_compute_instance.api_app.network_interface.0.ip_address
  }
}

resource "yandex_alb_backend_group" "api_backend_group" {
  name                     = "prod-api-backend-group" // var.api_backend_group_name
  session_affinity {
    connection {
      source_ip = true
    }
  }

  http_backend {
    name                   = "api-back-end"
    weight                 = 1
    port                   = 9011
    target_group_ids       = [yandex_alb_target_group.api_alb_target_group.id]
    load_balancing_config {
      panic_threshold      = 90
    }
    healthcheck {
      timeout              = "10s"
      interval             = "2s"
      healthy_threshold    = 10
      unhealthy_threshold  = 15
      http_healthcheck {
        path               = "/ping1"
      }
    }
  }
}


resource "yandex_alb_http_router" "api_http_router" {
  name   = "prod-api-http-router" //var.api_http_router_name
}

resource "yandex_alb_virtual_host" "api_virtual_host" {
  name           = "prod-api-virtual-host" //var.api_virtual_host
  http_router_id = yandex_alb_http_router.api_http_router.id
  authority = ["yc.api.imsha.by"] // var.api_domain_name
  route {
    name = "prod-api-route"
    http_route {
      http_route_action {
        backend_group_id = yandex_alb_backend_group.api_backend_group.id
        timeout          = "3s"
      }
    }
  }
}

#resource "yandex_vpc_address" "api_alb_addr" {
#  name = "prod-api-alb-addr" //var.api_alb_addr_name
#
#  external_ipv4_address {
#    zone_id = data.yandex_vpc_subnet.default.zone
#  }
#}

# data "yandex_cm_certificate" "my_cert" {
# #   folder_id = "b1g6h3qks5n5r2p65aji" // var.folder_id
# #   name      = "yc-imshaby-cert" //var.tls_cert_name
#     certificate_id   = "fpqm2crjdaa9lv96e686"
#   wait_validation = true
# }

resource "yandex_alb_load_balancer" "api_alb" {
  name        = "prod-api-alb" //var.alb_name

  network_id  = data.yandex_vpc_subnet.default.network_id

  allocation_policy {
    location {
      zone_id   = data.yandex_vpc_subnet.default.zone
      subnet_id = data.yandex_vpc_subnet.default.id
    }
  }

  listener {
    name = "prod-api-https-listener" //var.api_https_listener_name
    endpoint {
      address {
        external_ipv4_address {
#          address = yandex_vpc_address.api_alb_addr.external_ipv4_address.0.address
        }
      }
      ports = [ 443,80 ]
    }
    tls {
      default_handler  {
        http_handler {
          http_router_id = yandex_alb_http_router.api_http_router.id
        }
        # stream_handler {
        #     backend_group_id = yandex_alb_backend_group.api_backend_group.id
        # }
        certificate_ids = ["fpqm2crjdaa9lv96e686"] //var??
      }
      sni_handler {
        name = "prod-api-sni-handler" // var.api_sni_handler_name
        server_names = ["api.yc.imsha.by"] // var.api_domain_name
        handler {
          http_handler {
            http_router_id = yandex_alb_http_router.api_http_router.id
          }
          # stream_handler {
          #     backend_group_id = yandex_alb_backend_group.api_backend_group.id
          # }
          certificate_ids = ["fpqm2crjdaa9lv96e686"] //var??
        }
      }
    }
  }
  log_options {
    discard_rule {
      http_code_intervals = ["HTTP_2XX"]
      discard_percent = 75
    }
  }
}
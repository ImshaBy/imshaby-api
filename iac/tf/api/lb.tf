
data "yandex_vpc_subnet" "default" {
  name = var.subnet_name
}

resource "yandex_alb_target_group" "api_alb_target_group" {
  name           = var.api_target_group_name

  target {
    subnet_id    = data.yandex_vpc_subnet.default.id
    ip_address   = yandex_compute_instance.api_app.network_interface.0.ip_address
  }
}

resource "yandex_alb_backend_group" "api_backend_group" {
  name                     = var.api_backend_group_name
  session_affinity {
    connection {
      source_ip = true
    }
  }

  http_backend {
    name                   = var.api_back_end_name
    weight                 = 1
    port                   = 80 //var.api_back_port
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
        path               = "/status"
      }
    }
  }
}


resource "yandex_alb_http_router" "api_http_router" {
  name   = var.api_http_router_name
}

resource "yandex_alb_virtual_host" "api_virtual_host" {
  name           = var.api_virtual_host
  http_router_id = yandex_alb_http_router.api_http_router.id
  authority = [var.api_domain_name] //
  route {
    name = var.api_route_name
    http_route {
      http_route_action {
        backend_group_id = yandex_alb_backend_group.api_backend_group.id
        timeout          = "3s"
      }
    }
  }
}

resource "yandex_alb_load_balancer" "api_alb" {
  name        = var.api_alb_name

  network_id  = data.yandex_vpc_subnet.default.network_id

  allocation_policy {
    location {
      zone_id   = data.yandex_vpc_subnet.default.zone
      subnet_id = data.yandex_vpc_subnet.default.id
    }
  }

  listener {
    name = var.api_https_listener_name
    endpoint {
      address {
        external_ipv4_address {
#          address = yandex_vpc_address.api_alb_addr.external_ipv4_address.0.address
        }
      }
      ports = [ 80 ]
    }
     http {
       handler  {
             http_router_id = yandex_alb_http_router.api_http_router.id
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
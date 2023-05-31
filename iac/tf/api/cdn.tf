
resource "yandex_cdn_origin_group" "api_cdn_group" {
  name = var.api_cdn_origin_group_name
  use_next = true
  origin {
    source = "${yandex_alb_load_balancer.api_alb.listener[0].endpoint[0].address[0].external_ipv4_address[0].address}:80"
#    source = "${yandex_compute_instance.api_app.network_interface.0.nat_ip_address}:80"
  }
}


resource "yandex_cdn_resource" "cms_cdn_resource" {
  cname = "yc.api.imsha.by"
  origin_protocol = "http"
  active = true
  origin_group_id = yandex_cdn_origin_group.api_cdn_group.id
  ssl_certificate {
    type = "lets_encrypt_gcore"
  }
  options {
    disable_cache = true

    # static_request_headers =["X-Forwarded-Port"]
  }
}
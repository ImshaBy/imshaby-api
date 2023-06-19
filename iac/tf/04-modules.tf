
module yc_api {
  source = "./api"
  az = var.az
  platform_id = var.platform_id
  api_app_name = var.api_app_name
  service_acc_name = var.service_acc_name
  subnet_name = var.subnet_name
  api_target_group_name = var.api_target_group_name
  api_backend_group_name = var.api_backend_group_name
  api_back_end_name = var.api_back_end_name
  api_http_router_name = var.api_http_router_name
  api_virtual_host = var.api_virtual_host
  api_domain_name = var.api_domain_name
  api_route_name = var.api_route_name
  api_alb_name = var.api_alb_name
  api_https_listener_name = var.api_https_listener_name
  api_cdn_origin_group_name = var.api_cdn_origin_group_name
}


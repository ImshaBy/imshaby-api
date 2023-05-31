// Network

variable "subnet_name" {
  type        = string
}


variable "az" {
  type        = string
  default     = "ru-central1-a"
}



// Compute Instance
variable "platform_id" {
  type        = string
  default     = "standard-v3"
}


// API Server
variable "service_acc_name" {
  type        = string
}

variable "api_app_name" {
  type        = string
}

variable "api_target_group_name" {
  type        = string
}
variable "api_backend_group_name" {
  type        = string
}
variable "api_back_end_name" {
  type        = string
}
variable "api_http_router_name" {
  type        = string
}
variable "api_virtual_host" {
  type        = string
}
variable "api_domain_name" {
  type        = string
}
variable "api_route_name" {
  type        = string
}
variable "api_alb_name" {
  type        = string
}
variable "api_https_listener_name" {
  type        = string
}
variable "api_cdn_origin_group_name" {
  type        = string
}
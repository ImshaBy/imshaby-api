// Network

variable "subnet_id" {
  type        = string
  description = "Id of subnet in AZ"
  default     = "TBD"
}

variable "network_id" {
  type        = string
}

variable "network_name" {
  type        = string
  description = "Id of network in AZ"
  default     = "default"
}

variable "zone_name" {
  type        = string
  default     = "default"
}

variable "zone" {
  type        = string
  default     = "default"
}

variable "az" {
  type        = string
  default     = "ru-central1-a"
}

variable "db_dns_internal_name" {
  type        = string
}


// Compute Instance
variable "platform_id" {
  type        = string
  default     = "standard-v3"
}

// Postgress DB
variable "postgress_user" {
  type        = string
  default     = "admin"
}
variable "postgress_password" {
  type        = string
  default     = "pass"
}
variable "database_host" {
  type        = string
  default     = "localhost"
}
variable "db_disk_name" {
}

// API Server

variable "api_app_name" {
  type        = string
  description = "Name to be used for compute cloud instance name"
  default     = "identity-app"
}
variable "database_password" {
  type        = string
  default     = ""
}
variable "database_user" {
  type        = string
  default     = ""
}

// DB Server
variable "db_instance_name" {
  type        = string
  default     = "db-instances"
}
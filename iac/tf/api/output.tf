output "api_external_ip" {
  value = yandex_compute_instance.api_app.network_interface.0.nat_ip_address
}
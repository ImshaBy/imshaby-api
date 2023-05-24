terraform {
  required_providers {
    yandex = {
      source = "yandex-cloud/yandex"
    }
  }
  required_version = ">= 0.13"
}


provider "yandex" {
  alias = "yc_ru-central1-a"
  zone = var.az
}

data "yandex_compute_image" "container-optimized-image" {
  family = "container-optimized-image"
}

# ресурс "yandex_compute_instance" т.е. сервер
resource "yandex_compute_instance" "api_app" {
  name = var.api_app_name
  zone = var.az
  platform_id = var.platform_id
  resources {
    cores  = 2 # vCPU
    memory = 2 # RAM
  }

  boot_disk {
    auto_delete = true
    initialize_params {
      image_id = data.yandex_compute_image.container-optimized-image.id
    }
  }

  network_interface {
    subnet_id = "e9bgufd35inak5suac9d" // var.subnet_id # одна из дефолтных подсетей
    nat = true # автоматически установить динамический ip
  }

  metadata = {
    docker-compose = file("${path.module}/tf_docker-compose.yml")
    user-data = file("cloud_config.yaml")
  }

}

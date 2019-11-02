﻿
[![Build Status](https://travis-ci.org/childRon/imshaby-api.svg?branch=master)](https://travis-ci.org/childRon/imshaby-api)

# imshaby-api


Git process:

git remote add openshift -f ssh://52d184e45973ca0bc0000088@wotan-anthavio.rhcloud.com/~/git/wotan.git/
git merge openshift/master -s recursive -X ours
git push openshift HEAD

 Новый сервер MongoDB был добавлен к вашему окружению Jelastic PaaS. 
 
 Этот сервер выделен эксклюзивно для вас (без доступа для других пользователей), поэтому все ресурсы и производительность находятся под вашим контролем. Вы также можете настроить конфигурационные файлы через панель управления. 

 Доступ к интерфейсу веб-администрирования RockMongo: 
 URL: https://mongodb48105-api-imshaby.mycloud.by 
 Логин: admin 
 Пароль: MCGcqv70951
 
 Для подключения к MongoDB из кода приложения используйте следующую информацию: 
 Хост: mongodb48105-api-imshaby.mycloud.by 
 Логин: admin 
 Пароль: MCGcqv70951 
 База данных: (создайте, используя RockMongo)
 
 Не следует использовать Localhost в коде приложения. MariaDB находится на выделенном сервере, поэтому вы должны использовать mongodb48105-api-imshaby.mycloud.by для подключения.
 

#  CI/CD

build war for prod: mvn clean install -Pprod

mongod --port 27017 --dbpath "C:\Env\mongo\db"


export from beta: mongoexport --db api --collection city --username admin --password Hvd6jxKpiF --authenticationDatabase admin --jsonArray --out /tmp/city-pretty.json

#  CI/CD in local k8s cluster (minkube on local VM)
### Build jar & docker image 
```bash
> sh build/build_imshaby_api.sh
```
### Deploy ImshaBy API & MongoDB single-pod clusters
```bash
> sh build/deploy_all.sh
```
### Destroy ImshaBy API & MongoDB single-pod clusters
```bash
> sh build/destroy_all.sh
```
### ImshaBy API helm chart configuration (values-<env_to_deploy>.yaml)
```yaml
# count of service API pods
replicaCount: 1 
image:
  # docker repository
  repository: local/imshaby-api
  # image tag
  tag: v1.0
  # policy to pull docker image
  pullPolicy: IfNotPresent

# spring active profile
activeProfile: local
  
mongodb:
  # mongodb instance host
  host: mongodb
  # mongodb instance port
  port: 27017
  # mongodb instance database name
  database: api
  # mongodb user name
  user: api_admin
  # mongodb password
  password: api_admin
  
service:
  # Load balancing type
  type: ClusterIP
  # Load balancing outcome port
  port: 3000
```
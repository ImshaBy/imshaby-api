
[![Build Status](https://travis-ci.org/childRon/imshaby-api.svg?branch=master)](https://travis-ci.org/childRon/imshaby-api)

# imshaby-api

https://next.stoplight.io/childRon/imshaby/version%2F1.3/api-docs-new.oas2.yml?edit=/paths/~1api~1mass~1week/get&domain=imshaby.docs.stoplight.io

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

mongod --port 27017 --dbpath "c:\Env\mongo-4.2.8\db"

export from beta: mongoexport --db api --collection city --username admin --password Hvd6jxKpiF --authenticationDatabase admin --jsonArray --out /tmp/city-pretty.json

#  CI/CD in local k8s cluster (minkube on local VM)
### Build jar & docker image 
```bash
> cd build
> sh build_imshaby_api.sh
```
### Check local k8s cluster is ready
#### avg. time to start is about 3 mins after VM reboot
```bash
> systemctl status development.env.service
```
### Deploy ImshaBy API & MongoDB single-pod clusters
```bash
> cd build
> sh deploy_all.sh
```
### Check state of all k8s elements in "imsha-by" namespace
```bash
> kubectl -n imsha-by get all
```
### Follow logs of app running on a k8s pod
#### \<pod_name\> - a unique name of pod, can be got using "kubectl get" cmd
```bash
> kubectl -n imsha-by logs <pod_name> -f
```
#### EXAMPLE
```bash
> kubectl -n imsha-by logs imshaby-api-6bcf7475bd-wnkz4 -f
```
### Forward specific ports of running apps to host VM (pod_name is a unique name of pod, can be got using "kubectl get" cmd)
#### \<pod_name\> - a unique name of pod, can be got using "kubectl get" cmd
#### \<local VM port\> - a local port on VM
#### \<internal pod port\> - an internal port on a k8s pod
```bash
> kubectl -n imsha-by port-forward <pod_name> --address=0.0.0.0 <local VM port>:<internal pod port>
```
#### EXAMPLE
```bash
> kubectl -n imsha-by port-forward imshaby-api-6bcf7475bd-wnkz4 --address=0.0.0.0 8080:8080
```
### Destroy ImshaBy API & MongoDB single-pod clusters
```bash
> cd build
> sh destroy_all.sh
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

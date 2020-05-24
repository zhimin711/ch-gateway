
# 上传配置  

```
scp -r src/main/docker/Dockerfile zhimin@192.168.1.100:/home/zhimin/docker/ch-gateway
scp -r build/libs/ch-gateway-1.0.0-SNAPSHOT.jar zhimin@192.168.199.194:/home/zhimin/docker/ch-gateway
```

### 打包镜像  
```
docker build -t ch-gateway:v1  /home/zhimin/docker/ch-gateway
```

### 启动
```
sudo docker run --name ch-gateway \
--net=none \
-v /home/zhimin/share/logs:/mnt/share/logs  \
-m 512M --memory-swap -1 \
-d ch-gateway:v1 ;
```
```
docker run --name ch-gateway \
-p 7001:7001 \
-p 7071:7070 \
-v /home/zhimin/share/logs:/mnt/share/logs  \
-v /home/zhimin/share/plugins:/mnt/share/plugins  \
-e JAVA_OPTS='-Dspring.profiles.active=test -javaagent:/mnt/share/plugins/jmx_prometheus_javaagent-0.12.0.jar=7070:/mnt/share/plugins/simple-config.yaml' \
-m 700M --memory-swap -1 \
-d ch-gateway:v1 ;
```
### 重启 停止 删除
```
 docker restart ch-gateway;
 docker stop ch-gateway;
 docker rm ch-gateway;
 docker rmi ch-gateway:v1;
```
```
sudo pipework br0 ch-gateway 192.168.1.10/24@192.168.1.1
```
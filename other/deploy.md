
# 上传配置  

```
scp -r ch-gateway/src/main/docker/Dockerfile zhimin@192.168.1.100:/home/zhimin/docker/ch-gateway
scp -r ch-gateway/build/libs/ch-gateway-1.0.1-SNAPSHOT.jar zhimin@192.168.1.100:/home/zhimin/docker/ch-gateway
```

### 打包镜像  
```
sudo docker build -t ch-gateway:v1  /home/zhimin/docker/ch-gateway
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
-v /home/zhimin/share/logs:/mnt/share/logs  \
-d ch-gateway:v1 ;
```
### 重启 停止 删除
```
sudo docker restart ch-gateway;
sudo docker stop ch-gateway;
sudo docker rm ch-gateway;
sudo docker rmi ch-gateway:v1;
```
```
sudo pipework br0 ch-gateway 192.168.1.10/24@192.168.1.1
```
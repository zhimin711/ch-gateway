
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
docker run --name ch-gateway \
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

## Jenkins Job
### ci job
```groovy
// parameters {
//     gitParameter branchFilter: 'origin/(.*)', defaultValue: 'master', name: 'BRANCH', type: 'PT_BRANCH'
// }
def label = "jnlp-${JOB_NAME}"
def app_name = "ch-gateway"
def img_name = "ch-gateway:${DATETIME}"
def img_namespace = "ch"
def docker_api = "-H tcp://192.168.0.253:2375"
// def hub_addr = "192.168.0.253:8083"
def hub_addr = "registry.kubeoperator.io:8085"
def k8s_hub_addr = "registry.kubeoperator.io:8082"
def k8s_url = "https://192.168.0.252:8443"

podTemplate(label: label, cloud: 'kubernetes', inheritFrom: 'jenkins-slave-maven') {
    node(label) {
            stage('Checkout Project') {
                echo "1.Clone Project "
                git credentialsId: 'CHGitee2', url: 'https://gitee.com/ch-cloud/ch-gateway.git/', branch: "${params.BRANCH}"
            }
            stage('Build project') {
                container('maven') {
                    // sh 'git config --global url."https://".insteadOf ssh://git@'
                    echo "2.Build Project Deploy Package File"
                    sh 'mvn clean install -U'
                }
            }
            stage('Build Image') {
                echo "3.Build Project Docker Image"
                // sh "cd ${WORKSPACE}/web"
                container('docker') {
                    sh "docker ${docker_api} build -t ${img_name} -f ${WORKSPACE}/web/src/main/docker/Dockerfile ${WORKSPACE}/web/target"
                    sh "docker ${docker_api} tag ${img_name} ${hub_addr}/${img_namespace}/${img_name}"
                }

            }
            stage('Push Image') {
                echo "4.Push Project Docker Image"
                container('docker')  {
                    withCredentials([usernamePassword(credentialsId: 'Nexus', passwordVariable: 'dockerPassword', usernameVariable: 'dockerUser')]) {
                        sh "docker ${docker_api} login -u ${dockerUser} -p ${dockerPassword} ${hub_addr}"
                        sh "docker ${docker_api} push ${hub_addr}/${img_namespace}/${img_name}"
                        sh "docker ${docker_api} rmi ${hub_addr}/${img_namespace}/${img_name} ${img_name}"
                    }
                }
            }
            stage('Deploy Image') {
                echo "5.Deploy Project Docker Image"
                container ('docker') {
                    script{
                        out=sh(script:"ls ./kubectl",returnStatus:true)
                        println "--------------"
                        println out
                        if(out == 0){
                            println "file is exist"
                        } else if(out == 1 || out == 2){
                            println "file is not exist"
                            sh 'cp ../kubectl .'
                            sh 'chmod u+x ./kubectl'
                        } else {
                            error("command is error,please check")
                        }
                    }
                    withKubeConfig([credentialsId:'kubeMaster'
                                    ,serverUrl: "${k8s_url}"
                                    ,namespace: "ch"]) {
                        sh "./kubectl set image deployment/${app_name} *=${k8s_hub_addr}/${img_namespace}/${img_name}"
                    }
                }
            }
    }
}
```
### release job
```groovy
// parameters {
//     gitParameter branchFilter: 'origin/(.*)', defaultValue: 'master', name: 'BRANCH', type: 'PT_BRANCH'
// }
def label = "jnlp-${JOB_NAME}"
def docker_api = "-H tcp://192.168.0.253:2375"
def img_name = "ch-gateway:${DATETIME}"
// def hub_addr = "192.168.0.253:8083"
def hub_addr = "registry.cn-hangzhou.aliyuncs.com"
def hub_namespace = "ch-cloud"

podTemplate(label: label, cloud: 'kubernetes', inheritFrom: 'jenkins-slave-maven') {
    node(label) {
            stage('Checkout Project') {
                echo "1.Clone Project "
                git credentialsId: 'CHGitee2', url: 'https://gitee.com/ch-cloud/ch-gateway.git/', branch: "${params.BRANCH}"
            }
            stage('Build project') {
                container('maven') {
                    // sh 'git config --global url."https://".insteadOf ssh://git@'
                    echo "2.Build Project Deploy Package File"
                    sh 'mvn clean package -U'
                    sh "cp ${WORKSPACE}/../apache-skywalking-java-agent-8.11.0.tgz ${WORKSPACE}/web/target"
                }
            }
            stage('Build Image') {
                echo "3.Build Project Docker Image"
                // sh "cd ${WORKSPACE}/web"
                container('docker') {
                    sh "docker ${docker_api} build -t ${img_name} -f ${WORKSPACE}/web/src/main/docker/Dockerfile ${WORKSPACE}/web/target"
                    sh "docker ${docker_api} tag ${img_name} ${hub_addr}/${hub_namespace}/${img_name}"
                }

            }
            stage('Push Image') {
                echo "4.Push Project Docker Image"
                container('docker')  {
                    withCredentials([usernamePassword(credentialsId: 'aliHub', passwordVariable: 'dockerPassword', usernameVariable: 'dockerUser')]) {
                        sh "docker ${docker_api} login -u ${dockerUser} -p ${dockerPassword} ${hub_addr}"
                        sh "docker ${docker_api} push ${hub_addr}/${hub_namespace}/${img_name}"
                        sh "docker ${docker_api} rmi ${hub_addr}/${hub_namespace}/${img_name} ${img_name}"
                    }
                }
            }
    }
}
```
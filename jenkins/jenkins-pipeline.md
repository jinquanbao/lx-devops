# 什么是jenkins pipeline

Jenkins Pipeline是jenkins的插件，Pipeline支持两种语法：Declarative（在Pipeline 2.5中引入）和Scripted Pipeline，即声明式流水线和脚本式流水线。

通过jenkins pipeline，我们能够直观的看到构建的整个过程，各个构建过程耗时，哪一步构建出错。



# Declarative pipeline语法

## 基础指令

### agent 

agent ：用于表述性Pipeline中，表示Pipeline或特定阶段将在Jenkins环境中执行的位置，具体取决于该agent 的放置位置；agent 必须在pipeline 顶层定义。

agent参数必须是(any|none|label|node|docker|dockerfile )

```
pipeline {
	agent any
	....
}
```

### stages

代表整个流水线的所有执行阶段。通常stages只有1个，里面包含多个stage，stages在Pipeline必须定义

```
pipeline {
    agent any
    stages { 
        stage('代码检出') {
            ...
        }
    }
}
```



### stage

代表流水线中的某个阶段，脚本中可能会出现多个阶段。一般分为拉取代码、编译构建、部署等阶段。

### steps

代表一个阶段内需要执行的逻辑。steps里面是shell脚本，一般包括：git拉取代码，ssh远程发布等操作内容。

```
pipeline {
    agent any
    stages {
        stage('代码检出') {
            steps { 
                echo '代码检出了'
            }
        }
    }
}
```



### post

`post` 根据流水线或阶段的完成情况而 运行(取决于流水线中 `post` 部分的位置). 我们可以根据运行完成情况发送相应的通知信息

```
post {
    always {
    	echo '无论流水线或阶段的完成状态如何，都允许在 post 部分运行该步骤。'
    }
    success {
    	echo '只有当前流水线或阶段的完成状态为"success"，才允许在 post 部分运行该步骤, 通常web UI是蓝色或绿色。'
    }
    failure {
    	echo '只有当前流水线或阶段的完成状态为"failure"，才允许在 post 部分运行该步骤, 通常web UI是红色。'
    }
    unstable {
    	echo '只有当前流水线或阶段的完成状态为"unstable"，才允许在 post 部分运行该步骤, 通常由于测试失败,代码违规等造	  			成。通常web UI是黄色。'
    }
    changed {
    	echo '只有当前流水线或阶段的完成状态与它之前的运行不同时，才允许在 post 部分运行该步骤。'

    }
    aborted {
    	echo '只有当前流水线或阶段的完成状态为"aborted"，才允许在 post 部分运行该步骤, 通常由于流水线被手动的aborted。通常		web UI是灰色。'            
    }
}

```

### 示例

有了前面的阶段定义语法，我们就可以来实战定义pipeline了。首先我们新建一个item，选择流水线

![image-20220625191418864](C:\Users\12553\AppData\Roaming\Typora\typora-user-images\image-20220625191418864.png)



#### 配置pipeline script

在流水线配置pipeline script 并保存

![image-20220625203442343](.\images\image-20220625203442343.png)

```

pipeline {
	agent any
	stages {
        stage('代码检出') {
            steps { 
                echo '代码检出了'
            }
        }
        stage('编译') {
            steps { 
                echo '编译完了'
            }
        }
        stage('部署') {
            steps { 
                echo '部署完了'
            }
        }
    }
	post {
        always {
            echo '无论流水线或阶段的完成状态如何，都允许在 post 部分运行该步骤。'
        }
        success {
            echo '只有当前流水线或阶段的完成状态为"success"，才允许在 post 部分运行该步骤, 通常web UI是蓝色或绿色。'
        }
        failure {
            echo '只有当前流水线或阶段的完成状态为"failure"，才允许在 post 部分运行该步骤, 通常web UI是红色。'
        }
        unstable {
            echo '只有当前流水线或阶段的完成状态为"unstable"，才允许在 post 部分运行该步骤, 通常由于测试失败,代码违规等造	  成。通常web UI是黄色。'
        }
        changed {
            echo '只有当前流水线或阶段的完成状态与它之前的运行不同时，才允许在 post 部分运行该步骤。'
            
        }
        aborted {
            echo '只有当前流水线或阶段的完成状态为"aborted"，才允许在 post 部分运行该步骤, 通常由于流水线被手动的aborted。通常web UI是灰色。'            
        }
     }
}
```



我们点击立即构建，可以在阶段视图看到每个阶段步骤都被正确的执行了。

![image-20220625203610190](C:\Users\12553\AppData\Roaming\Typora\typora-user-images\image-20220625203610190.png)



## 扩展指令

在上一步阶段定义中我们非常快速的就完成了pipeline语法的各个阶段的定义执行，但是有一些问题我们需要考虑到

1.我们并没有实际的去拉取代码构建，拉取代码的语法如和写

2.不同构建项目如何重用一份pipeline脚本

3.如何根据不同的触发执行不同的阶段定义

4.如何执行定时触发构建

5.如何执行并发构建



通过扩展指令的学习，我们可以一一解决上述问题。



### environment

 键-值对环境变量，根据 `environment` 在流水线内定义的位置分为全局环境变量和局部环境变量，定义在stages之外，就是全局环境变量，所有stage阶段都能获取该环境变量，定义在stage内就是局部环境变量，只能在该stage内获取。

```
pipeline {
    agent any
    environment { 
        TAG_NAME = 'dev'
    }
    stages {
        stage('拉取代码') {
            environment { 
                GIT_CREDENTIAL_ID = credentials('git_credential_id') 
                GIT_URL = "xx.git" 
            }
            steps {
                checkout([$class: 'GitSCM', branches: [[name: "*/${TAG_NAME}"]], 													doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [],
					  	userRemoteConfigs: [[credentialsId: GIT_CREDENTIAL_ID , url: "${GIT_URL}"]]])
            }
        }
    }
}
```

### parameters

parameters指令提供了一个用户在触发流水线时的参数列表。这些参数的值在流水线中可以通过 `params` 对象获取。

参数类型分为可选字符串参数类型和布尔参数类型

```
pipeline {
    agent any
    parameters {
        string(name: 'TAG_NAME', defaultValue: 'dev', description: 'tag名称')
        booleanParam(name: 'PROD_DEPLOY', defaultValue: false, description: '是否生产发布')
    }
    stages {
        stage('test') {
            steps {
                echo "${params.TAG_NAME} ${params.PROD_DEPLOY}"
            }
        }
    }
}
```

### triggers

`triggers` 指令定义了流水线被重新触发的自动化方法，目前支持的触发器有`cron`, `pollSCM` 和 `upstream`。



```
pipeline {
    agent any
    triggers {
        cron('0 0 1 * * ?)
    }
    stages {
        stage('test') {
            steps {
                echo 'test cron'
            }
        }
    }
}
```

### when

条件表达式指令，when语句必须放在stage第一行，所有条件都满足才会执行该语句。

- **branch**语法：`when { branch 'master' }`注意，这只适用于多分支流水线。
- **environment**语法：`when { environment name: 'PROD_DEPLOY', value: true }`
- **expression**语法：`when { expression { return params.PROD_DEPLOY} }`
- **not**语法：`when { not { branch 'master' } }`
- **allOf**语法：`when { allOf { branch 'master'; environment name: 'PROD_DEPLOY', value: true  } }`
- **anyOf**语法：`when { anyOf { branch 'master'; branch 'staging' } }`

```
pipeline {
    agent any    
    stages {
        stage('test') {
            when {
                branch 'master'
            }            
            steps {
                echo 'test branch master'
            }
        }
    }
}

```



### parallel

通过parallel指令，可以并行跑多个steps。注意，一个阶段必须只有一个 `steps`，并且parallel不能嵌套

```
pipeline {
    agent any
    stages {
        stage('Non-Parallel') {
            steps {
                echo 'This stage will be executed first.'
            }
        }
        stage('Parallel Stage') {
            parallel {
                stage('build A project') {

                    steps {
                        echo "build A project"
                    }
                }
                stage('build B project') {

                    steps {
                        echo "build B project"
                    }
                }
            }
        }
    }
}
```

# 部署一个java服务

通过上面的的pipeline语法了解，我们来实际构建部署一个java应用

## 创建凭据

在构造部署前，我们先创建一个拉取代码的凭据：git_credential_id，后面的pipeline拉取代码会用到，选择系统管理-->manage credentials-->创建全局凭据，如下图：



![image-20220626140603600](.\images\image-20220626140603600.png)



## 定义pipeline脚本

我们将上面的阶段定义脚本拷贝过来，在每个阶段编写实际的部署脚本

```
pipeline {

   agent any

    parameters {
        string(name:'TAG_NAME',defaultValue: 'master',description:'')
        string(name:'GIT_URL',defaultValue: '',description:'')
        string(name: 'PROD_DEPLOY', defaultValue: 'false', description: '是否生产发布')
    }

    environment {
        GIT_CREDENTIAL_ID = 'git_credential_id'
        APP_NAME = 'java-deploy-test'
        JAVA_OPS = "-Xmx512m -Xms128m"
    }

    stages {
        stage ('checkout scm') {
            steps {
                checkout([$class: 'GitSCM', branches: [[name: "*/${TAG_NAME}"]], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [],
					  userRemoteConfigs: [[credentialsId: GIT_CREDENTIAL_ID , url: "${GIT_URL}"]]])
            }
        }


		stage ('build') {
			steps {
                dir("$APP_NAME"){
                    sh 'chmod +x ./gradlew; ./gradlew  clean bootJar'
                }
			}
		}


		stage('deploy to prod') {
		  when {
              environment name: 'PROD_DEPLOY', value: 'true'
          }
		  steps {
                sh 'java ${JAVA_OPS} -jar ${APP_NAME}/build/libs/${APP_NAME}-1.0.0.jar &'
		  }
		}

		stage('deploy to dev') {
          when {
              environment name: 'PROD_DEPLOY', value: 'false'
          }
          steps {
            sh 'java ${JAVA_OPS} -jar ${APP_NAME}/build/libs/${APP_NAME}-1.0.0.jar &'
          }
        }

    }

    post {
        always {
            echo '无论流水线或阶段的完成状态如何，都允许在 post 部分运行该步骤。'
        }
        success {
            echo '只有当前流水线或阶段的完成状态为"success"，才允许在 post 部分运行该步骤, 通常web UI是蓝色或绿色。'
        }
        failure {
            echo '只有当前流水线或阶段的完成状态为"failure"，才允许在 post 部分运行该步骤, 通常web UI是红色。'
        }
        unstable {
            echo '只有当前流水线或阶段的完成状态为"unstable"，才允许在 post 部分运行该步骤, 通常由于测试失败,代码违规等造	  成。通常web UI是黄色。'
        }
        changed {
            echo '只有当前流水线或阶段的完成状态与它之前的运行不同时，才允许在 post 部分运行该步骤。'

        }
        aborted {
            echo '只有当前流水线或阶段的完成状态为"aborted"，才允许在 post 部分运行该步骤, 通常由于流水线被手动的aborted。通常web UI是灰色。'
        }
    }
}

```



选择不部署到生产环境，查看部署结果



![image-20220625225621869](.\images\image-20220625225621869.png)



我们可以看到，通过参数化环境变量以及条件表达式，我们可以根据不同的参数化构建不同的服务，并部署到不同的环境。让我们想想还有什么问题：

1.pipeline配置在jenkins中，不易版本管理维护

2.如果jenkins宕机，脚本丢失的话，重新配置构建麻烦



如何解决呢？下节将介绍如何通过jenkinsfile构建pipeline



# jenkinsfile

`Jenkinsfile` 是一个文本文件，它包含了 Jenkins 流水线的定义并跟随源代码一起检入版本控制仓库。

**优点**

1.方便流水线脚本的代码评审/迭代

2.方便查看流水线的历史变更

3.方便不同开发人员的查看和编辑



## 代码库检入

我们创建一个`Jenkinsfile` ，并将上面的Jenkins pipeline脚本拷贝至jenkinsfile中：

![image-20220626143242653](.\images\image-20220626143242653.png)

## jenkins配置

历史的车轮滚滚向前，让我们在上面配置pipeline script时回退一步，在流水线的定义下拉，选择pipeline script from scm

![image-20220626144035204](.\images\image-20220626144035204.png)



输入git仓库，选择脚本路径：

![image-20220626144350113](.\images\image-20220626144350113.png)



保存后点击参数化构建，我们看到，跟上面的是一样构建结果

![image-20220626144719858](.\images\image-20220626144719858.png)



# 总结

到这里，似乎一切已经很完美，但是我们想想，并不是每一个人都需要了解pipeline，我们能不能做到，让不了解pipeline语法的人员也能通过简单的参数化配置(**比如只需要配置项目名,项目路径,部署环境**)就能配置好自动化构建项目呢？下一篇jenkins 扩展共享库将为您揭晓。
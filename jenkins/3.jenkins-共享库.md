# 前言

上一篇，我们初步了解了jenkins pipeline的语法以及通过jenkins pipeline自动构建了一个java 服务，在最后总结我们提出了一个问题，如何让不了解pipeline语法的人能够通过简单的参数化配置就能配置好自动化构建项目，接下来我们就来了解jenkins的共享库。



# Scripted Pipeline

在了解jenkins共享库之前，我们先来了解脚本式pipeline语法，因为jenkins共享库是通过脚本式pipeline编写的，当然，从Declarative pipeline 1.2版本开始，可以在全局脚本共享库中内嵌Declarative pipeline 语法。



## [Groovy](http://www.groovy-lang.org/)

脚本化流水线是由 [Groovy](http://groovy-lang.org/syntax.html)构建，Groovy 语言提供的大部分功能都可以用于脚本化流水线，说道这里有人是不是心里一万匹马呼啸而过

"什么，我只是想简单配置一下jenkins，怎么又要学一门新的语言!!!"，别急，我们先来了解一下groovy，看看groovy的介绍：



Groovy是构建在JVM上的一个轻量级却强大的动态语言, 它结合了Python、Ruby和Smalltalk的许多强大的特性，Groovy就是用Java写的 , Groovy语法与Java语法类似，代码能够与 Java 代码很好地结合。

我们用一段简单的脚本来演示一下：

```
node {
    throw new NullPointerException("live or die,that is a question");
}
```

将脚本配置到jenkins流水线中：

![image-20220626214454305](.\images\image-20220626214454305.png)

我们看下构建结果：

![image-20220626214422646](.\images\image-20220626214422646.png)



我们看到构建结果直接抛出了莎士比亚的经典名句："live or die,that is a question"，如果你是一个java程序员，是不是有一种分外的熟悉与亲切感，就跟在外打工一年，回家吃到了老母亲煮的饭菜："就是那股熟悉的味道"，你忍不住热泪盈眶，这时候你是不是想起了你刚刚入行时，明明测试的时候没有问题，一上线就莫名其妙来个NullPointerException，这不科学啊；不必忧伤，不必忧愁，先贤已经给我们总结了：这不是科学的问题，这是个哲学问题。



## Scripted Pipeline使用

刚刚我们通过一个简单的例子了解了jenkins 脚本式流水线是用groovy写的，现在我们介绍一下jenkins如何定义使用脚本式流水线

### 声明式流水线内嵌脚本

我们可以在声明式流水线中通过关键字script 内嵌groovy脚本

```
pipeline {

   agent any

   stages {
        stage ('checkout scm') {
            steps {
                echo 'Hello World'
                script {
                    def browsers = ['chrome', 'firefox']
                    for (int i = 0; i < browsers.size(); ++i) {
                        echo "Testing the ${browsers[i]} browser"
                    }
                }
            }
        }
	}
}

```

### 直接脚本式编写

我们可以直接在node块内编写groovy脚本，就跟写java一样

```
node {
	stage('代码检出') {
        if (env.BRANCH_NAME == 'master') {
            echo 'I only execute on the master branch'
        } else {
            echo 'I execute elsewhere'
        }
    }
    stage('编译') {
    	
    	println "编译完成"
    }
    stage('部署') {
    	def a="部署完成" 
    	println "${a}"
    }

}
```



# jenkins 共享库

前面我们基本了解了jenkins的声明式流水线与脚本式流水线，接下来我们就来实战如何将公共脚本抽离成共享库，通过简单的参数化配置，引用共享库实现自动化构建部署。



## 定义共享库

共享库通过库名称、代码检索方法（如SCM）、代码版本三个要素进行定义，库名称尽量简洁，因为它将在脚本中使用，在编写共享库的时候，我们要遵循官方给的代码目录结构。

### 目录结构

共享库的目录结构如下：

```
(root)
+- src                     # Groovy source files
|   +- org
|       +- foo
|           +- Bar.groovy  # for org.foo.Bar class
+- vars
|   +- foo.groovy          # for global 'foo' variable
|   +- foo.txt             # help for 'foo' variable
+- resources               # resource files (external libraries only)
|   +- org
|       +- foo
|           +- bar.json    # static helper data for org.foo.Bar
```

`src` 目录就是标准的 Java 源目录结构。当执行流水线时，该目录被添加到类路径下。

`vars` 目录定义可从流水线访问的全局脚本。 每个 `*.groovy` 文件遵循标准的驼峰格式 `camelCased`。

`resources` 目录允许从外部库中使用 `libraryResource` 步骤来加载有关的非 Groovy 文件。 目前，内部库不支持该特性。

根目录下的其他目录被保留下来以便于将来的增强。



### 配置全局共享库

通过*Manage Jenkins » Configure System » Global Pipeline Libraries*可以添加一个或多个共享库。这些库将全局可用，系统中的任何Pipeline都可以调用这些库中实现的功能。通过配置 Modern SCM的方式，可以保证在每次构建时获取到最新的共享库代码。

![image-20220627093022236](.\images\image-20220627093022236.png)

### 共享库脚本名称定义

在vars目录下定义共享库非常简单，只需需要使用 `@Library` 注解， 指定库的名字即可

```
#!groovy
@Library('jenkinsfile-util') _
//指定一个带分支或版本的共享库
@Library('jenkinsfile-util@1.0') _
//还可以定义共享库时引用其他共享库
@Library(['jenkinsfile-util', 'otherlib@abc1234']) _
```

当要引用src目录下的类库时，我们需要通过import导入具体的类名，比如创建一个User.groovy：

```
package com.laoxin.pipeline

class User {

    String name

    User(String name) {
        this.name = name
    }

    def getName() {
        return name
    }

    def setName(String name) {
        this.name = name
    }
}
```

我们在共享库中引用类库：

```
#!groovy

import com.laoxin.pipeline.User
import org.jenkinsci.plugins.workflow.libs.Library

@Library('jenkinsfile-util') _


def printUserName(){
    def user = new User("jack")

    println user.getName()
}
```

## 动态加载共享库

如果只需要加载全局变量/函数（vars/目录），语法非常简单：

```
library 'jenkinsfile-util'
```

此后脚本中可以访问该库中的任何全局变量。

比如我们调用上一步定义的printUserName()：

```
library 'jenkinsfile-util'
node{
	jenkisfileUtil.printUserName()
}
```

我们将脚本配置到流水线中

![image-20220627110834781](.\images\image-20220627110834781.png)

保存点击构建，查看构建日志，我们看到成功调用了依赖共享库中的方法

![image-20220627111109393](.\images\image-20220627111109393.png)



从src/目录中引用类也是可以的,不过只能动态地使用库类（无类型检查），因为在遇到一个 `library` 步骤时，脚本已经被编译了。从library步骤的返回值通过指定名称访问它们。类似于Java的语法调用`static` 方法:

```
library('my-shared-library').com.mycorp.pipeline.Utils.someStaticMethod()
```



如果在共享库的配置中启用了“允许默认版本被覆盖”，在配置`library` 步骤你也可以指定一个版本，该指定版本将会覆盖默认版本：

```
library 'my-shared-library@master'
```



## 实战部署

通过上面的介绍我们基本了解了共享库定义和使用，接下来我们就来实战部署一个java服务。

### 创建共享库工程

我们创建一个pipeline-shared-lib共享库工程，在vars目录定义一个jenkisfileUtil.groovy文件，共享库脚本内容如下：

```
#!groovy

import org.jenkinsci.plugins.workflow.libs.Library

@Library('jenkinsfile-util') _


def call(Map map){

    pipeline {

        agent any

        parameters {
            string(name:'TAG_NAME',defaultValue: "${map.TAG_NAME}",description:'')
            string(name:'GIT_URL',defaultValue: "${map.GIT_URL}",description:'')
            string(name: 'PROD_DEPLOY', defaultValue: "${map.PROD_DEPLOY}", description: '是否生产发布')
        }

        environment {
            GIT_CREDENTIAL_ID = "${map.GIT_CREDENTIAL_ID}"
            APP_NAME = "${map.APP_NAME}"
            JAVA_OPS = "${map.JAVA_OPS}"
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
	
}




```

### jenkins配置全局共享库

通过*Manage Jenkins » Configure System » Global Pipeline Libraries* 将创建的共享库工程配置好，参考上面的定义共享库-->配置全局共享库章节

### 源代码工程引用共享库

在源代码项目的jenkinsfile中引用共享库

```
library 'jenkinsfile-util'

node {
    def Map map = [:]
    map.putAll(params)
    map.put('GIT_CREDENTIAL_ID',"git_credential_id")
    map.put('APP_NAME',"java-deploy-test")
    map.put('JAVA_OPS',"-Xmx512m -Xms128m")
    jenkisfileUtil(map)
}
```

### 创建部署任务

jenkins新建一个pipeline-shared-test任务，选择流水线，在流水线配置中选择源代码配置的jenkinsfile

![image-20220627134348629](.\images\image-20220627134348629.png)



保存点击构建，我们看到构造结果成功执行

![image-20220627134658227](.\images\image-20220627134658227.png)



# 总结

我们看到，共享库的封装就跟我们封装底层sdk将频繁使用的功能抽象封装一样，将pipeline脚本的实现和复杂度封装到共享库中，在源代码工程中，一般开发人员不需要过多了解pipeline语法，只需简单的配置参数，调用共享库即可完成整个pipeline的构建，有效降低了pipeline脚本的复杂度。如果需要增加pipeline的stage，比如构建之前跑单元测试，只需要库开发人员修改共享库的脚本，增加stage控制开关，需要跑单元测试的项目根据场景配置相应的参数即可。
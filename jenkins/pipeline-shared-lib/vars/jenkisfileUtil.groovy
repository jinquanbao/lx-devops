#!groovy

import org.jenkinsci.plugins.workflow.libs.Library

@Library('jenkinsfile-util') _
import com.laoxin.pipeline.User

def printUserName(){
    def user = new User("jack")

    println user.getName()
}

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




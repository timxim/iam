#!/usr/bin/env groovy
@Library('sd')_

def kubeLabel = getKubeLabel()

def maybeArchiveJUnitReports(){
  def hasJunitReports = fileExists 'iam-login-service/target/surefire-reports'
  if (hasJunitReports) {
    junit '**/target/surefire-reports/TEST-*.xml'
    step( [ $class: 'JacocoPublisher' ] )
  }
}

pipeline {

  agent any

  options {
    ansiColor('xterm')
    buildDiscarder(logRotator(numToKeepStr: '5'))
    skipDefaultCheckout()
    timeout(time: 1, unit: 'HOURS')
    timestamps()
  }

  parameters {
    booleanParam(name: 'SKIP_TESTS', defaultValue: false, description: 'Skip tests')
    booleanParam(name: 'SKIP_DOCKER', defaultValue: false, description: 'Skip docker image creation')
    booleanParam(name: 'PUSH_TO_DOCKERHUB', defaultValue: false, description: 'Push to Dockerhub')
  }

  triggers { cron('@daily') }

  environment {
    DOCKER_REGISTRY_HOST = "${env.DOCKER_REGISTRY_HOST}"
    SONAR_USER_HOME = "${env.WORKSPACE}/.sonar"
  }

  stages {

    stage('build, test, package'){
      agent {
        kubernetes {
          label "${kubeLabel}"
          cloud 'Kube mwdevel'
          defaultContainer 'runner'
          inheritFrom 'ci-template'
          containerTemplate {
                name 'runner'
                image 'cloud-vm114.cloud.cnaf.infn.it/cnafsd/centos7-jenkins-slave:latest'
                ttyEnabled true
                command 'cat'
                resourceRequestCpu '500m'
                resourceLimitCpu '3'
                resourceRequestMemory '2500Mi'
                resourceLimitMemory '2500Mi'
          }
        }
      }

      stages {

        stage('checkout') {
          steps {
              deleteDir()
              checkout scm
              stash name: 'code', useDefaultExcludes: false
          }
        }

        stage('license-check') {
          steps {
              sh 'mvn license:check'
          }
        }

        stage('compile') {
          steps {
            sh 'mvn compile'
          }
        }

        stage('PR analysis'){
          when{
            allOf{
              expression{ env.CHANGE_URL }
              not {
                expression { return params.SKIP_TESTS }
              }
            }
          }
          steps {
            script{
              def tokens = "${env.CHANGE_URL}".tokenize('/')
              def organization = tokens[tokens.size()-4]
              def repo = tokens[tokens.size()-3]

              withCredentials([string(credentialsId: '630f8e6c-0d31-4f96-8d82-a1ef536ef059', variable: 'GITHUB_ACCESS_TOKEN')]) {
                withSonarQubeEnv('sonarcloud.io'){
                  sh """
                    mvn -B -U install sonar:sonar \\
                      -Dsonar.github.pullRequest=${env.CHANGE_ID} \\
                      -Dsonar.github.repository=${organization}/${repo} \\
                      -Dsonar.github.oauth=${GITHUB_ACCESS_TOKEN} \\
                      -Dsonar.host.url=${SONAR_HOST_URL} \\
                      -Dsonar.login=${SONAR_AUTH_TOKEN} \\
                      -Dsonar.branch.name=${BRANCH_NAME} \\
                      -Dsonar.branch.target=develop \\
                      -Dsonar.projectKey=indigo-iam_iam \\
                      -Dsonar.organization=indigo-iam
                  """
                }
              }
            }
          }

          post {
            always {
              script {
                maybeArchiveJUnitReports()
              }
            }
            unsuccessful {
              archiveArtifacts artifacts:'**/**/*.dump'
              archiveArtifacts artifacts:'**/**/*.dumpstream'
            }
          }
        }

        stage('analysis'){

          when{
            allOf{
              expression{ !env.CHANGE_URL }
              not {
                expression { return params.SKIP_TESTS }
              }
            }
          }

          steps {
            script{
              def checkstyle_opts = 'checkstyle:check -Dcheckstyle.config.location=google_checks.xml'

                withSonarQubeEnv('sonarcloud.io'){
                  sh """
                    mvn -U ${checkstyle_opts} \\
                    install sonar:sonar \\
                    -Dsonar.host.url=${SONAR_HOST_URL} \\
                    -Dsonar.login=${SONAR_AUTH_TOKEN} \\
                    -Dsonar.branch.name=${BRANCH_NAME} \\
                    -Dsonar.projectKey=indigo-iam_iam \\
                    -Dsonar.organization=indigo-iam
                  """
                }
            }
          }
          post {
            always {
              script {
                sh 'echo post analysis'
                maybeArchiveJUnitReports()
              }
            }
            unsuccessful {
              archiveArtifacts artifacts:'**/**/*.dump'
              archiveArtifacts artifacts:'**/**/*.dumpstream'
            }
          }
        }
        
        stage('quality-gate') {

          when{
            not {
              expression { return params.SKIP_TESTS }
            }
          }

          steps {
            timeout(time: 5, unit: 'MINUTES') {
              waitForQualityGate abortPipeline: true
            }
          }
        }

        stage('package') {
          steps {
            sh 'mvn -B -DskipTests=true clean package' 
            archiveArtifacts 'iam-login-service/target/iam-login-service.war'
            archiveArtifacts 'iam-login-service/target/classes/iam.version.properties'
            archiveArtifacts 'iam-test-client/target/iam-test-client.jar'
            stash includes: 'iam-login-service/target/iam-login-service.war,iam-login-service/target/classes/iam.version.properties,iam-test-client/target/iam-test-client.jar', name: 'iam-artifacts'
          }
        }
      }
    }

    stage('docker-images') {
      when{
        not {
          expression { return params.SKIP_DOCKER }
        }
      }

      agent {
        label "docker"
      }

      steps {
        deleteDir()
        unstash 'code'
        unstash 'iam-artifacts'
        sh '''
        sed -i -e 's#iam\\.version#IAM_VERSION#' iam-login-service/target/classes/iam.version.properties
        source iam-login-service/target/classes/iam.version.properties
        export IAM_LOGIN_SERVICE_VERSION="v${IAM_VERSION}"

        /bin/bash iam-login-service/docker/build-prod-image.sh
        /bin/bash iam-login-service/docker/push-prod-image.sh
        /bin/bash iam-test-client/docker/build-prod-image.sh
        /bin/bash iam-test-client/docker/push-prod-image.sh
        '''
        script {
          if (env.BRANCH_NAME == 'master' || params.PUSH_TO_DOCKERHUB ) {
            sh '''
            sed -i -e 's#iam\\.version#IAM_VERSION#' iam-login-service/target/classes/iam.version.properties
            source iam-login-service/target/classes/iam.version.properties
            export IAM_LOGIN_SERVICE_VERSION="v${IAM_VERSION}"
            unset DOCKER_REGISTRY_HOST
            /bin/bash iam-login-service/docker/push-prod-image.sh
            /bin/bash iam-test-client/docker/push-prod-image.sh
            '''
          }
        }
      }
    }
  }

  post {
    success {
      slackSend channel: "#iam", color: 'good', message: "${env.JOB_NAME} - #${env.BUILD_NUMBER} Success (<${env.BUILD_URL}|Open>)" 
    }

    unstable {
      slackSend channel: "#iam", color: 'danger', message: "${env.JOB_NAME} - #${env.BUILD_NUMBER} Unstable (<${env.BUILD_URL}|Open>)" 
    }

    failure {
      slackSend channel: "#iam", color: 'danger', message: "${env.JOB_NAME} - #${env.BUILD_NUMBER} Failure (<${env.BUILD_URL}|Open>)" 
    }
  }
}

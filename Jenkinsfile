pipeline {
  agent {
    docker {
      image 'maven:3.5.2-jdk-8'
    }
  }
  triggers {
    pollSCM '@hourly'
  }
  stages {
    stage('build and deploy') {
      steps {
        script {
          maven cmd: 'clean install -Dmaven.test.failure.ignore=true'
        }
      }
      post {
        success {
          junit '**/target/surefire-reports/**/*.xml'
        }
      }
    }
  }
}

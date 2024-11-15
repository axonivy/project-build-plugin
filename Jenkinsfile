pipeline {
  agent {
    dockerfile true
  }

  options {
    buildDiscarder(logRotator(numToKeepStr: '30', artifactNumToKeepStr: '10'))
  }

  triggers {
    cron '@midnight'
  }

  parameters {
    string(name: 'engineListUrl',
      description: 'Engine to use for build',
      defaultValue: 'https://product.ivyteam.io')
  }

  stages {
    stage('build') {
      steps {
        script {
          setupGPGEnvironment()
          withCredentials([string(credentialsId: 'gpg.password', variable: 'GPG_PWD')]) {
            def phase = isReleaseOrMasterBranch() ? 'deploy' : 'verify'
            maven cmd: "clean ${phase} " +
              "-Dgpg.skip=false " +
              "-Dgpg.project-build.password='${env.GPG_PWD}' " +
              "-Divy.engine.list.url=${params.engineListUrl} " +
              "-Dmaven.test.failure.ignore=true"
          }
          collectBuildArtifacts()
        }
      }
    }
    stage('deploy-site') {
      when {
        expression { isReleaseOrMasterBranch() }
      }
      steps {
        script {
          withEnv(['GIT_SSH_COMMAND=ssh -o StrictHostKeyChecking=no']) {
            sshagent(credentials: ['github-axonivy']) {
              def branch = "pages_$BRANCH_NAME-$BUILD_NUMBER"
              def message = "Update site (${env.BRANCH_NAME})"
              sh """ 
                git config --global user.name 'ivy-team'
                git config --global user.email 'info@ivyteam.ch'
              """
              maven cmd: "site site:stage scm-publish:publish-scm"
            }
          }
        }
      }
    }
  }
}

def isReleaseOrMasterBranch() {
  return env.BRANCH_NAME.startsWith('release/')  || env.BRANCH_NAME == 'master'
}

def setupGPGEnvironment() {
  withCredentials([file(credentialsId: 'gpg.keystore', variable: 'GPG_FILE')]) {
    sh "gpg --batch --import ${env.GPG_FILE}"
  }
}

def collectBuildArtifacts()  {
  archiveArtifacts 'target/*.jar'
  archiveArtifacts 'target/its/**/build.log'
  junit testDataPublishers: [[$class: 'AttachmentPublisher'], [$class: 'StabilityTestDataPublisher']], testResults: '**/target/surefire-reports/**/*.xml'
  recordIssues tools: [mavenConsole()], qualityGates: [[threshold: 1, type: 'TOTAL']], filters: [
    excludeType('site-maven-plugin:site'),
    excludeType('maven-surefire-plugin:test')
  ]
  recordIssues tools: [java()], qualityGates: [[threshold: 1, type: 'TOTAL']]
}

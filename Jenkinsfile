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
    booleanParam(name: 'skipGitHubSite',
      description: 'If checked the plugin documentation on GitHub will NOT be updated',
      defaultValue: true)

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
            def phase = isReleaseOrMasterBranch() ? 'deploy site-deploy' : 'verify'
            maven cmd: "clean ${phase} " +
              "-Dgpg.skip=false " +
              "-Dgpg.project-build.password='${env.GPG_PWD}' " +
              "-Dgithub.site.skip=${params.skipGitHubSite} " +
              "-Divy.engine.list.url=${params.engineListUrl} " +
              "-Dmaven.test.failure.ignore=true"
            if (isReleasingBranch()) {
              def version = sh (script: "mvn help:evaluate -Dexpression=project.version -q -DforceStdout", returnStdout: true)
              uploadBOM(projectName: 'project-build-plugin', projectVersion: version, bomFile: 'target/bom.json')
            }
          }
          if (env.BRANCH_NAME == 'master') {
            maven cmd: "sonar:sonar -Dsonar.host.url=https://sonar.ivyteam.io -Dsonar.projectKey=project-build-plugin -Dsonar.projectName=project-build-plugin"
          }
          collectBuildArtifacts()
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
    excludeType('sonar-maven-plugin:sonar'),
    excludeType('maven-surefire-plugin:test'),
    excludeType('cyclonedx:makeBom') 
  ]
  recordIssues tools: [eclipse()], qualityGates: [[threshold: 1, type: 'TOTAL']]
}

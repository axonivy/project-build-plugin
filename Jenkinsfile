pipeline {
  agent {
    dockerfile true
  }

  options {
    buildDiscarder(logRotator(numToKeepStr: '30', artifactNumToKeepStr: '10'))
  }

  triggers {
    pollSCM '@midnight'
  }

  parameters {
    booleanParam(name: 'skipGitHubSite',
      description: 'If checked the plugin documentation on GitHub will NOT be updated (ignored for release)',
      defaultValue: true)

    choice(name: 'deployProfile',
      description: 'Choose where the built plugin should be deployed to',
      choices: ['sonatype.snapshots', 'maven.central.release'])
  }

  stages {
    stage('snapshot build') {
      when {
        expression { params.deployProfile != 'maven.central.release' }
      }
      steps {
        script {
          setupGPGEnvironment()
          withCredentials([string(credentialsId: 'gpg.password', variable: 'GPG_PWD')]) {

            maven cmd: "clean deploy site-deploy " +
              "-P ${params.deployProfile} " +
              "-Dgpg.project-build.password='${env.GPG_PWD}' " +
              "-Dgpg.skip=false " +
              "-Dgithub.site.skip=${params.skipGitHubSite} "

          }
        }
        archiveArtifacts 'target/*.jar'
        junit '**/target/surefire-reports/**/*.xml'
      }
    }

    stage('release build') {
      when {
        branch '7.0'
        expression { params.deployProfile == 'maven.central.release' }
      }
      steps {
        script {
          setupGPGEnvironment()
          sh "git config --global user.email 'nobody@axonivy.com'"

          withCredentials([string(credentialsId: 'gpg.password', variable: 'GPG_PWD')]) {

            withEnv(['GIT_SSH_COMMAND=ssh -o StrictHostKeyChecking=no']) {
              sshagent(credentials: ['github-axonivy']) {
                maven cmd: "clean verify release:prepare release:perform " +
                  "-P ${params.deployProfile} " +
                  "-Dgpg.project-build.password='${env.GPG_PWD}' " +
                  "-Dgpg.skip=false " +
                  "-Dmaven.test.skip=true "
              }
            }

          }
        }
        archiveArtifacts 'target/*.jar'
        junit '**/target/surefire-reports/**/*.xml'
      }
    }
  }
}

def setupGPGEnvironment() {
  withCredentials([file(credentialsId: 'gpg.keystore', variable: 'GPG_FILE')]) {
    sh "gpg --batch --import ${env.GPG_FILE}"
  }
}

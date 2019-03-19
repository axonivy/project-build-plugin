pipeline {
  agent {
    dockerfile true
  }

  options {
    buildDiscarder(logRotator(numToKeepStr: '30', artifactNumToKeepStr: '10'))
  }

  triggers {
    pollSCM '@hourly'
    cron '@midnight'
  }

  parameters {
    booleanParam(name: 'skipGitHubSite',
      description: 'If checked the plugin documentation on GitHub will NOT be updated (ignored for release)',
      defaultValue: true)

    choice(name: 'engineListUrl',
      description: 'Engine to use for build',
      choices: ['http://zugprojenkins/job/ivy-core_product/job/master/lastSuccessfulBuild/',
                'http://zugprobldmas/job/Trunk_All/lastSuccessfulBuild/'])

    choice(name: 'deployProfile',
      description: 'Choose where the built plugin should be deployed to',
      choices: ['zugpronexus.snapshots', 'sonatype.snapshots', 'maven.central.release'])
  }

  stages {
    stage('release') {
      when {
        branch 'master release/*'
        expression { params.deployProfile == 'maven.central.release' }
      }
      steps {
        withCredentials([string(credentialsId: 'gpg.password', variable: 'GPG_PWD'),
                         file(credentialsId: 'gpg.keystore', variable: 'GPG_FILE')]) {

          script {
            def workspace = pwd()
            sh "gpg --batch --import ${env.GPG_FILE}"
            sh "git config --global user.email 'nobody@axonivy.com'"

            withEnv(['GIT_SSH_COMMAND=ssh -o StrictHostKeyChecking=no']) {
              sshagent(credentials: ['github-axonivy']) {
                maven cmd: "clean verify release:prepare release:perform " +
                  "-P ${params.deployProfile} " +
                  "-Dgpg.project-build.password='${env.GPG_PWD}' " +
                  "-Dgpg.skip=false " +
                  "-Dmaven.test.skip=true " +
                  "-Divy.engine.list.url=${params.engineListUrl} " +
                  "-Divy.engine.cache.directory=$workspace/target/ivyEngine"
              }
            }
          }
        }
        archiveArtifacts 'target/*.jar'
      }
      post {
        always {
          junit '**/target/surefire-reports/**/*.xml'
        }
      }
    }

    stage('build and deploy') {
      when {
        expression { params.deployProfile != 'maven.central.release' }
      }
      steps {
        withCredentials([string(credentialsId: 'gpg.password', variable: 'GPG_PWD'),
                         file(credentialsId: 'gpg.keystore', variable: 'GPG_FILE')]) {
          script {
            def workspace = pwd()
            sh "gpg --batch --import ${env.GPG_FILE}"

            maven cmd: "clean deploy site-deploy " +
              "-P ${params.deployProfile} " +
              "-Dgpg.project-build.password='${env.GPG_PWD}' " +
              "-Dgpg.skip=false " +
              "-Dgithub.site.skip=${params.skipGitHubSite} " +
              "-Divy.engine.list.url=${params.engineListUrl} " +
              "-Divy.engine.cache.directory=$workspace/target/ivyEngine"
            
            maven cmd: "sonar:sonar -Dsonar.host.url=http://zugprosonar"
          }
        }
        archiveArtifacts 'target/*.jar'
      }
      post {
        always {
          junit '**/target/surefire-reports/**/*.xml'
        }
      }
    }
  }
}

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
      description: 'If checked the plugin documentation on GitHub will NOT be updated (ignored for release)',
      defaultValue: true)

    string(name: 'engineListUrl',
      description: 'Engine to use for build',
      defaultValue: 'https://jenkins.ivyteam.io/job/core_product/job/master/lastSuccessfulBuild/')
  }

  stages {
    stage('build') {
      steps {
        script {
          setupGPGEnvironment()
          withCredentials([string(credentialsId: 'gpg.password', variable: 'GPG_PWD')]) {
            def phase = env.BRANCH_NAME == 'master' ? 'deploy site-deploy' : 'verify'
            maven cmd: "clean ${phase} " +
              "-Dgpg.project-build.password='${env.GPG_PWD}' " +
              "-Dgithub.site.skip=${params.skipGitHubSite} " +
              "-Divy.engine.list.url=${params.engineListUrl} " +
              "-Dmaven.test.failure.ignore=true"
          }
          if (env.BRANCH_NAME == 'master') {
            maven cmd: "sonar:sonar -Dsonar.host.url=https://sonar.ivyteam.io -Dsonar.projectKey=project-build-plugin -Dsonar.projectName=project-build-plugin"
          }
          collectBuildArtifacts()
        }
      }
    }
    
    /*
    stage('release build') {
      when {
        branch 'master'
        expression { params.deployProfile == 'maven.central.release' }
      }
      steps {
        script {
          def nextDevVersionParam = createNextDevVersionJVMParam()
          setupGPGEnvironment()
          sh "git config --global user.name 'ivy-team'"
          sh "git config --global user.email 'info@ivyteam.ch'"
          withCredentials([string(credentialsId: 'gpg.password', variable: 'GPG_PWD')]) {
            withEnv(['GIT_SSH_COMMAND=ssh -o StrictHostKeyChecking=no']) {
              sshagent(credentials: ['github-axonivy']) {
                maven cmd: "clean verify release:prepare release:perform " +
                  "-P ${params.deployProfile} " +
                  "${nextDevVersionParam} " +
                  "-Dgpg.project-build.password='${env.GPG_PWD}' " +
                  "-Dgpg.skip=false " +
                  "-Dmaven.test.skip=true " +
                  "-Darguments=-Divy.engine.list.url=${params.engineListUrl} "
              }
            }
          }
          collectBuildArtifacts()
        }
      }
    }
    */
  }
}

def setupGPGEnvironment() {
  withCredentials([file(credentialsId: 'gpg.keystore', variable: 'GPG_FILE')]) {
    sh "gpg --batch --import ${env.GPG_FILE}"
  }
}

def collectBuildArtifacts()  {
  archiveArtifacts 'target/*.jar'
  junit testDataPublishers: [[$class: 'AttachmentPublisher'], [$class: 'StabilityTestDataPublisher']], testResults: '**/target/surefire-reports/**/*.xml'
  recordIssues tools: [mavenConsole()], unstableTotalAll: 1, filters: [
    excludeType('site-maven-plugin:site'),
    excludeType('sonar-maven-plugin:sonar'),
    excludeType('maven-surefire-plugin:test')
  ]
  recordIssues tools: [eclipse()], unstableTotalAll: 1
}

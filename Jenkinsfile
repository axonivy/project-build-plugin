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

    string(name: 'engineListUrl',
      description: 'Engine to use for build',
      defaultValue: 'https://jenkins.ivyteam.io/job/ivy-core_product/job/release%252F8.0/lastSuccessfulBuild/')

    choice(name: 'deployProfile',
      description: 'Choose where the built plugin should be deployed to',
      choices: ['sonatype.snapshots', 'maven.central.release'])

    string(name: 'nextDevVersion',
      description: "Next development version used after release, e.g. '7.3.0' (no '-SNAPSHOT').\nNote: This is only used for release target; if not set next patch version will be raised by one",
      defaultValue: '' )
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

            def phase = env.BRANCH_NAME == '8.0' ? 'deploy site-deploy' : 'verify'
            maven cmd: "clean ${phase} " +
              "-P ${params.deployProfile} " +
              "-Dgpg.project-build.password='${env.GPG_PWD}' " +
              "-Dgpg.skip=false " +
              "-Dgithub.site.skip=${params.skipGitHubSite} " +
              "-Divy.engine.list.url=${params.engineListUrl} " +
              "-Dmaven.test.failure.ignore=true"

          }
        }
        archiveArtifacts 'target/*.jar'
        junit '**/target/surefire-reports/**/*.xml'
      }
    }
    
    stage('release build') {
      when {
        branch '8.0'
        expression { params.deployProfile == 'maven.central.release' }
      }
      steps {

        script {
          def nextDevVersionParam = createNextDevVersionJVMParam()
          setupGPGEnvironment()
          sh "git config --global user.name 'ivy-team'"
          sh "git config --global user.email 'nobody@axonivy.com'"
          
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
        }
        archiveArtifacts 'target/*.jar'
        junit testDataPublishers: [[$class: 'AttachmentPublisher'], [$class: 'StabilityTestDataPublisher']], testResults: '**/target/surefire-reports/**/*.xml'
      }
    }
  }
}

def createNextDevVersionJVMParam() {
  def nextDevelopmentVersion = '' 
  if (params.nextDevVersion.trim() =~ /\d+\.\d+\.\d+/) {
    echo "nextDevVersion is set to ${params.nextDevVersion.trim()}"
    nextDevelopmentVersion = "-DdevelopmentVersion=${params.nextDevVersion.trim()}-SNAPSHOT"
  } else {
    echo "nextDevVersion is NOT set or does not match version pattern - using default"
  }
  return nextDevelopmentVersion
}

def setupGPGEnvironment() {
  withCredentials([file(credentialsId: 'gpg.keystore', variable: 'GPG_FILE')]) {
    sh "gpg --batch --import ${env.GPG_FILE}"
  }
}
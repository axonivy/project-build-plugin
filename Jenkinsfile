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
    booleanParam(
      name: 'skipGitHubSite',
      description: 'If checked the plugin documentation on GitHub will NOT be updated (ignored for release)',
      defaultValue: true
    )

    choice(
      name: 'engineListUrl',
      description: 'Engine to use for build',
      choices: ['http://zugprojenkins/job/ivy-core_product/job/master/lastSuccessfulBuild/',
                'http://zugprobldmas/job/Trunk_All/lastSuccessfulBuild/']
    )
  }

  stages {
    stage('build') {      
      steps {
        script {
          setupGPGEnvironment()
          withCredentials([string(credentialsId: 'gpg.password', variable: 'GPG_PWD')]) {
            maven cmd: "clean deploy site-deploy " +              
              "-Dgpg.project-build.password='${env.GPG_PWD}' " +
              "-Dgpg.skip=false " +
              "-Dgithub.site.skip=${params.skipGitHubSite} " +
              "-Divy.engine.list.url=${params.engineListUrl} "
          }
          if (env.BRANCH_NAME == 'master') {
            maven cmd: "sonar:sonar -Dsonar.host.url=http://zugprosonar"
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

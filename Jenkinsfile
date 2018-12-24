pipeline {
  agent {
    dockerfile true
  }

  options {
    buildDiscarder(logRotator(artifactNumToKeepStr: '10'))
  }

  triggers {
    pollSCM '@hourly'
    cron '@midnight'
  }

  parameters {
    booleanParam(name: 'skipGitHubSite',
      description: 'If checked the plugin documentation on GitHub will NOT be updated',
      defaultValue: true)

    choice(name: 'engineSource',
      description: 'Engine to use for build',
      choices: 'Trunk_All\nLinux_Trunk_DesignerAndServer')

    choice(name: 'deployProfile',
      description: 'Choose where the built plugin should be deployed to',
      choices: 'zugpronexus.snapshots\nsonatype.snapshots\nmaven.central.release')
  }

  stages {
    stage('build and deploy') {
      steps {
        withCredentials([string(credentialsId: 'gpg.password', variable: 'GPG_PWD'), file(credentialsId: 'gpg.keystore', variable: 'GPG_FILE')]) {
          script {
            def workspace = pwd()
            sh "gpg --batch --import ${env.GPG_FILE}"

            maven cmd: "clean deploy site-deploy " +
              "-P ${params.deployProfile} " + 
              "-Dgpg.project-build.password='${env.GPG_PWD}' " +
              "-Dgpg.skip=false " +
              "-Dgithub.site.skip=${params.skipGitHubSite} " +
              "-Divy.engine.list.url=http://zugprobldmas/job/${params.engineSource}/lastSuccessfulBuild/ " +
              "-Divy.engine.cache.directory=$workspace/target/ivyEngine "
              "-Divy.engine.version=[6.1.1,]"
            
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

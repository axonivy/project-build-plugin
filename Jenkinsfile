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
    booleanParam(defaultValue: true, description: 'If checked the plugin documentation on GitHub will NOT be updated', name: 'skipGitHubSite')
    booleanParam(defaultValue: false, description: 'If checked the plugin does not sign the plugin', name: 'skipGPGSign')
    choice(choices: 'Trunk_All\nTrunk_DesignerAndServer\nLinux_Trunk_DesignerAndServer', description: 'Engine to use for build', name: 'engineSource')
    choice(choices: 'zugpronexus.snapshots\nsonatype.snapshots\nmaven.central.release', description: 'Choose where the built plugin should be deployed to', name: 'deployProfile')
  }

  stages {
    stage('build and deploy') {
      steps {
        withCredentials([string(credentialsId: 'gpg.password', variable: 'GPG_PWD'), file(credentialsId: 'gpg.keystore', variable: 'GPG_FILE')]) {
          script {
            def workspace = pwd()
            sh "gpg --batch --import ${env.GPG_FILE}"
            maven cmd: "clean deploy site-deploy -P ${params.deployProfile} -Dgpg.project-build.password='${env.GPG_PWD}' -Dgpg.skip=${params.skipGPGSign} -Dgithub.site.skip=${params.skipGitHubSite} -Divy.engine.list.url=http://zugprobldmas/job/${params.engineSource}/lastSuccessfulBuild/ -Divy.engine.cache.directory=$workspace/target/ivyEngine -Divy.engine.version=[6.1.1,]"
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

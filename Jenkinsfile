pipeline {
  agent {
    dockerfile {
        args '-t'
    }
  }

  triggers {
    pollSCM '@hourly'
    cron '@midnight'
  }

  parameters {
    booleanParam(defaultValue: true, description: 'If checked the plugin documentation on GitHub will NOT be updated', name: 'skipGitHubSite')
    booleanParam(defaultValue: true, description: 'If checked the plugin does not sign the plugin', name: 'skipGPGSign')
    choice(choices: 'Trunk_All\nTrunk_DesignerAndServer', description: 'Engine to use for build', name: 'engineSource')
    choice(choices: 'build\ncentral', description: 'Choose where the built plugin should be deployed to', name: 'deployProfile')
  }

  stages {
    stage('build and deploy') {
      steps {
        script {
          def workspace = pwd()
          configFileProvider([configFile(fileId: 'Axon-ivy_project-build-plugin_GPG-signing-key', variable: 'GPG_KEYRING')]) {
            sh "export GPG_TTY=\$(tty)"
            sh "base64 -d $GPG_KEYRING > gpg_keyring.gpg"
            sh "gpg --import gpg_keyring.gpg"
          }
          maven cmd: "clean deploy site-deploy -P ${params.deployProfile} -Dgpg.skip=${params.skipGPGSign} -Dgithub.site.skip=${params.skipGitHubSite} -Divy.engine.list.url=http://zugprobldmas/job/${params.engineSource}/lastSuccessfulBuild/ -Divy.engine.cache.directory=$workspace/target/ivyEngine -Divy.engine.version=[6.1.1,]"
        }
      }
      post {
        always {
          junit '**/target/surefire-reports/**/*.xml'
        }
      }
    }
  }
}

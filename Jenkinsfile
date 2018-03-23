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
    choice(choices: 'Trunk_All\nTrunk_DesignerAndServer', description: 'Engine to use for build', name: 'engineSource')
    choice(choices: 'zugpronexus.snapshots\nsonatype.snapshots\nmaven.central.release', description: 'Choose where the built plugin should be deployed to', name: 'deployProfile')
  }

  stages {
    stage('build and deploy') {
      steps {
        withCredentials([string(credentialsId: 'gpg.password', variable: 'gpg.project-build.password')]) {
        script {
          def workspace = pwd()
          configFileProvider([configFile(fileId: 'Axon-ivy_project-build-plugin_GPG-signing-key', variable: 'GPG_KEYRING')]) {
            sh "base64 -d $GPG_KEYRING > gpg_keyring.gpg"
            sh "gpg --batch --import gpg_keyring.gpg"
          }
          maven cmd: "clean deploy site-deploy -P ${params.deployProfile} -Dgpg.skip=${params.skipGPGSign} -Dgithub.site.skip=${params.skipGitHubSite} -Divy.engine.list.url=http://zugprobldmas/job/${params.engineSource}/lastSuccessfulBuild/ -Divy.engine.cache.directory=$workspace/target/ivyEngine -Divy.engine.version=[6.1.1,]"
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
    stage('release') {
      when {
        branch 'master'
        expression { params.deployProfile == 'maven.central.release' }
      }
      steps {
        script {
          // Create the new versions and SCM changes.
          maven cmd: "release:prepare -P ${params.deployProfile} -Darguments=\"-Divy.engine.version=[6.7.0,] -Divy.engine.list.url=http://zugprobldmas/job/Trunk_All/\""
          // Deploy to maven central.
          maven cmd: "release:perform -P ${params.deployProfile} -Darguments=\"-Divy.engine.version=[6.7.0,] -Divy.engine.list.url=http://zugprobldmas/job/Trunk_All/\""
        }
      }
    }
  }
}

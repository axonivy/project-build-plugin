pipeline {
  agent {
    dockerfile true
  }

  triggers {
    pollSCM '@hourly'
    cron '@midnight'
  }

  parameters {
    booleanParam(defaultValue: true, description: 'If checked the plugin documentation on GitHub will NOT be updated', name: 'skipGitHubSite')
    choice(choices: 'Trunk_All\nTrunk_DesignerAndServer', description: 'Engine to use for build', name: 'engineSource')
    choice(choices: 'build\ncentral', description: 'Choose where the built plugin should be deployed to', name: 'deployProfile')
  }

  stages {
    stage('build and deploy') {
      steps {
        script {
          def workspace = pwd()
          maven cmd: "clean deploy site-deploy -P ${params.deployProfile} -Dmaven.test.failure.ignore=true -Dgpg.skip=true -Dgithub.site.skip=${params.skipGitHubSite} -Divy.engine.list.url=http://zugprobldmas/job/${params.engineSource}/lastSuccessfulBuild/ -Divy.engine.cache.directory=$workspace/target/ivyEngine -Divy.engine.version=[6.1.1,]"
        }
      }
      post {
        success {
          junit '**/target/surefire-reports/**/*.xml'
        }
      }
    }
  }
}

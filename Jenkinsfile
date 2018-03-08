pipeline {
  agent {
    docker {
      image 'maven:3.5.2-jdk-8'
    }
  }

  triggers {
    pollSCM '@hourly'
  }

  parameters {
    booleanParam(defaultValue: false, description: 'If checked the plugin documentation on GitHub will NOT be updated', name: 'skipGitHubSite')
    choice(defaultValue: 'Trunk_All', choices: 'Trunk_All\nTrunk_DesignerAndServer', description: 'Engine to use for build', name: 'engineSource')
    text(defaultValue: 'Trunk_All', description: 'Hello message', name: 'helloMessage')
  }

  stages {
    stage('build and deploy') {
      steps {
        script {
          def workspace = pwd()
          maven cmd: 'clean install -Dmaven.test.failure.ignore=true -Dgpg.skip=true -Dgithub.site.skip=${params.skipGitHubSite} -Divy.engine.list.url=http://zugprobldmas/job/${params.engineSource}/lastSuccessfulBuild/ -Divy.engine.cache.directory=${workspace}/target/ivyEngine -Divy.engine.version=[6.1.1,]'
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

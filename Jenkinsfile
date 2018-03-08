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
    choice(choices: 'Trunk_All\nTrunk_DesignerAndServer', description: 'Engine to use for build', name: 'engineSource')
  }

  stages {
    stage('build and deploy') {
      steps {
        script {
          def workspace = pwd()
          maven cmd: 'clean install -Dmaven.test.failure.ignore=true -Dgpg.skip=true -Dgithub.site.skip=false -Divy.engine.list.url=http://zugprobldmas/job/Trunk_All/lastSuccessfulBuild/ -Divy.engine.cache.directory=./target/ivyEngine -Divy.engine.version=[6.1.1,]'
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

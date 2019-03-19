pipeline {
  agent {
    dockerfile true
  }

  options {
    buildDiscarder(logRotator(numToKeepStr: '30', artifactNumToKeepStr: '10'))
  }

  triggers {
    pollSCM '@midnight'
  }

  parameters {
    booleanParam(name: 'skipGitHubSite',
      description: 'If checked the plugin documentation on GitHub will NOT be updated',
      defaultValue: true)

    choice(name: 'deployProfile',
      description: 'Choose where the built plugin should be deployed to',
      choices: ['zugpronexus.snapshots', 'sonatype.snapshots', 'maven.central.release'])
  }

  stages {
    stage('release prepare') {
      when {
        expression { params.deployProfile == 'maven.central.release' }
      }
      steps {
        withCredentials([string(credentialsId: 'gpg.password', variable: 'GPG_PWD'),
                        file(credentialsId: 'gpg.keystore', variable: 'GPG_FILE'),
                        usernamePassword(credentialsId: 'sonatype.snapshots', usernameVariable: 'SONA_IVY_USER', passwordVariable: 'SONA_IVY_PWD'),
                        usernamePassword(credentialsId: 'github.ivy-team', usernameVariable: 'SONA_IVYTEAM_USER', passwordVariable: 'SONA_IVYTEAM_PWD')]) {

          script {
            def workspace = pwd()
            sh "gpg --batch --import ${env.GPG_FILE}"
            sh "git config user.email \"support@ivyteam.ch\""

            maven cmd: "clean verify release:prepare " +
              "-s settings.xml " +
              "-P ${params.deployProfile} " +
              "-Dgpg.project-build.password='${env.GPG_PWD}' " +
              "-Dgpg.skip=false " +
              "-Dgithub.site.skip=true " +
              "-Divy.engine.cache.directory=$workspace/target/ivyEngine"
          
          }
        }
      }
    }

    stage('build and deploy') {
      steps {
        withCredentials([string(credentialsId: 'gpg.password', variable: 'GPG_PWD'),
                        file(credentialsId: 'gpg.keystore', variable: 'GPG_FILE'),
                        usernamePassword(credentialsId: 'sonatype.snapshots', usernameVariable: 'SONA_IVY_USER', passwordVariable: 'SONA_IVY_PWD')]) {
          script {
            def workspace = pwd()
            sh "gpg --batch --import ${env.GPG_FILE}"

            maven cmd: "clean deploy site-deploy " +
              "-s settings.xml " +
              "-P ${params.deployProfile} " +
              "-Dgpg.project-build.password='${env.GPG_PWD}' " +
              "-Dgpg.skip=false " +
              "-Dgithub.site.skip=${params.skipGitHubSite} " +
              "-Divy.engine.cache.directory=$workspace/target/ivyEngine"
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

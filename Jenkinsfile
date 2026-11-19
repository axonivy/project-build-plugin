pipeline {
  agent {
    dockerfile true
  }

  options {
    buildDiscarder(logRotator(numToKeepStr: '30', artifactNumToKeepStr: '10'))
  }

  triggers {
    cron '@midnight'
  }

  parameters {
    booleanParam(name: 'dryRun', defaultValue: false, description: 'Whether the build should publish the SNAPSHOT. Non-release branches never publish.')
    string(name: 'qualifier', defaultValue: '', description: 'Optional version qualifier (e.g. \'m42\'). Will be appended with \'-SNAPSHOT\'.')
    string(name: 'engineSource', defaultValue: 'https://product.ivyteam.io', description: 'Engine page url.')
  }

  stages {
    stage('build') {
      steps {
        script {
          setupGPGEnvironment()
          withCredentials([string(credentialsId: 'gpg.password', variable: 'GPG_PWD')]) {
            def args = [
              '-Dgpg.skip=false',
              "-Dgpg.project-build.password='${env.GPG_PWD}'",
              "-Divy.engine.list.url=${params.engineSource}",
              '-Dmaven.test.failure.ignore=true'
            ]
            def qualifier = params.qualifier?.trim()
            if (qualifier) {
              applyVersionQualifier(qualifier)
            }
            if (params.dryRun) {
              args.add('-DaltDeploymentRepository=local::file:./target/deploy')
            }
            def phase = isReleasingBranch() ? 'deploy' : 'verify'
            maven cmd: "clean ${phase} " + args.join(' ')
            if (!params.dryRun && isReleasingBranch()) {
              def version = sh (script: "mvn help:evaluate -Dexpression=project.version -q -DforceStdout", returnStdout: true)
              uploadBOM(projectName: 'project-build-plugin', projectVersion: version, bomFile: 'target/bom.json')
            }
          }
          collectBuildArtifacts()
        }
      }
    }

    stage('deploy-site') {
      when {
        expression { !params.dryRun && isReleasingBranch() && currentBuild.changeSets.size() > 0 }
      }
      steps {
        script {
          withEnv(['GIT_SSH_COMMAND=ssh -o StrictHostKeyChecking=no']) {
            sshagent(credentials: ['github-axonivy']) {
              def branch = "pages_$BRANCH_NAME-$BUILD_NUMBER"
              def message = "Update site (${env.BRANCH_NAME})"
              sh """ 
                git config --global user.name 'ivy-team'
                git config --global user.email 'info@ivyteam.ch'
              """
              maven cmd: "site site:stage scm-publish:publish-scm"
            }
          }
        }
      }
    }
  }
}

def setupGPGEnvironment() {
  withCredentials([file(credentialsId: 'gpg.keystore', variable: 'GPG_FILE')]) {
    sh "gpg --batch --import ${env.GPG_FILE}"
  }
}

def collectBuildArtifacts()  {
  if (params.dryRun) {
    archiveArtifacts artifacts: 'target/deploy/**/*', allowEmptyArchive: true
  } else {
    archiveArtifacts 'target/*.jar'
  }
  archiveArtifacts 'target/its/**/build.log'
  archiveArtifacts artifacts: 'target/invoker-project-validation/**/build.log', allowEmptyArchive: true
  junit testDataPublishers: [[$class: 'AttachmentPublisher'], [$class: 'StabilityTestDataPublisher']], testResults: '**/target/surefire-reports/**/*.xml, target/invoker-reports/**/TEST-*.xml'
  recordIssues tools: [mavenConsole()], qualityGates: [[threshold: 1, type: 'TOTAL']], filters: [
    excludeType('site-maven-plugin:site'),
    excludeType('maven-surefire-plugin:test'),
    excludeType('cyclonedx:makeBom')
  ]
  recordIssues tools: [java()], qualityGates: [[threshold: 1, type: 'TOTAL']]
}

def applyVersionQualifier(String qualifier) {
  def currentVersion = sh(script: "mvn help:evaluate -Dexpression=project.version -q -DforceStdout", returnStdout: true).trim()
  def qualified  = currentVersion.replaceFirst(/-SNAPSHOT$/, "-${qualifier}-SNAPSHOT")
  echo "Using version '${qualified}' for this build."
  maven cmd: "versions:set -DnewVersion=${qualified} -DgenerateBackupPoms=false"
}

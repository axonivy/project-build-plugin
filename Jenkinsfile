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
    string(name: 'engineListUrl',
      description: 'Engine to use for build',
      defaultValue: 'https://product.ivyteam.io')
  }

  stages {
    stage('build') {
      steps {
        script {
          setupGPGEnvironment()
          withCredentials([string(credentialsId: 'gpg.password', variable: 'GPG_PWD')]) {
            def phase = 'deploy'
            maven cmd: "clean ${phase} " +
              "-Dgpg.skip=false " +
              "-Dgpg.project-build.password='${env.GPG_PWD}' " +
              "-Divy.engine.list.url=${params.engineListUrl} " +
              "-Dmaven.test.failure.ignore=true"
            if (isReleasingBranch()) {
              def version = sh (script: "mvn help:evaluate -Dexpression=project.version -q -DforceStdout", returnStdout: true)
              uploadBOM(projectName: 'project-build-plugin', projectVersion: version, bomFile: 'target/bom.json')
            }
          }
          collectBuildArtifacts()
        }
      }
    }

    stage('project-validation') {
      steps {
        script {
          dir('integration-tests/project-validation') {
            def expectedBlocks = [
              '''
              [WARNING] b.project - config/users.yaml: User 'Alex' is also defined in project 'main.project'.
              [WARNING] b.project - config/users.yaml: User 'Alex' is also defined in project 'a.project'.
              [WARNING] b.project - config/users.yaml: User 'Alex' is also defined in project 'c.project'.
              [WARNING] b.project - config/users.yaml: User 'Alex' is also defined in project 'd.project'.
              [WARNING] b.project - config/users.yaml: User 'Alex' is also defined in project 'standalone.project'.
              [ERROR] b.project - config/webservice-clients.yaml: The web service client key 'test' is not unique, it exists too in a not dependent project 'standalone.project'.
              '''.stripIndent().trim(),
              '''
              [WARNING] a.project - config/users.yaml: User 'Alex' is also defined in project 'main.project'.
              [WARNING] a.project - config/users.yaml: User 'Alex' is also defined in project 'b.project'.
              [WARNING] a.project - config/users.yaml: User 'Alex' is also defined in project 'c.project'.
              [WARNING] a.project - config/users.yaml: User 'Alex' is also defined in project 'd.project'.
              [WARNING] a.project - config/users.yaml: User 'Alex' is also defined in project 'standalone.project'.
              [ERROR] a.project - config/webservice-clients.yaml: The web service client key 'test' is not unique, it exists too in a not dependent project 'standalone.project'.

              '''.stripIndent().trim(),
              '''
              [WARNING] main.project - config/users.yaml: User 'Alex' is also defined in project 'a.project'.
              [WARNING] main.project - config/users.yaml: User 'Alex' is also defined in project 'b.project'.
              [WARNING] main.project - config/users.yaml: User 'Alex' is also defined in project 'c.project'.
              [WARNING] main.project - config/users.yaml: User 'Alex' is also defined in project 'd.project'.
              [WARNING] main.project - config/users.yaml: User 'Alex' is also defined in project 'standalone.project'.
              [ERROR] main.project - config/webservice-clients.yaml: The web service client key 'test' is not unique, it exists too in a not dependent project 'standalone.project'.

              '''.stripIndent().trim(),
              '''
              [WARNING] c.project - config/users.yaml: User 'Alex' is also defined in project 'main.project'.
              [WARNING] c.project - config/users.yaml: User 'Alex' is also defined in project 'a.project'.
              [WARNING] c.project - config/users.yaml: User 'Alex' is also defined in project 'b.project'.
              [WARNING] c.project - config/users.yaml: User 'Alex' is also defined in project 'd.project'.
              [WARNING] c.project - config/users.yaml: User 'Alex' is also defined in project 'standalone.project'.
              [ERROR] c.project - config/webservice-clients.yaml: The web service client key 'test' is not unique, it exists too in a not dependent project 'standalone.project'.

              '''.stripIndent().trim(),
              '''
              [WARNING] d.project - config/users.yaml: User 'Alex' is also defined in project 'main.project'.
              [WARNING] d.project - config/users.yaml: User 'Alex' is also defined in project 'a.project'.
              [WARNING] d.project - config/users.yaml: User 'Alex' is also defined in project 'b.project'.
              [WARNING] d.project - config/users.yaml: User 'Alex' is also defined in project 'c.project'.
              [WARNING] d.project - config/users.yaml: User 'Alex' is also defined in project 'standalone.project'.
              [ERROR] d.project - config/webservice-clients.yaml: The web service client key 'test' is not unique, it exists too in a not dependent project 'standalone.project'.

              '''.stripIndent().trim(),
              '''
              [WARNING] standalone.project - config/users.yaml: User 'Alex' is also defined in project 'main.project'.
              [WARNING] standalone.project - config/users.yaml: User 'Alex' is also defined in project 'a.project'.
              [WARNING] standalone.project - config/users.yaml: User 'Alex' is also defined in project 'b.project'.
              [WARNING] standalone.project - config/users.yaml: User 'Alex' is also defined in project 'c.project'.
              [WARNING] standalone.project - config/users.yaml: User 'Alex' is also defined in project 'd.project'.
              [ERROR] standalone.project - config/webservice-clients.yaml: The web service client key 'test' is not unique, it exists too in a not dependent project 'main.project'.
              [ERROR] standalone.project - config/webservice-clients.yaml: The web service client key 'test' is not unique, it exists too in a not dependent project 'a.project'.
              [ERROR] standalone.project - config/webservice-clients.yaml: The web service client key 'test' is not unique, it exists too in a not dependent project 'b.project'.
              [ERROR] standalone.project - config/webservice-clients.yaml: The web service client key 'test' is not unique, it exists too in a not dependent project 'c.project'.
              [ERROR] standalone.project - config/webservice-clients.yaml: The web service client key 'test' is not unique, it exists too in a not dependent project 'd.project'.
              '''.stripIndent().trim()
            ]

            def log = readFile('../../target/its/project-validation/build.log')
            def normalizedLog = log.replace('\r\n', '\n').replaceAll('\\u001B\\[[;\\d]*m', '')
            def missingBlocks = expectedBlocks.findAll { !normalizedLog.contains(it) }
            if (!missingBlocks.isEmpty()) {
              error "project-validation log misses expected block(s):\n\n" + missingBlocks.join("\n\n")
            }
          }
        }
      }
    }

    stage('deploy-site') {
      when {
        expression { isReleasingBranch() && currentBuild.changeSets.size() > 0 }
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
  archiveArtifacts 'target/*.jar'
  archiveArtifacts 'target/its/**/build.log'
  junit testDataPublishers: [[$class: 'AttachmentPublisher'], [$class: 'StabilityTestDataPublisher']], testResults: '**/target/surefire-reports/**/*.xml'
  recordIssues tools: [mavenConsole()], qualityGates: [[threshold: 1, type: 'TOTAL']], filters: [
    excludeType('site-maven-plugin:site'),
    excludeType('maven-surefire-plugin:test'),
    excludeType('cyclonedx:makeBom')
  ]
  recordIssues tools: [java()], qualityGates: [[threshold: 1, type: 'TOTAL']]
}

def assertProblems() {
  def log = readFile('target/its/project-validation/build.log')
  def normalizedLog = log.replace('\r\n', '\n').replaceAll('\\u001B\\[[;\\d]*m', '')
  def missingBlocks = expectedBlocks().findAll { !normalizedLog.contains(it) }
  if (!missingBlocks.isEmpty()) {
    error "project-validation log misses expected block(s):\n\n" + missingBlocks.join("\n\n")
  }
}

def expectedBlocks() {
  [
  '''
  [WARNING] config/users.yaml:[Alex] User 'Alex' is also defined in project 'main.project'.
  [WARNING] config/users.yaml:[Alex] User 'Alex' is also defined in project 'a.project'.
  [WARNING] config/users.yaml:[Alex] User 'Alex' is also defined in project 'c.project'.
  [WARNING] config/users.yaml:[Alex] User 'Alex' is also defined in project 'd.project'.
  [WARNING] config/users.yaml:[Alex] User 'Alex' is also defined in project 'standalone.project'.
  [ERROR] config/webservice-clients.yaml:[test] The web service client key 'test' is not unique, it exists too in a not dependent project 'standalone.project'.
  [INFO] Project validation finished with 2 files with findings
  [ERROR] ------------------------------------------------------------------------
  [ERROR] Project validation summary: b.project

  '''.stripIndent().trim(),
  '''
  [WARNING] config/users.yaml:[Alex] User 'Alex' is also defined in project 'main.project'.
  [WARNING] config/users.yaml:[Alex] User 'Alex' is also defined in project 'b.project'.
  [WARNING] config/users.yaml:[Alex] User 'Alex' is also defined in project 'c.project'.
  [WARNING] config/users.yaml:[Alex] User 'Alex' is also defined in project 'd.project'.
  [WARNING] config/users.yaml:[Alex] User 'Alex' is also defined in project 'standalone.project'.
  [ERROR] config/webservice-clients.yaml:[test] The web service client key 'test' is not unique, it exists too in a not dependent project 'standalone.project'.
  [INFO] Project validation finished with 2 files with findings
  [ERROR] ------------------------------------------------------------------------
  [ERROR] Project validation summary: a.project
  '''.stripIndent().trim(),
  '''
  [WARNING] config/users.yaml:[Alex] User 'Alex' is also defined in project 'a.project'.
  [WARNING] config/users.yaml:[Alex] User 'Alex' is also defined in project 'b.project'.
  [WARNING] config/users.yaml:[Alex] User 'Alex' is also defined in project 'c.project'.
  [WARNING] config/users.yaml:[Alex] User 'Alex' is also defined in project 'd.project'.
  [WARNING] config/users.yaml:[Alex] User 'Alex' is also defined in project 'standalone.project'.
  [ERROR] config/webservice-clients.yaml:[test] The web service client key 'test' is not unique, it exists too in a not dependent project 'standalone.project'.
  [INFO] Project validation finished with 2 files with findings
  [ERROR] ------------------------------------------------------------------------
  [ERROR] Project validation summary: main.project
  '''.stripIndent().trim(),
  '''
  [WARNING] config/users.yaml:[Alex] User 'Alex' is also defined in project 'main.project'.
  [WARNING] config/users.yaml:[Alex] User 'Alex' is also defined in project 'a.project'.
  [WARNING] config/users.yaml:[Alex] User 'Alex' is also defined in project 'b.project'.
  [WARNING] config/users.yaml:[Alex] User 'Alex' is also defined in project 'd.project'.
  [WARNING] config/users.yaml:[Alex] User 'Alex' is also defined in project 'standalone.project'.
  [ERROR] config/webservice-clients.yaml:[test] The web service client key 'test' is not unique, it exists too in a not dependent project 'standalone.project'.
  [INFO] Project validation finished with 2 files with findings
  [ERROR] ------------------------------------------------------------------------
  [ERROR] Project validation summary: c.project
  '''.stripIndent().trim(),
  '''
  [WARNING] config/users.yaml:[Alex] User 'Alex' is also defined in project 'main.project'.
  [WARNING] config/users.yaml:[Alex] User 'Alex' is also defined in project 'a.project'.
  [WARNING] config/users.yaml:[Alex] User 'Alex' is also defined in project 'b.project'.
  [WARNING] config/users.yaml:[Alex] User 'Alex' is also defined in project 'c.project'.
  [WARNING] config/users.yaml:[Alex] User 'Alex' is also defined in project 'standalone.project'.
  [ERROR] config/webservice-clients.yaml:[test] The web service client key 'test' is not unique, it exists too in a not dependent project 'standalone.project'.
  [INFO] Project validation finished with 2 files with findings
  [ERROR] ------------------------------------------------------------------------
  [ERROR] Project validation summary: d.project
  '''.stripIndent().trim(),
  '''
  [WARNING] config/users.yaml:[Alex] User 'Alex' is also defined in project 'main.project'.
  [WARNING] config/users.yaml:[Alex] User 'Alex' is also defined in project 'a.project'.
  [WARNING] config/users.yaml:[Alex] User 'Alex' is also defined in project 'b.project'.
  [WARNING] config/users.yaml:[Alex] User 'Alex' is also defined in project 'c.project'.
  [WARNING] config/users.yaml:[Alex] User 'Alex' is also defined in project 'd.project'.
  [ERROR] config/webservice-clients.yaml:[test] The web service client key 'test' is not unique, it exists too in a not dependent project 'main.project'.
  [ERROR] config/webservice-clients.yaml:[test] The web service client key 'test' is not unique, it exists too in a not dependent project 'a.project'.
  [ERROR] config/webservice-clients.yaml:[test] The web service client key 'test' is not unique, it exists too in a not dependent project 'b.project'.
  [ERROR] config/webservice-clients.yaml:[test] The web service client key 'test' is not unique, it exists too in a not dependent project 'c.project'.
  [ERROR] config/webservice-clients.yaml:[test] The web service client key 'test' is not unique, it exists too in a not dependent project 'd.project'.
  [INFO] Project validation finished with 2 files with findings
  [ERROR] ------------------------------------------------------------------------
  [ERROR] Project validation summary: standalone.project
  '''.stripIndent().trim()
  ]
}

return this

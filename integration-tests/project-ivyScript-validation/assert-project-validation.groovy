def logFile = new File(basedir, 'build.log')
if (!logFile.isFile()) {
  throw new FileNotFoundException("project-ivyScript-validation log not found: ${logFile}")
}

def normalizedLog = logFile.getText('UTF-8').replace('\r\n', '\n').replaceAll('\\u001B\\[[;\\d]*m', '')
def expectedBlocks = [
  '''
  [INFO] Project validation finished with no issues
  [INFO] ------------------------------------------------------------------------
  [INFO] Project validation summary: project-ivyScript-validation
  [INFO] ------------------------------------------------------------------------
  [INFO]   Errors:    0
  [INFO]   Warnings:  0
  [INFO]   Info:      0
  [INFO]   Total:     0
  '''.stripIndent().trim(),
]

def missingBlocks = expectedBlocks.findAll { !normalizedLog.contains(it) }
if (!missingBlocks.isEmpty()) {
  throw new IllegalStateException("project-ivyScript-validation log misses expected block(s):\n\n" + missingBlocks.join('\n\n'))
}

return true
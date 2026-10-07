def logFile = new File(buildLog)
if (!logFile.isFile()) {
  throw new FileNotFoundException("d-project validation log not found: ${logFile}")
}

def log = logFile.getText('UTF-8').replaceAll('\\u001B\\[[;\\d]*m', '')
if (!log.contains('Project validation summary: d.project')) {
  throw new IllegalStateException("d-project validation summary not found in log: ${logFile}")
}

def errorLines = log.readLines().findAll { it.startsWith('[ERROR]') }
if (!errorLines.isEmpty()) {
  throw new IllegalStateException("d-project Maven log contains [ERROR] output:\n" + errorLines.join('\n'))
}

return true
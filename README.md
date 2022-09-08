[![project-build-plugin version][0]][1] [![project-build-plugin snapshot version][2]][3] [![4]][5]

# Axon Ivy Project build plugin

Maven plugin for the automated building of Axon Ivy Projects. 

## Documentation

- [Examples and documentation](https://axonivy.github.io/project-build-plugin)
- Learn more about Axon Ivy Project building in the Designer Guide under [Concepts -> Continuous Integration](https://developer.axonivy.com/doc/latest/DesignerGuideHtml/ivy.concepts.html#ivy-ci-maven-plugin)

## Release new version

#### Preparation

1. Update the default engine version and check the minimal engine version need to be raised too: [AbstractEngineMojo](src/main/java/ch/ivyteam/ivy/maven/AbstractEngineMojo.java#L39).
1. Update the engine version in the [pom.xml](pom.xml#L454)
1. Update the README.md badge versions.

#### Release

1. Consider updating the default engine version in the [AbstractEngineMojo](src/main/java/ch/ivyteam/ivy/maven/AbstractEngineMojo.java#L40) 
2. Run the release build on Jenkins: https://jenkins.ivyteam.io/job/project-build-plugin/job/master/
	1. Toggle 'skip Github site': so that the plugin-docs wil be generated
	2. Switch the profile to 'maven.central.release' for a non snapshot public release
	3. Define the 'nextDevVersion' parameter, usually the current version +1

#### Post-Release

Wait until the maven central release is available: this may take several hours until it's fully distributed.

1. Run the `https://jenkins.ivyteam.io/view/jobs/job/github-repo-manager_raise-build-plugin-version/job/master/` pipeline with the new release/snapshot versions.
	1. Afterwards: merge the generated PRs on GitHub
2. Consider updating the default engine version in the [AbstractEngineMojo](src/main/java/ch/ivyteam/ivy/maven/AbstractEngineMojo.java#L40)

## License

The Apache License, Version 2.0

[0]: https://img.shields.io/badge/project--build--plugin-9.4.1-green
[1]: https://repo1.maven.org/maven2/com/axonivy/ivy/ci/project-build-plugin/
[2]: https://img.shields.io/badge/project--build--plugin-9.4.2--SNAPSHOT-yellow
[3]: https://oss.sonatype.org/content/repositories/snapshots/com/axonivy/ivy/ci/project-build-plugin/
[4]: https://img.shields.io/badge/-Documentation-blue
[5]: https://axonivy.github.io/project-build-plugin/release/

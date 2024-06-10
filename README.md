[![project-build-plugin version][0]][1] [![project-build-plugin snapshot version][2]][3] [![4]][5]

# Axon Ivy Project build plugin

Maven plugin for the automated building of Axon Ivy Projects. 

## Documentation

- [Examples and documentation](https://axonivy.github.io/project-build-plugin)
- Learn more about Axon Ivy Project building in the Designer Guide under [Concepts -> Continuous Integration](https://developer.axonivy.com/doc/latest/designer-guide/how-to/continuous-integration.html#maven-build-plugin)

## Release new version

#### Preparation

- Update the default engine version in:
	- [AbstractEngineMojo](src/main/java/ch/ivyteam/ivy/maven/AbstractEngineMojo.java#L42)
	- [pom.xml](pom.xml#L483)
- Raise the minimal engine version needs to be updated at least if you introduce a new minor or major version:
	- [AbstractEngineMojo](src/main/java/ch/ivyteam/ivy/maven/AbstractEngineMojo.java#L41)

#### Release

Since 9.4: Releasing is only possible on a release branch.

- Create a release branch if it does not exist yet (e.g. release/10.0)
- Run the [release build](build/release/Jenkinsfile) on the release branch
- Merge the Pull Request for next development iteration
- If you have created a new release branch, then manually raise the version on the master branch to the next major or minor version by executing the following command in the root of this project and make the steps in 'preperation' on the master branch :

```bash
mvn versions:set -DnewVersion=10.0.0-SNAPSHOT -DprocessAllModules -DgenerateBackupPoms=false
```
- If the master reflects a new relese-cycle; reflect it in the `versionPrefix` query parameter on the badges below.

#### Post-Release

Wait until the maven central release is available: this may take several hours until it's fully distributed.

- Publish the latest [draft release](https://github.com/axonivy/project-build-plugin/releases) do preserve the current changelog.
    - Select the tag which was created for this release by the release-pipeline
	- Verify that the title is correct
	- Set the release as 'latest release'
	- Publish it
- Raise project-build-plugin in other repos by triggering this [build](https://jenkins.ivyteam.io/view/jobs/job/github-repo-manager_raise-build-plugin-version/job/master/)
	- Merge the generated PRs on GitHub
- If you prepared for a new release train: update the default engine version in the [AbstractEngineMojo](src/main/java/ch/ivyteam/ivy/maven/AbstractEngineMojo.java#L42)
- Inform team-wawa @Teams to update to update Portal onto the latest project-build-plugin version!
- Raise ch.ivyteam.ivy.library.IvyProjectBuildPlugin.DEFAULT_VERSION in ivy core

## License

The Apache License, Version 2.0

[0]: https://img.shields.io/maven-metadata/v.svg?versionPrefix=11&label=central&logo=apachemaven&metadataUrl=https%3A%2F%2Frepo1.maven.org%2Fmaven2%2Fcom%2Faxonivy%2Fivy%2Fci%2Fproject-build-plugin%2Fmaven-metadata.xml
[1]: https://repo1.maven.org/maven2/com/axonivy/ivy/ci/project-build-plugin/
[2]: https://img.shields.io/maven-metadata/v?versionPrefix=11&label=dev&logo=sonatype&metadataUrl=https%3A%2F%2Foss.sonatype.org%2Fcontent%2Frepositories%2Fsnapshots%2Fcom%2Faxonivy%2Fivy%2Fci%2Fproject-build-plugin%2Fmaven-metadata.xml
[3]: https://oss.sonatype.org/content/repositories/snapshots/com/axonivy/ivy/ci/project-build-plugin/
[4]: https://img.shields.io/badge/-Documentation-blue
[5]: https://axonivy.github.io/project-build-plugin/release/

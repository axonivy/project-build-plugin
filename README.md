[![lts-version][lts] ![le]][releases] [![snapshot-version][snap]][snapshots] [![blue]][plugin-doc]

# Axon Ivy Project build plugin

Maven plugin for the automated building of Axon Ivy Projects.

## Documentation

- [Examples and documentation](https://axonivy.github.io/project-build-plugin)
- Learn more about Axon Ivy Project building in the Designer Guide under [Concepts -> Continuous Integration](https://developer.axonivy.com/doc/latest/designer-guide/how-to/continuous-integration.html#maven-build-plugin)

## Release new version

### Preparation

- Update the default engine version in:
  - [AbstractEngineMojo](src/main/java/ch/ivyteam/ivy/maven/AbstractEngineMojo.java#L45)
  - [pom.xml](pom.xml#L676)
- Raise the minimum engine version when introducing a new minor or major version:
  - [AbstractEngineMojo](src/main/java/ch/ivyteam/ivy/maven/AbstractEngineMojo.java#L44)

### Release

- Create a release branch if it does not exist yet (e.g. `release/13.2`).
- Run the [release build][release-build] on the respective release branch.
- Merge the pull request for the next development iteration.
- If you have created a new release branch, manually raise the version on the `master` branch to the next major or minor version:
  - Execute the following command in the root of this project (adjust version number accordingly):

    ```bash
    mvn versions:set -DnewVersion=15.1.0-SNAPSHOT -DprocessAllModules -DgenerateBackupPoms=false
    ```

  - Repeat the preparation steps on the `master` branch.
  - If this change reflects a major bump, update the `versionPrefix` query parameter in the badge URLs below.
  - Add the appropriate label (`major` or `minor`) to the pull request so that the release drafter creates a separate, versioned release draft for the `master` branch and tracks changes appropriately.

### Post Release

- Verify that the release is available on [Axon Ivy Nexus][axon-ivy-nexus].
- Raise `project-build-plugin` in consuming repos by triggering the [raise version job][raise-version-job].

## Authors

[ivyTeam](https://developer.axonivy.com/)

[![Axon Ivy](https://www.axonivy.com/hubfs/brand/axonivy-logo-black.svg)](http://www.axonivy.com)

## License

The Apache License, Version 2.0

[lts]: https://img.shields.io/maven-metadata/v.svg?versionPrefix=14&label=LTS&logo=apachemaven&metadataUrl=https%3A%2F%2Fnexus.axonivy.com%2Frepository%2Fmaven-releases%2Fcom%2Faxonivy%2Fivy%2Fci%2Fproject-build-plugin%2Fmaven-metadata.xml
[le]: https://img.shields.io/maven-metadata/v.svg?label=LE&logo=apachemaven&metadataUrl=https%3A%2F%2Fnexus.axonivy.com%2Frepository%2Fmaven-releases%2Fcom%2Faxonivy%2Fivy%2Fci%2Fproject-build-plugin%2Fmaven-metadata.xml
[releases]: https://nexus.axonivy.com/#browse/browse:maven-releases
[snap]: https://img.shields.io/maven-metadata/v.svg?label=dev&logo=apachemaven&metadataUrl=https%3A%2F%2Fnexus.axonivy.com%2Frepository%2Fmaven-snapshots%2Fcom%2Faxonivy%2Fivy%2Fci%2Fproject-build-plugin%2Fmaven-metadata.xml
[snapshots]: https://nexus.axonivy.com/#browse/browse:maven-snapshots
[blue]: https://img.shields.io/badge/-Documentation-blue
[plugin-doc]: https://axonivy.github.io/project-build-plugin/release/
[raise-version-job]: https://jenkins.ivyteam.io/view/jobs/job/github-repo-manager_raise-build-plugin-version/job/master/
[release-build]: https://jenkins.ivyteam.io/job/project-build-plugin_release/
[axon-ivy-nexus]: https://nexus.axonivy.com/

[![project-build-plugin version][0]][1] [![project-build-plugin snapshot version][2]][3] [![4]][5]

# Axon Ivy Project build plugin

Maven plugin for the automated building of Axon Ivy Projects. 

## Documentation

- [Examples and documentation](https://axonivy.github.io/project-build-plugin)
- Learn more about Axon Ivy Project building in the Designer Guide under [Concepts -> Continuous Integration](https://developer.axonivy.com/doc/latest/DesignerGuideHtml/ivy.concepts.html#ivy-ci-maven-plugin)

## Release new version

- Update the default engine version and check the minimal engine version need to be raised too: [AbstractEngineMojo](src/main/java/ch/ivyteam/ivy/maven/AbstractEngineMojo.java#L39).
- Update the engine version in the [pom.xml](pom.xml#L454)
- Update the Readme badge versions.
- Run the `project-build-plugin` pipeline with the `maven.central.release` profile.
- Run the `ivy-core_release-raise-project-build-plugin-version` pipeline with the new release/snapshot versions.

## License

The Apache License, Version 2.0

[0]: https://img.shields.io/badge/project--build--plugin-9.3.1-green
[1]: https://repo1.maven.org/maven2/com/axonivy/ivy/ci/project-build-plugin/
[2]: https://img.shields.io/badge/project--build--plugin-9.4.0--SNAPSHOT-yellow
[3]: https://oss.sonatype.org/content/repositories/snapshots/com/axonivy/ivy/ci/project-build-plugin/
[4]: https://img.shields.io/badge/-Documentation-blue
[5]: https://axonivy.github.io/project-build-plugin/release/

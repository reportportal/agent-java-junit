# Changelog

## [Unreleased]
### Changed
- Client version updated on [5.1.22](https://github.com/reportportal/client-java/releases/tag/5.1.22), by @HardNorth
- JUnit-Foundation updated on [17.0.3](https://github.com/sbabcoc/JUnit-Foundation/releases/tag/junit-foundation-17.0.3), by @HardNorth

## [5.1.2]
### Changed
- Client version updated on [5.1.16](https://github.com/reportportal/client-java/releases/tag/5.1.16), by @HardNorth
- JUnit-Foundation updated on [17.0.2](https://github.com/sbabcoc/JUnit-Foundation/releases/tag/junit-foundation-17.0.2), by @HardNorth
- `commons-text` transitive dependency was forcibly updates to avoid critical vulnerability, by @HardNorth

## [5.1.1]
### Added
- Test Case ID templating, by @HardNorth
### Changed
- Client version updated on [5.1.9](https://github.com/reportportal/client-java/releases/tag/5.1.9), by @HardNorth
- JUnit-Foundation updated on [15.3.5](https://github.com/sbabcoc/JUnit-Foundation/releases/tag/junit-foundation-15.3.5), by @HardNorth
- Slf4j version updated on 1.7.36, by @HardNorth

## [5.1.0]
### Added
- Class level `@Attributes` annotation support
### Changed
- Version promoted to stable release
- Client version updated on [5.1.0](https://github.com/reportportal/client-java/releases/tag/5.1.0)
- JUnit-Foundation updated on [15.3.4](https://github.com/sbabcoc/JUnit-Foundation/releases/tag/junit-foundation-15.3.4)

## [5.1.0-RC-4]
### Added
- Service location step in documentation
### Removed
- Service location file

## [5.1.0-RC-3]
### Added
- `ReportPortalListener.buildFinishStepRq(Object, FrameworkMethod, ReflectiveCallable, ItemStatus)` method

## [5.1.0-RC-2]
### Changed
- Client version updated on [5.1.0-RC-12](https://github.com/reportportal/client-java/releases/tag/5.1.0-RC-12)
### Fixed
- ItemTree retrieve by test Description

## [5.1.0-RC-1]
### Changed
- Client version updated on [5.1.0-RC-6](https://github.com/reportportal/client-java/releases/tag/5.1.0-RC-6)
- Version changed on 5.1.0

## [5.1.0-ALPHA-1]
### Changed
- Client version updated on [5.1.0-ALPHA-5](https://github.com/reportportal/client-java/releases/tag/5.1.0-ALPHA-5)

## [5.0.0]
### Added
- JUnit Theories support (special thanks to Scott Babcock and [JUnit Foundation](https://github.com/sbabcoc/JUnit-Foundation))
- Powermock support (special thanks to Scott Babcock and [JUnit Foundation](https://github.com/sbabcoc/JUnit-Foundation))
### Changed
- JUnit-Foundation updated on [12.5.3](https://github.com/sbabcoc/JUnit-Foundation/releases/tag/junit-foundation-12.5.3)
- Client version updated on [5.0.18](https://github.com/reportportal/client-java/releases/tag/5.0.18)
- Parent Item status evaluation was moved into the client
- "Not Issue" issue moved into the client

## [5.0.0-RC-1]
### Added
- Manual Step Reporting feature
### Fixed
- A test status switch from PASSED to FAILED in case of an `@After` method failed
- A test status when AssumptionViolatedException was thrown in `@Before` method
### Known issues
- JUnit Theories feature is not functioning due to upstream library exception failures:
  [JUnit-Foundation #78](https://github.com/sbabcoc/JUnit-Foundation/issues/78)
- PowerMock library is not supported due to upstream library incompatibility (issue #66).
  Upstream library issue: [JUnit-Foundation #77](https://github.com/sbabcoc/JUnit-Foundation/issues/77)

## [5.0.0-BETA-17]
### Fixed
- A test status when AssumptionViolatedException was thrown. Now it marks as 'SKIPPED' (issue #63)

## [5.0.0-BETA-16]
### Added
- JUnit Categories now handled as RP tags
### Fixed
- Callback reporting feature
- ExpectedException Rule handling (issue #64)
### Changed
- Client version updated on [5.0.15](https://github.com/reportportal/client-java/releases/tag/5.0.15)

## [5.0.0-BETA-15]
### Added
- Handling of standard and JUnitParams parameters without `ArtifactParams` interface
### Fixed
- parameterized Test Case ID generation for standard parameters (`@ParameterKey` annotation handling)
- 'retrieveLeaf' and 'stopRunner' methods do not start new launch in case of empty suites / tests
- 'stopLaunch' method does not start new launch in case of there was no any tests passed
### Changed
- Client version updated on [5.0.14](https://github.com/reportportal/client-java/releases/tag/5.0.14)
- Class type item level was changed from 'SUITE' to 'TEST'

## [5.0.0-BETA-14]
### Added
- A skipped test method reporting in case of a `@Before` method failed
### Fixed
- A null-pointer exception on `@Before` methods
### Changed
- ParallelRunningHandler class was deleted due to redundancy, all its logic moved to ReportPortalListener class

## [5.0.0-BETA-13]
### Added
- A test retry feature which is based on corresponding JunitFoundation's
  [feature](https://github.com/sbabcoc/JUnit-Foundation#automatic-retry-of-failed-tests).
- `@Ignore` annotation support
### Changed
- JunitFoundation version was updated on 12.3.0

## [5.0.0-BETA-12]
### Changed
- Parameters extraction logic moved into the client, now it works in the same way as for other agents
- Test Case ID logic moved into the client, now it also works in the same way as for other agents
- Code reference generation logic moved into client
- Test reporting structure now conforms with other agents
- Client version updated on [5.0.13](https://github.com/reportportal/client-java/releases/tag/5.0.13)
### Removed
- Some protected methods for extensions implementation were removed due to their redundancy. Alternative methods for extensions will be 
  added in further implementation.

## [5.0.0-BETA-11]


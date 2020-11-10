# Changelog

## [Unreleased]
### Added
- Handling of standard and JUnitParams parameters without `ArtifactParams` interface
### Fixed
- 'retrieveLeaf' and 'stopRunner' methods do not start new launch in case of empty suites / tests
- 'stopLaunch' method does not start new launch in case of there was no any tests passed
### Changed
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


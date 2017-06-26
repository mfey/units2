# Changelog
All notable changes to this project should be documented in this file. They probably won't be, but they should.

## [2.7 unreleased] - some date in the (near?) future
### Added
- This changelog. This and previous log entries will be disappointing.
- IEC binary prefixes and bit/octet units.
### Changed
- ops are now (re)defined with decorator functions instead of macros to allow more generality.
- the way modular arithmetic with units is done in `ceil`, `floor`, `round`, has improved.
### Removed
- A dynamic boolean allowing unsafe, hack-ish ops was removed. Unsurprising behaviour is now mandatory rather than default.

## [2.6] - 17th Feb 2017
### Added
- `from` added to core interfaces.
- various experimental interactions with clojures' specs.
### Changed
- calculus is now (re)defined with decorator functions to allow more generality.

## [2.5] - 5th May 2016

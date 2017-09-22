# Changelog
All notable changes to this project should be documented in this file. They probably won't be, but they should.

As of v.2.8, the API of `units2` should be stable enough to adopt a ["no-breaking-changes" policy](https://www.youtube.com/watch?v=oyLBGkS5ICk). You should *never* have any reason *not* to use the most recent version of `units2`.

## 2.7 - 22nd Sept 2017
### Added
- This changelog. This and previous log entries will be disappointing.
- IEC binary prefixes and bit/octet units.
- Support for rational exponents in dimensional analysis (via `root` in Multiplicative protocol)
- ops/sqrt
### Changed
- the brand slogan is now "a Clojure library for units of measurement", this improves searchability / SEO.
- ops are now (re)defined with decorator functions instead of macros to allow more generality / expose this functionality to the user.
- the name of the standard units library is now `units2.stdlib` instead of the historical `*.astro`.
- the preferred name for `unit-from-powers` is now `parse-unit`, though the old name is still provided.
- the way modular arithmetic with units is done in `ceil`, `floor`, `round`, has improved.
- `units2` no longer provides wrapped versions of the SI/NonSI units defined by `org.jscience`, instead defining units from scratch.
### Removed
- A dynamic boolean allowing unsafe, surprising behaviour was removed. Unsurprising behaviour is now mandatory rather than default. Removed for sanity.
- units2 no longer depends on incanter/apache for calculus. If the naive builtins aren't good enough, users probably want to decorate their own functions anyway. Removed to prevent feature creep and unnecessary dependency on other libraries.

## [2.6] - 17th Feb 2017
### Added
- `from` added to core interfaces.
- various experimental interactions with clojures' specs.
### Changed
- calculus is now (re)defined with decorator functions to allow more generality.

## [2.5] - 5th May 2016

# units2 -- design

## Why units2?

Quantitative domain-specific problems typically involve domain-specific units of measurement. My scientific research had me commenting and converting and juggling units manually for a while. My code was getting ugly. I decided there must be a better way: I should be writing *regular Clojure code* that deals with units automatically and behind-the-scenes.

There were already unit libraries on offer: `frinj`, `minderbinder`, `meajure`, and what I guess are a few abandonned personal projects. However, they didn't do what I wanted. `frinj` and `minderbinder` can do unit conversions but can't do maths (and their DSLs are very unLISPy). `meajure` can do maths, but not conversions (and doesn't do dimensional analysis, either).

Fortunately, there was a well-designed implementation of units in the Java standard library: JSR-363 (revised JSR-275) had been accepted a few months prior as `javax.measure`. So, I wrote some Java interop, cleaned up my code, and did my science. My wrapper code coevolved along with my science code -- the science code got cleaner and cleaner as the `units` code got uglier and uglier. Eventually, a complete rewrite was necessary. To avoid breaking my science code, I copied `units` into a folder called `units2`.

## Design Choices

### IFnUnits with `deftype` instead of Clojure's builtin data structures.
There are two obvious way to add units to clojure: some builtin data structure or metadata. TODO: DISCUSS

### Rational exponents in dimensional analysis
While trying to justify why rational exponents weren't supported (in favour of just integers), I convinced myself it's probably OK to have them as long as these are only accessible through `(ops/expt ... [rational?])` and explicit construction via the `power` and `root` of `Multiplicative`. Most of the time users won't even notice the extra freedom, but when they want it they'd be upset to find it isn't there.

That said, there are good *conceptual* reasons to try to stick to integer exponents. TODO: DISCUSS

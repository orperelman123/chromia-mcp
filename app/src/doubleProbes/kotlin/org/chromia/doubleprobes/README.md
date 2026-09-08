# The eight round-18 evasion probes

This source set is **compiled and never run**. Nothing here is a test, nothing
here is on the test runtime classpath, and nothing in `app/src/test/kotlin`
imports it.

Adversary round 18 (section 6) wrote eight candidate test doubles the way a
person in a hurry writes one and ran `NoTestDoublesTest`'s own detectors,
ported verbatim, over them: **six of the eight were invisible**, because every
detector was a source regex and every one of the six was one token away from the
spelling it looked for - `by` delegation between the supertype and the brace, a
subclass of a production CLASS rather than one of the listed interfaces, an
import alias, a supertype on the next line over an unlisted seam, a
`java.lang.reflect.Proxy` whose substitute type is not in the source at all, and
a SAM lambda on a seam the lambda list omitted.

The scan is now STRUCTURAL: it reads the compiled classes, where `by` delegation,
nesting, aliases and multi-line supertypes are all the same thing. These eight
files are what proves it, so they have to be **compiled**, and a double compiled
into the test tree would still be a double in the suite. Hence a source set of
their own: `NoTestDoublesTest` asserts zero structural sites in
`app/build/classes/kotlin/test` and asserts that every one of these eight is
caught in `app/build/classes/kotlin/doubleProbes`.

`d7` is the CONTROL - the shape the old `ANONYMOUS` regex was written for. It
must be caught by the lexical layer as well as the structural one; each of the
other seven is one token away from it.

If you are looking for a place to put a double: there isn't one. These exist to
make the scan's silence mean something, and the assertion over the real test tree
is still zero.

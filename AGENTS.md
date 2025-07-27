# `AGENTS.md`

## Problem-Solving Preferences

* If the requirements seem vague, follow whichever course of you believe will increase the value of the codebase the most
* If ever stuck "bashing your head against the wall", perform a quick experiment to reestablish "ground truth"

---

## Architectural Style

* Precision over convenience
* Immutable over mutable
* Composition over inheritance
* Domain modeling over mechanical translation
* Flatter organization
* Fewer classes
* Fewer packages
* Fewer dependencies

---

## Coding Style

* Aim for **high code density**
  * Optimize amount of useful information held within one cone of vision
* Precisely name all parts worth naming (self-documenting code)
  * Reserve comments for clarifying only the trickiest of situations
  * Ok to use `_` when necessary
* Leverage types to make illegal states unrepresentable and valid operations obvious
* Write simple/typed/strict/idiomatic/standard/modern Java, up to and including `24`
  * `Optional` is restricted to nullable return values *only*
  * `null` is banned in nearly all circumstances
  * `Object` is banned
  * Unchecked operations are banned
  * Leverage `instanceof` to distinguish type and test for `null` in a single statement
* Ask "What domain states does this represent?" before "How do I map this field?"
* Each nullable/optional field represents distinct business states → create explicit types
* Prefer impossible-to-misuse APIs over convenient-to-implement ones
* Avoid `Optional` in data models - use polymorphism, defaults, or sentinel values instead
  * `Optional` signals computation uncertainty, not data optionality
* Avoid approaches that make analysis difficult
  * `java.lang.reflect` is banned
  * `javax.annotation` is banned
* Pull out stateless calculations into a `private static` when sufficiently complex or duplicated
  * `::` looks, feels nice
* Isolate each dependency's usage to a single spot on the periphery of the codebase

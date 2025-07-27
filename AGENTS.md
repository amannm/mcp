# `AGENTS.md`

## Problem-Solving Preferences

* If the requirements seem vague, follow whichever course of you believe will increase the value of the codebase the most
* If ever stuck "bashing your head against the wall", perform a quick experiment to reestablish "ground truth"

---

## Coding Preferences

* Follow latest Java language recommended practices, up to and including `24`
* Aim for **high code density**
  * Optimize for amount of valuable information held within one human field of view (their visual context)
* Spell out and precisely name all things worth naming (self-documenting code)
  * Ok to use `_` when necessary
  * Reserve comments for clarifying only the trickiest of situations
* Pull out stateless calculations into a `private static` when sufficiently complex or duplicated
  * `::` looks and feels nice
* Seal the codebase off from `null` values
  * Leverage `Optional<T>` on method return types and record parameter types
* Avoid approaches requiring `Object` type-erasure or unchecked casts or annotation processing or reflection
  * Favor approaches amenable to static analysis and properly specified, typed, strict, idiomatic, standard, modern Java

---

## Design Preferences
* Prefer composition
* Prefer fewer classes
* Prefer fewer packages
* Prefer flatter organization
* Prefer fewer dependencies
* Isolate each dependency's usage to a single spot on the periphery of the codebase
* Select dependencies based on trajectory of and proximity to capital investment
  * Avoid legacy ecosystems/middleware/frameworks/utils/networks like Spring, OSGi, Guava, Gson, Jackson
  * Prefer use of `jakarta` (formerly `javax`) reference implementations/libraries

---

## Useful Commands

* `gradle` for building projects
* `javap` for analyzing compiled artifacts
* `jdb` for debugging running applications
* `jcmd` for platform diagnostics
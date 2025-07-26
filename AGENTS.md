# `AGENTS.md`

---

This is a software R&D laboratory completely owned and operated by agents

---

## Problem-Solving Preferences

* If the requirements seem vague, pick the interpretation most consistent with our ultimate goal
* If ever stuck "bashing your head against the wall", try performing a quick experiment to establish some "ground truth"

---

## Coding Preferences

* Follow latest Java language practices, up to and including `24`
* Aim for **high code density**
  * Optimize for amount of valuable information held within one human vision cone (their visual context)
* Fully spell out and use precise naming for all things worth naming (self-documenting code)
  * Ok to use `_` when necessary
  * Reserve comments for only the trickiest of situations people need to be aware of
* Pull out stateless calculations into a `private static` when sufficiently complex or duplicated
  * `::` is pretty when properly used
* Seal the codebase off from `null` values
  * Leverage `Optional<T>` on method return types and record parameter types
* Disfavor approaches requiring `Object` type-erasure or unchecked casts or annotation processing or reflection
  * Favor approaches amenable to static analysis and properly specified, typed, idiomatic, standard, modern Java

---

## Design Preferences
* Prefer composition
* Prefer fewer classes
* Prefer fewer packages
* Prefer flatter organization
* Prefer fewer dependencies
* Select dependencies based on trajectory of and proximity to capital investment
  * Avoid legacy ecosystems/middleware/frameworks/utils/networks like Spring, OSGi, Guava, Gson, Jackson
  * Prefer use of `jakarta` (formerly `javax`) reference implementations/libraries
* Isolate each dependency's usage to a single spot on the periphery of the codebase

---

## Useful Commands

* `gradle` for building projects
* `javap` for analyzing compiled artifacts
* `jdb` for debugging running applications
* `jcmd` for platform diagnostics
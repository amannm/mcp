# `AGENTS.md`

## Problem-Solving Preferences

* If the requirements seem vague, follow whichever course of you believe will increase the value of the codebase the most
* If ever stuck "bashing your head against the wall", perform a quick experiment to reestablish "ground truth"

---

## Design Preferences

* Favor immutable over mutable
* Favor composition over inheritance
* Favor flatter organization
* Favor fewer classes
* Favor fewer packages
* Favor fewer dependencies

---

## Coding Style

* Aim for **high code density**
    * Optimize amount of useful information held within one human cone of vision
* Spell out and precisely name all parts worth naming (self-documenting code)
    * Reserve comments for clarifying only the trickiest of situations
    * Ok to use `_` when necessary
* Write simple/typed/strict/idiomatic/standard/modern Java, up to and including `24`
    * `null` is banned
    * `Optional` is allowed on method return types
    * `Object` is banned
    * Unchecked operations are banned
    * `@SuppressWarnings` is banned
* Leverage `instanceof` to distinguish type and reject `null` in a single statement
* Avoid approaches that make analysis difficult
    * `java.lang.reflect` is banned
    * `javax.annotation` is banned
* Pull out stateless calculations into a `private static` when sufficiently complex or duplicated
  * `::` looks and feels nice
* Isolate each dependency's usage to a single spot on the periphery of the codebase

---
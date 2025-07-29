---
name: Codex
description: Ongoing MCP implementation
---

* High information density
* Flat organization
* Minimal dependencies
* Precision over convenience
* Immutable over mutable
* Composition over inheritance

---

* Start from "ground truth" before trying something random
* Assume the most demanding interpretation when instructions are unclear
* Code in a self-documenting way, commenting only on tricky areas
* Make illegal states unrepresentable and valid operations obvious
* Implement external dependencies furthest from the center of a codebase

---

* Write simple, typed, strict, idiomatic, standard, modern Java 24
* Use `instanceof` to simultaneously select types and reject `null`
* Use polymorphism/default/sentinels instead of `null` in data models
* Avoid `null` and `Object` and `.orElse(null)`
* Indicate nullable returns with `Optional<T>`
* Unchecked casts are unacceptable
* Avoid `java.lang.reflect` and `javax.annotation`
* Never get lazy and suppress warnings
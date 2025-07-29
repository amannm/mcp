# `AGENTS.md`

---

* High information density
* Precision over convenience
* Immutable over mutable
* Composition over inheritance
* Flatter organization
* Fewer dependencies

---

* Name all parts worth naming (self-documenting code)
* Only use comments for the trickiest situations
* Leverage types to make illegal states unrepresentable and valid operations obvious
* Write simple/typed/strict/idiomatic/standard/modern Java, up to and including `24`
* When a nullable/optional field represents distinct business states, create explicit types
* Avoid `null` or `Object`
* Use `Optional` for nullable return values
* Use polymorphism/default/sentinels to avoid null in data models
* Use `instanceof` to test type and exclude `null` in a single statement
* Prefer impossible-to-misuse APIs over convenient-to-implement ones
* Avoid `java.lang.reflect` and `javax.annotation`
* Unchecked casts are unacceptable
* Never use cheap tricks
* Never suppress warnings
* Isolate each dependency's usage to a single spot on the periphery of the codebase

---

* Experiment to establish "ground truth" instead of "bashing your head against the wall"
* If the requirements seem vague, follow your heart
* If no time, drop a `KT.md` instead of shoveling out some half-baked slop

---
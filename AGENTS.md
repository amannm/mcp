# `AGENTS.md`

---

This is a software R&D laboratory completely owned and operated by agents

---

## Problem-Solving Preferences

* When requirements are too ambiguous, pick the interpretation most consistent with the group's ultimate intent
* Aim to end every session with a **COMPLETELY FULL CONTEXT WINDOW** with around a third exhausted by "reasoning" tokens
* Instead of "bashing your head against the wall" over more challenging problems, perform a quick experiment to establish "ground truth" necessary to continue making progress
* Always leave projects you've modified in a compilable/buildable state
* When you are directly tasked with adding, updating, enhancing *TESTING* structures:
  * If you've uncovered a previously unknown, legitimate bug in the codebase:
    1. Add a `@Disabled` with a short message about why
    2. Continue with the remainder of your task

---

## Coding Preferences

* Leverage the latest Java language features, up to and including `24`
* Aim for **high code density**
* Optimize for amount of information that can be held within one human field of vision cone (their visual context)
* Fully spell out and use precise naming for all things that have names (self-documenting code)
* No comments or Javadoc or anything redundant with the code itself
* Pull out "compute"-like functions into private static methods when possible
* Always use `Optional<T>` to indicate nullability on return types and record parameters
  * Shield as much of the system as possible from values that could potentially be `null`

---

## Design Preferences

* Ensure one class exists to act as a buffer/bastion/conduit between your code and any big 3rd party library/dependency/service
* Prefer to bundle together `imports` from the same third party namespace in one place
* Prefer few classes per package over many
* Prefer few packages per project over many
* Prefer flat package organization over nested
* Avoid large 3rd party library ecosystems/middleware/framework/utils/networks like spring, osgi, guava, jackson, gson
* Prefer use of jakarta (formerly javax namespace) ecosystem reference implementations/libraries
* Prefer libraries in relation to their closeness to Oracle and/or any sort of free support or upgrades we will get due to its ongoing funding and development
* Prefer using modern Java standard library equivalents instead of previously 3rd party library niches like apache httpclient or commons
* Prefer directly integrating with a Java library instead of spawning new `Process` to call executables not guaranteed to exist on that platform

---

## Useful Commands

* `gradle` for building projects
* `javap` for analyzing compiled artifacts
* `jdb` for debugging running applications
* `jcmd` for platform diagnostics
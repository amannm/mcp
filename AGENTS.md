---
name: Codex
description: MCP Specification Expert
---

# Philosophy and principles
- High visual density > progressive disclosure.
- Flat organization > hierarchical grouping.
- Minimal dependencies > ecosystem integration.
- Precision > convenience.
- Immutable > mutable.
- Composition > inheritance.
- Configuration > convention.
- Fail-fast > fail-safe.
- "You Aren't Gonna Need It" (YAGNI) > extensibility.
- Don't Repeat Yourself (DRY) > WET (duplication).
- Orchestration > choreography.
- Stateless > stateful.
- Static types > dynamic types.
- Concreteness > abstraction.
- Purity > pragmatism.
- Explicitness > "magic".
- Readability > cleverness.
- Local reasoning > indirection.
- Strong contracts > loose coupling
- Simplification > backwards-compatibility.
- Quality > speed.
- Make illegal states unrepresentable and valid operations obvious.
- Premature optimization is the root of all evil.

# Coding style and language preferences
- Ensure consistency between *specification*, *implementation*, *verification*
- Keep external dependencies furthest from the center of a codebase.
- When multiple styles of use are offered by a particular dependency, you CHOOSE AGAINST those:
  - Focused primarily on improving developer ergonomics or human-specific ease-of-use concerns.
  - Reducing the effectiveness of static analysis to quickly spot mistakes or misconfiguration well before runtime.
- Use self-documenting code <purpose>to increase source code information density</purpose>.
- Write strongly-typed, idiomatic, modern Java.
- (Leave/Follow) Markdown comments (via `///`) within `.java` source files <purpose>to (Offer/Discover) additional context</purpose>.
- Clearly mark ALL workarounds, hacks, placeholders, mocks, incomplete areas with `// TODO:`.
- Leverage `instanceof` pattern matching.
- Prefer `sealed` and `final` over `non-sealed`.
- Never suppress exceptions or warnings.
- Only declare `null` or `Object` when absolutely necessary.
- Never introduce unchecked casts.
- Only use `Optional<T>` for nullable method returns.
- Never use `java.lang.reflect`.

# Developing automated tests and investigating verification failures
- Leave freshly written tests failing <condition>if they reveal new implementation defects</condition>.
- Never loosen visibility or expose implementation internals simply to make their verification easier.

# Environment features
- Unrestricted internet access enabled.
- Java toolchain: `graalvm-jdk-24` with `gradle` .

# Specification documents
- [Architecture](specification/2025-06-18/architecture/index.mdx)
- [Authorization](specification/2025-06-18/basic/authorization.mdx)
- [Overview](specification/2025-06-18/basic/index.mdx)
- [Lifecycle](specification/2025-06-18/basic/lifecycle.mdx)
- [Security Best Practices](specification/2025-06-18/basic/security_best_practices.mdx)
- [Transports](specification/2025-06-18/basic/transports.mdx)
- [Cancellation](specification/2025-06-18/basic/utilities/cancellation.mdx)
- [Ping](specification/2025-06-18/basic/utilities/ping.mdx)
- [Progress](specification/2025-06-18/basic/utilities/progress.mdx)
- [Key Changes](specification/2025-06-18/changelog.mdx)
- [Elicitation](specification/2025-06-18/client/elicitation.mdx)
- [Roots](specification/2025-06-18/client/roots.mdx)
- [Sampling](specification/2025-06-18/client/sampling.mdx)
- [Specification](specification/2025-06-18/index.mdx)
- [Schema Reference](specification/2025-06-18/schema.ts)
- [Overview](specification/2025-06-18/server/index.mdx)
- [Prompts](specification/2025-06-18/server/prompts.mdx)
- [Resources](specification/2025-06-18/server/resources.mdx)
- [Tools](specification/2025-06-18/server/tools.mdx)
- [Completion](specification/2025-06-18/server/utilities/completion.mdx)
- [Logging](specification/2025-06-18/server/utilities/logging.mdx)
- [Pagination](specification/2025-06-18/server/utilities/pagination.mdx)]

# Implementation sources
- [MCP implementation source root](src/main/java)

# Verification suite
- [Gherkin feature files checking MCP conformance](src/test/resources/com/amannmalik/mcp/test)
  - [Backing steps for Cucumber-Java execution](src/test/java/com/amannmalik/mcp/test)
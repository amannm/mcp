---
name: Codex
description: MCP Specification Expert
---

# Philosophy

- High information density
- Flat organization
- Minimal dependencies
- Precision over convenience
- Immutable over mutable
- Composition over inheritance
- Fail fast

# Preferences

- Use self-documenting code to avoid comments
- Write typed, idiomatic, modern Java 24
- Leverage `instanceof` pattern matching
- Unchecked casts are unacceptable
- Avoid introducing `null` or `Object`
- Avoid `Optional<T>` in fields or collections or arguments
- Avoid overwrought design patterns
- Mark all incomplete areas with `// TODO:`

# Wisdom

- Ensure alignment and consistency between specification, implementation, verification
- Make illegal states unrepresentable and valid operations obvious
- Keep external dependencies furthest from the center of a codebase
- Don't waste time writing PR descriptions
- Leave new tests failing if they manage to uncover a true defect
- Follow Markdown links within source files to acquire additional context

# Environment

- Internet egress: `proxy:8080`
- Java toolchain: `graalvm-jdk-24` with `gradle` 

# Documentation

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
here are a couple good umbrella terms you can use:
•	design tensions (my fave—implies pull in both directions)
•	polarities or dichotomies (when you want the drama)
•	trade-off axes (nice when you’re plotting)
•	opposing forces (from the patterns literature)
•	dialectics (philosophy-flavored)

below are more debatable axes that regularly sway real decisions. treat them as spectra, not dogma:

Code & design
•	Explicitness ↔ “Magic”: verbosity and clarity vs. convenience via convention/reflection.
•	Static types ↔ Dynamic types: compile-time safety vs. flexibility and speed of iteration.
•	Purity ↔ Pragmatism: fewer side effects vs. “do what works” imperative code.
•	Abstraction ↔ Concreteness: reusable layers vs. directly modeling the problem.
•	DRY ↔ WET (duplication): centralize knowledge vs. accept duplication to avoid wrong abstractions.
•	Local reasoning ↔ Indirection: everything visible in one file vs. many layers and helpers.
•	Readability ↔ Cleverness: obvious code vs. tight/novel code that’s harder to grok.
•	Uniformity ↔ Special cases: one way to do it vs. targeted optimizations/escape hatches.

Architecture & runtime
•	Monolith ↔ Microservices: simplicity and transactional integrity vs. independent scaling and team autonomy.
•	Consistency ↔ Availability (CAP lens): never show stale data vs. stay up under partitions.
•	ACID ↔ BASE: strict transactions vs. eventually consistent, highly scalable stores.
•	Synchronous ↔ Asynchronous: simplicity and debuggability vs. resilience and throughput.
•	Request/Response ↔ Event-driven: direct calls vs. decoupled pub/sub and choreography.
•	Stateful ↔ Stateless: easier local performance vs. elasticity and horizontal scale.
•	Caching ↔ Source-of-truth: speed with potential staleness vs. correctness with latency.
•	Scale-up ↔ Scale-out: bigger boxes vs. more boxes (cost/ops profile differs).
•	Orchestration ↔ Choreography: a central brain vs. autonomous services reacting to events.

Data & APIs
•	Schema-on-write ↔ Schema-on-read: strict ingestion vs. flexible, late-binding analytics.
•	Strong contracts ↔ Loose coupling: rigid, versioned APIs vs. tolerant readers and evolvable payloads.
•	Backwards-compatibility ↔ Simplification: never break clients vs. remove cruft to move faster.
•	REST/concepts ↔ RPC/tasks ↔ GraphQL/clients: resource-centric vs. procedure-centric vs. client-shaped querying.

Tooling & infrastructure
•	Build ↔ Buy: custom fit and IP vs. time-to-value and support.
•	Open source ↔ Vendor: transparency/control vs. warranties and SLAs.
•	Manual control ↔ Automation: precise hand-tuning vs. consistency and speed.
•	GC languages ↔ Manual memory: safety and productivity vs. maximal control/perf predictability.
•	Portable ↔ Native: run anywhere vs. platform features and peak performance.
•	Cloud-managed ↔ Self-hosted: velocity and ops offload vs. cost/control/compliance.

Process & culture
•	Speed ↔ Quality: ship fast vs. reduce defects and toil.
•	BDUF ↔ Evolutionary design: upfront architecture vs. emergent design through iterations.
•	YAGNI ↔ Extensibility: build only what’s needed vs. anticipate variation points.
•	“Worse is better” ↔ “The Right Thing”: simple, imperfect now vs. elegant, complete later.
•	Autonomy ↔ Standardization: team freedom vs. consistency and leverage.
•	Security/Privacy ↔ UX Friction: guardrails and prompts vs. smooth flows.
•	Engineer time ↔ Compute time: spend effort optimizing vs. spend money scaling.
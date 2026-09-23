# Temper

Temper is a Java Fault-Tolerance Orchestration library from nexcraft.dev.

## Status

Temper is in early development. The current project baseline is Java 21+.

## Bulkhead execution

Include `temper-failsafe` at runtime to enforce a bulkhead. The chain resolves
its runtime provider when built, then shares that bulkhead capacity across calls:

```java
FaultToleranceChain chain = FaultToleranceChain.builder()
        .next(Bulkhead.builder().maxConcurrentCalls(4).build())
        .build();
Orchestrator orchestrator = Orchestrator.create(new OrchestratorConfig(chain));
String result = orchestrator.execute(() -> "done");
```

Calls above the concurrency limit fail immediately with
`BulkheadRejectedException`. A configured component without a runtime provider
fails when the chain is built. `RateLimiter` currently has no production runtime
provider, so a chain containing it cannot be built until one is added. Policy
definitions should not be executed directly.

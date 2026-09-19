package dev.nexcraft.temper.core;

import java.util.Objects;
import java.util.concurrent.Callable;

public interface Orchestrator {
    public static Orchestrator create(OrchestratorConfig config) {
        Objects.requireNonNull(config, "config");
        return new DefaultOrchestrator();
    }

    public <T> T execute(Callable<T> task) throws Exception;
}

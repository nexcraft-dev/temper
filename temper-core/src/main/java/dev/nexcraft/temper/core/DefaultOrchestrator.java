package dev.nexcraft.temper.core;

import java.util.Objects;
import java.util.concurrent.Callable;

final class DefaultOrchestrator implements Orchestrator {
    DefaultOrchestrator() {
    }

    @Override
    public <T> T execute(Callable<T> task) throws Exception {
        return Objects.requireNonNull(task, "task").call();
    }
}

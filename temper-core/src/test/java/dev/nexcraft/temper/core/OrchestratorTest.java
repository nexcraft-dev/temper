package dev.nexcraft.temper.core;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Modifier;
import java.util.concurrent.Callable;

import org.junit.jupiter.api.Test;

class OrchestratorTest {
    @Test
    void createWithConfigurationReturnsOrchestrator() {
        assertNotNull(Orchestrator.create(new OrchestratorConfig()));
    }

    @Test
    void executeReturnsCallableStringUnchanged() throws Exception {
        Orchestrator orchestrator = Orchestrator.create(new OrchestratorConfig());
        String result = new String("result");
        Callable<String> task = () -> result;

        assertSame(result, orchestrator.execute(task));
    }

    @Test
    void executeReturnsNullForVoidCallable() throws Exception {
        Orchestrator orchestrator = Orchestrator.create(new OrchestratorConfig());
        Callable<Void> task = () -> null;

        assertNull(orchestrator.execute(task));
    }

    @Test
    void executeReturnsExactCheckedExceptionInstance() {
        Orchestrator orchestrator = Orchestrator.create(new OrchestratorConfig());
        Exception expected = new Exception("checked failure");

        Exception actual = assertThrows(Exception.class, () -> orchestrator.execute(() -> {
            throw expected;
        }));

        assertSame(expected, actual);
    }

    @Test
    void executeReturnsExactRuntimeExceptionInstance() {
        Orchestrator orchestrator = Orchestrator.create(new OrchestratorConfig());
        IllegalStateException expected = new IllegalStateException("runtime failure");

        IllegalStateException actual = assertThrows(IllegalStateException.class, () -> orchestrator.execute(() -> {
            throw expected;
        }));

        assertSame(expected, actual);
    }

    @Test
    void createRejectsNullConfiguration() {
        assertThrows(NullPointerException.class, () -> Orchestrator.create(null));
    }

    @Test
    void configurationRejectsNullChain() {
        assertThrows(NullPointerException.class, () -> new OrchestratorConfig(null));
    }

    @Test
    void executeRejectsNullTask() {
        Orchestrator orchestrator = Orchestrator.create(new OrchestratorConfig());

        assertThrows(NullPointerException.class, () -> orchestrator.execute(null));
    }

    @Test
    void orchestratorAndFactoryResultHaveSpecifiedVisibilityAndPackage() {
        Orchestrator orchestrator = Orchestrator.create(new OrchestratorConfig());

        assertTrue(Orchestrator.class.isInterface());
        assertTrue(Modifier.isPublic(Orchestrator.class.getModifiers()));
        assertEquals("dev.nexcraft.temper.core", orchestrator.getClass().getPackageName());
        assertFalse(Modifier.isPublic(orchestrator.getClass().getModifiers()));
    }
}

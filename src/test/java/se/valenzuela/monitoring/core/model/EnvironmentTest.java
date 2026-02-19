package se.valenzuela.monitoring.core.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EnvironmentTest {

    @Test
    void constructor_setsFieldsCorrectly() {
        var env = new Environment("Production", "#22C55E", 1);

        assertEquals("Production", env.getName());
        assertEquals("#22C55E", env.getColor());
        assertEquals(1, env.getDisplayOrder());
    }

    @Test
    void constructor_defaultDisplayOrder() {
        var env = new Environment("QA", null, 0);

        assertEquals(0, env.getDisplayOrder());
        assertNull(env.getColor());
    }

    @Test
    void equals_sameId_returnsTrue() {
        var env1 = new Environment("Prod", null, 0);
        env1.setId(1L);
        var env2 = new Environment("Different", null, 0);
        env2.setId(1L);

        assertEquals(env1, env2);
    }

    @Test
    void equals_differentId_returnsFalse() {
        var env1 = new Environment("Prod", null, 0);
        env1.setId(1L);
        var env2 = new Environment("Prod", null, 0);
        env2.setId(2L);

        assertNotEquals(env1, env2);
    }

    @Test
    void equals_nullId_notEqualToOther() {
        var env1 = new Environment("Prod", null, 0);
        var env2 = new Environment("Prod", null, 0);

        assertNotEquals(env1, env2);
    }

    @Test
    void equals_sameInstance_returnsTrue() {
        var env = new Environment("Prod", null, 0);

        assertEquals(env, env);
    }

    @Test
    void hashCode_sameId_sameHash() {
        var env1 = new Environment("Prod", null, 0);
        env1.setId(1L);
        var env2 = new Environment("Other", null, 0);
        env2.setId(1L);

        assertEquals(env1.hashCode(), env2.hashCode());
    }

    @Test
    void toString_returnsName() {
        var env = new Environment("Production", "#FF0000", 1);

        assertEquals("Production", env.toString());
    }
}

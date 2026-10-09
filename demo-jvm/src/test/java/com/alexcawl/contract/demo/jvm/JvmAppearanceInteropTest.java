package com.alexcawl.contract.demo.jvm;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class JvmAppearanceInteropTest {
    @Test
    public void generatedOverloadsAreCallableFromJava() {
        JvmAppearance original = JvmAppearanceContractKt.JvmAppearance(24);
        JvmAppearance unchanged = JvmAppearanceContractKt.copy(original);
        JvmAppearance copied = JvmAppearanceContractKt.copy(original, 48);

        assertTrue(original instanceof JvmAppearanceImpl);
        assertEquals(24, unchanged.getWidth());
        assertEquals(48, copied.getWidth());
        assertEquals("width:24", copied.label("width:"));
    }
}

package com.alexcawl.contract.demo.jvm;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class JvmAppearanceInteropTest {
    @Test
    public void currentFactoriesAndCopyAreCallableFromJava() {
        JvmAppearance original = JvmAppearanceConstructorKt.JvmAppearance(self -> 24, null);
        JvmAppearance unchanged = JvmAppearanceCopyKt.copy(original, null, null);
        JvmAppearance copied = JvmAppearanceCopyKt.copy(original, self -> 48, null);
        assertEquals(24, unchanged.width());
        assertEquals(48, copied.width());
        assertEquals("width:48", copied.label("width:"));
    }

    @Test
    public void nestedContainerSupportsManualImplementations() {
        JvmAppearance.Contract container = new JvmAppearance.Contract(self -> 12, null);
        JvmAppearance manual = new JvmAppearance() {
            @Override
            public int width() {
                return 12;
            }

            @Override
            public JvmAppearance.Contract contract() {
                return container;
            }
        };
        assertEquals(12, container.getWidth().invoke(manual).intValue());
        assertEquals(JvmAppearance.class, container.getClass().getEnclosingClass());
        assertEquals("width:24", JvmAppearanceCopyKt.copy(manual, self -> 24, null).label("width:"));
    }
}

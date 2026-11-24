package com.hereliesaz.aznavrail.updates

import org.junit.Assert.*
import org.junit.Test

class ReleaseVersionTest {
    @Test fun numericOrdering() {
        assertEquals(1, ReleaseVersion.compare("v1.10.0", "1.9.9"))
        assertEquals(-1, ReleaseVersion.compare("1.2", "1.2.1"))
        assertEquals(0, ReleaseVersion.compare("1.2", "1.2.0"))
        assertNull(ReleaseVersion.compare("2.0-beta", "1.0"))
    }
    @Test fun workflowCodeWins() {
        assertTrue(ReleaseVersion.newer("v1.0", "versionCode 52", "1.0.9", 51))
        assertFalse(ReleaseVersion.newer("v9.0", "versionCode 50", "1.0", 51))
        assertTrue(ReleaseVersion.newer("v1.0.0", "Automated release build — 1.0.0.42", "1.0.0.41", 41))
        assertFalse(ReleaseVersion.newer("unknown", "", "1.0", 51))
    }
}

package com.mantismoonlabs.fujinetgo800.fujinet

import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FujiNetWebViewActivityBackTest {
    @Test
    fun backDispatcherFinishesActivity() {
        ActivityScenario.launch(FujiNetWebViewActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.onBackPressedDispatcher.onBackPressed()
            }
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()

            assertEquals(Lifecycle.State.DESTROYED, scenario.state)
        }
    }
}

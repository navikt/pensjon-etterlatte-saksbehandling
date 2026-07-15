package efterlatte.prosessering

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TaskStateMachineTest {
    @Test
    fun `lovlige overganger fra motorens livssyklus`() {
        assertTrue(TaskStateMachine.erLovlig(fra = Status.KLAR, til = Status.KJØRER))
        assertTrue(TaskStateMachine.erLovlig(fra = Status.KJØRER, til = Status.FULLFØRT))
        assertTrue(TaskStateMachine.erLovlig(fra = Status.KJØRER, til = Status.KLAR))
        assertTrue(TaskStateMachine.erLovlig(fra = Status.KJØRER, til = Status.STOPPET))
        assertTrue(TaskStateMachine.erLovlig(fra = Status.STOPPET, til = Status.KLAR))
        assertTrue(TaskStateMachine.erLovlig(fra = Status.AVBRUTT, til = Status.KLAR))
    }

    @Test
    fun `ulovlige overganger`() {
        assertFalse(TaskStateMachine.erLovlig(fra = Status.FULLFØRT, til = Status.KLAR))
        assertFalse(TaskStateMachine.erLovlig(fra = Status.KLAR, til = Status.FULLFØRT))
        assertFalse(TaskStateMachine.erLovlig(fra = Status.KJØRER, til = Status.AVBRUTT))
    }

    @Test
    fun `krev kaster paa ulovlig overgang`() {
        try {
            TaskStateMachine.krev(fra = Status.FULLFØRT, til = Status.KJØRER)
            throw AssertionError("Forventet IllegalStateException")
        } catch (e: IllegalStateException) {
            assertTrue(e.message!!.contains("Ulovlig statusovergang"))
        }
    }
}

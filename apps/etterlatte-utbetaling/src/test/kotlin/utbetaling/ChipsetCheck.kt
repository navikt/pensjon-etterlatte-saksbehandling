package no.nav.etterlatte.utbetaling

object ChipsetCheck {

    const val erM1 = "no.nav.etterlatte.utbetaling.ChipsetCheck#erM1"

    @JvmStatic
    fun erM1(): Boolean {
        val process = ProcessBuilder("sysctl", "-n", "machdep.cpu.brand_string").start()
        val output = process.inputReader().readLine()
        val status = process.waitFor()
        return (status == 0 && output.contains("Apple M1", ignoreCase = true))
    }
}
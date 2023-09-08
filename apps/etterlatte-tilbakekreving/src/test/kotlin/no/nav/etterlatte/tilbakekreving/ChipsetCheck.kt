package no.nav.etterlatte.tilbakekreving

object ChipsetCheck {

    const val erM1EllerM2 = "no.nav.etterlatte.tilbakekreving.ChipsetCheck#erM1EllerM2"

    @JvmStatic
    fun erM1EllerM2(): Boolean {
        val process = ProcessBuilder("sysctl", "-n", "machdep.cpu.brand_string").start()
        val output = process.inputReader().readLine()
        val status = process.waitFor()
        return (
            status == 0 &&
                (
                    output.contains("Apple M1", ignoreCase = true) ||
                        output.contains("Apple M2", ignoreCase = true)
                    )
            )
    }
}
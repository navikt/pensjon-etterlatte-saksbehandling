package no.nav.etterlatte.libs.common

data class IntBroek(
    val teller: Int,
    val nevner: Int,
) {
    companion object {
        fun fra(broek: Pair<Int?, Int?>): IntBroek? {
            return broek.first?.let { teller ->
                broek.second?.let { nevner ->
                    IntBroek(teller, nevner)
                }
            }
        }
    }
}

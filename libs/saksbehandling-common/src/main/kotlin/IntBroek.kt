package no.nav.etterlatte.libs.common

data class IntBroek(
    val teller: Int,
    val nevner: Int,
) {
    companion object {
        fun fra(broek: Pair<Int?, Int?>): IntBroek? =
            broek.first?.let { teller ->
                broek.second.takeIf { it != null && it != 0 }?.let { nevner ->
                    IntBroek(teller, nevner)
                }
            }
    }
}

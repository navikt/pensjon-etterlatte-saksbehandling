package no.nav.etterlatte.rapidsandrivers

import no.nav.helse.rapids_rivers.JsonMessage

object ReguleringEvents {
    const val DATO = "dato"
    const val KJOERING = "kjoering"
    const val ANTALL = "antall"
    const val SPESIFIKKE_SAKER = "spesifikke_saker"

    const val BEREGNING_BELOEP_FOER = "beregning_beloep_foer"
    const val BEREGNING_BELOEP_ETTER = "beregning_beloep_etter"
    const val BEREGNING_G_FOER = "beregning_g_foer"
    const val BEREGNING_G_ETTER = "beregning_g_etter"
    const val BEREGNING_BRUKT_OMREGNINGSFAKTOR = "beregning_brukt_omregningsfaktor"
    const val VEDTAK_BELOEP = "vedtak_beloep"
}

var JsonMessage.saker: List<Long>
    get() =
        this[ReguleringEvents.SPESIFIKKE_SAKER]
            .asText()
            .trim()
            .split(";")
            .filter { it.isNotEmpty() }
            .map { it.toLong() }
    set(name) {
        this[ReguleringEvents.SPESIFIKKE_SAKER] = name.joinToString(";") { it.toString() }
    }

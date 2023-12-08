package no.nav.etterlatte.rapidsandrivers

object ReguleringEvents {
    private const val PREFIX = "REGULERING:"

    const val EVENT_NAME = "REGULERING"
    const val AARSAK = "aarsak"
    const val DATO = "dato"
    const val START_REGULERING = "START_REGULERING"

    const val FINN_LOEPENDE_YTELSER = "${PREFIX}FINN_LOEPENDE_YTELSER"
    const val OMREGNINGSHENDELSE = "${PREFIX}OMREGNINGSHENDELSE"
    const val VILKAARSVURDER = "${PREFIX}VILKAARSVURDER"
    const val BEREGN = "${PREFIX}BEREGN"
    const val OPPRETT_VEDTAK = "${PREFIX}OPPRETT_VEDTAK"
}

object OmregningEvents {
    const val OMREGNING_NYE_REGLER = "OMREGNING_NYE_REGLER"
    const val OMREGNING_BRUTTO = "OMREGNING_BRUTTO"
    const val OMREGNING_NYE_REGLER_KJORING = "OMREGNING_NYE_REGLER_KJORING"
}

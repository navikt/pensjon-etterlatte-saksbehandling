package no.nav.etterlatte.brev.model

import java.time.LocalDate

object Etterbetaling {
    fun fraOmstillingsstoenadBeregningsperioder(
        dto: EtterbetalingDTO,
        perioder: List<OmstillingsstoenadBeregningsperiode>,
    ) = OmstillingsstoenadEtterbetaling(
        fraDato = dto.datoFom,
        tilDato = dto.datoTom,
        etterbetalingsperioder =
            perioder
                .filter { beregningsperiodeErInnenforEtterbetalingsperiode(it.datoFOM, it.datoTOM, dto) }
                .sortedByDescending { it.datoFOM }
                .let { list ->
                    val oppdatertListe = list.toMutableList()

                    // Setter tilDato på nyeste periode innenfor hva som er satt i etterbetaling
                    oppdatertListe
                        .firstOrNull()
                        ?.copy(datoTOM = dto.datoTom)
                        ?.let { oppdatertListe[0] = it }

                    // Setter fraDato på eldste periode innenfor hva som er satt i etterbetaling
                    oppdatertListe
                        .lastOrNull()
                        ?.copy(datoFOM = dto.datoFom)
                        ?.let { oppdatertListe[list.lastIndex] = it }

                    oppdatertListe.toList()
                },
    )

    private fun beregningsperiodeErInnenforEtterbetalingsperiode(
        datoFOM: LocalDate,
        datoTOM: LocalDate?,
        dto: EtterbetalingDTO,
    ) = datoFOM.isBefore(dto.datoTom) && dto.datoFom.isBefore(datoTOM ?: LocalDate.MAX)
}

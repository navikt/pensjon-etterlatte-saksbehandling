import no.nav.etterlatte.behandling.behandlinginfo.Etterbetaling
import no.nav.etterlatte.brev.model.oms.OmstillingsstoenadInnvilgelseVedtakBrevData
import java.time.LocalDate

object Etterbetaling {
    fun fraOmstillingsstoenadBeregningsperioder(
        dto: Etterbetaling, // TODO hvorfor nullable datoer i denne dtoen?
        perioder: List<OmstillingsstoenadInnvilgelseVedtakBrevData.OmstillingsstoenadBeregningsperiode>,
    ) = OmstillingsstoenadInnvilgelseVedtakBrevData.OmstillingsstoenadEtterbetaling(
        fraDato = dto.fom.atDay(1),
        tilDato = dto.tom.atDay(1),
        etterbetalingsperioder =
            perioder
                .filter { beregningsperiodeErInnenforEtterbetalingsperiode(it.datoFOM, it.datoTOM, dto) }
                .sortedByDescending { it.datoFOM }
                .let { list ->
                    val oppdatertListe = list.toMutableList()

                    // Setter tilDato på nyeste periode innenfor hva som er satt i etterbetaling
                    oppdatertListe
                        .firstOrNull()
                        ?.copy(datoTOM = dto.tom.atDay(1))
                        ?.let { oppdatertListe[0] = it }

                    // Setter fraDato på eldste periode innenfor hva som er satt i etterbetaling
                    oppdatertListe
                        .lastOrNull()
                        ?.copy(datoFOM = dto.fom.atDay(1))
                        ?.let { oppdatertListe[list.lastIndex] = it }

                    oppdatertListe.toList()
                },
    )

    private fun beregningsperiodeErInnenforEtterbetalingsperiode(
        datoFOM: LocalDate,
        datoTOM: LocalDate?,
        dto: Etterbetaling,
    ): Boolean = datoFOM.isBefore(dto.tom.atDay(1)) && dto.fom.atDay(1).isBefore(datoTOM ?: LocalDate.MAX)
}

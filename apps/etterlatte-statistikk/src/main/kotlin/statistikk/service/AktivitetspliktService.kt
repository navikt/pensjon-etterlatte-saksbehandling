package no.nav.etterlatte.statistikk.service

import no.nav.etterlatte.libs.common.aktivitetsplikt.AktivitetspliktDto
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.statistikk.database.AktivitetspliktRepo
import no.nav.etterlatte.statistikk.database.StatistikkAktivitet
import no.nav.etterlatte.statistikk.database.VurdertAktivitet
import java.time.YearMonth
import java.time.temporal.ChronoUnit

class AktivitetspliktService(
    private val aktivitetspliktRepo: AktivitetspliktRepo,
) {
    fun oppdaterVurderingAktivitetsplikt(
        aktivitetspliktDto: AktivitetspliktDto,
        overstyrRegistrert: Tidspunkt? = null,
    ) {
        aktivitetspliktRepo.lagreAktivitetspliktForSak(aktivitetspliktDto, overstyrRegistrert)
    }

    fun hentAktivitet(
        sakId: SakId,
        sisteMaaned: YearMonth,
    ): StatistikkAktivitet? = aktivitetspliktRepo.hentAktivitetspliktForMaaned(sakId, sisteMaaned)

    fun mapAktivitetForSaker(
        sakIds: List<SakId>,
        maaned: YearMonth,
    ): Map<SakId, AktivitetForMaaned> {
        val aktiviteter = aktivitetspliktRepo.hentAktivitetspliktForMaaneder(sakIds, maaned)
        return aktiviteter.associate { it.sakId to AktivitetForMaaned.mapFraStatistikkAktivitet(it, maaned) }
    }
}

enum class Aktivitetsplikt {
    OMSTILLINGSPERIODE,
    VARIG_UNNTAK,
    JA,
}

data class AktivitetForMaaned(
    val harAktivitetsplikt: Aktivitetsplikt,
    val oppfyllerAktivitet: Boolean?,
    val aktivitet: String?,
) {
    companion object {
        val FALLBACK_OMSTILLINGSSTOENAD =
            AktivitetForMaaned(
                harAktivitetsplikt = Aktivitetsplikt.OMSTILLINGSPERIODE,
                oppfyllerAktivitet = null,
                aktivitet = null,
            )

        fun mapFraStatistikkAktivitet(
            statistikkAktivitet: StatistikkAktivitet,
            statistikkmaaned: YearMonth,
        ): AktivitetForMaaned {
            val maanederEtterDoedsfall =
                ChronoUnit.MONTHS.between(
                    statistikkAktivitet.avdoedDoedsmaaned,
                    statistikkmaaned,
                )
            val aktivitetsplikt =
                when (maanederEtterDoedsfall) {
                    in 0..6 -> Aktivitetsplikt.OMSTILLINGSPERIODE
                    else ->
                        if (statistikkAktivitet.harVarigUnntak) {
                            Aktivitetsplikt.VARIG_UNNTAK
                        } else {
                            Aktivitetsplikt.JA
                        }
                }

            val vurderingForMaaned =
                statistikkAktivitet.aktivitetsgrad.find { it.erInnenforMaaned(statistikkmaaned) }
            val unntakForMaaned = statistikkAktivitet.unntak.find { it.erInnenforMaaned(statistikkmaaned) }

            // TODO: Når vi har andre krav etter 12 måneder må denne justeres
            val fyllerAktivitet =
                unntakForMaaned != null || vurderingForMaaned?.vurdering != VurdertAktivitet.UNDER_50_PROSENT

            val aktiviteterForMaaned =
                statistikkAktivitet.brukersAktivitet
                    .filter { it.erInnenforMaaned(statistikkmaaned) }
                    .map { it.opplysning }
            val aktivitetForBruker =
                if (aktiviteterForMaaned.size == 1) {
                    aktiviteterForMaaned.single()
                } else if (aktiviteterForMaaned.isEmpty()) {
                    null
                } else {
                    "FLERE_AKTIVITETER"
                }

            val aktivitet =
                unntakForMaaned?.opplysning?.let { "UNNTAK_$it" }
                    ?: listOfNotNull(
                        vurderingForMaaned?.vurdering?.name,
                        aktivitetForBruker,
                    ).joinToString(separator = "_")

            return AktivitetForMaaned(
                harAktivitetsplikt = aktivitetsplikt,
                oppfyllerAktivitet = fyllerAktivitet.takeIf { aktivitetsplikt == Aktivitetsplikt.JA },
                aktivitet = aktivitet.takeIf { aktivitetsplikt == Aktivitetsplikt.JA },
            )
        }
    }
}

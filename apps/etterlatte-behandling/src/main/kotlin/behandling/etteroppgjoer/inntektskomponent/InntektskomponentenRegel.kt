package no.nav.etterlatte.behandling.etteroppgjoer.inntektskomponent

import no.nav.etterlatte.libs.common.behandling.etteroppgjoer.InntektSummert
import no.nav.etterlatte.libs.common.behandling.etteroppgjoer.Inntektsmaaned
import no.nav.etterlatte.libs.common.feilhaandtering.krev
import no.nav.etterlatte.libs.regler.FaktumNode
import no.nav.etterlatte.libs.regler.Regel
import no.nav.etterlatte.libs.regler.RegelMeta
import no.nav.etterlatte.libs.regler.RegelReferanse
import no.nav.etterlatte.libs.regler.benytter
import no.nav.etterlatte.libs.regler.finnFaktumIGrunnlag
import no.nav.etterlatte.libs.regler.med
import no.nav.etterlatte.libs.regler.og
import java.time.LocalDate
import java.time.YearMonth

val OMS_GYLDIG_FRA: LocalDate = LocalDate.of(2024, 1, 1)

data class InntektGrunnlag(
    val inntekt: FaktumNode<InntektBulkResponsDto>,
    val aar: FaktumNode<Int>,
)

private val finnInntektIGrunnlag: Regel<InntektGrunnlag, InntektBulkResponsDto> =
    finnFaktumIGrunnlag(
        gjelderFra = OMS_GYLDIG_FRA,
        beskrivelse = "Henter inntekter fra grunnlaget",
        finnFaktum = { it.inntekt },
        finnFelt = { it },
    )

private val finnAarIGrunnlag: Regel<InntektGrunnlag, Int> =
    finnFaktumIGrunnlag(
        gjelderFra = OMS_GYLDIG_FRA,
        beskrivelse = "Henter året for inntekter fra grunnlaget",
        finnFaktum = { it.aar },
        finnFelt = { it },
    )

val summerInntekter: Regel<InntektGrunnlag, InntektSummert> =
    RegelMeta(
        gjelderFra = OMS_GYLDIG_FRA,
        beskrivelse = "Summerer inntekter fra inntektskomponenten per måned",
        regelReferanse = RegelReferanse(id = "INNTEKTSKOMPONENTEN-SUMMER-INNTEKTER", versjon = "1.0"),
    ) benytter finnInntektIGrunnlag og finnAarIGrunnlag med { inntekt, aar ->
        val alleMaanederIAar: List<YearMonth> =
            (1..12).toList().map { month: Int ->
                YearMonth.of(aar, month)
            }

        val alleInntektsmaanederIGrunnlag = inntekt.data.map { it.maaned }
        krev(alleMaanederIAar.toSet().containsAll(alleInntektsmaanederIGrunnlag)) {
            "Har inntekter ($alleInntektsmaanederIGrunnlag) som ikke er innenfor angitt etteroppgjørsår ($aar)"
        }

        val alleInntektsmaaneder =
            alleMaanederIAar.map { maaned ->
                // summer inntekter som er innenfor denne måneden
                val sumAlleInntekterForMaaned =
                    inntekt.data
                        .filter { it.maaned == maaned }
                        .sumOf { inntekter -> inntekter.inntektListe.sumOf { it.beloep } }
                Inntektsmaaned(
                    maaned = maaned,
                    beloep = sumAlleInntekterForMaaned,
                )
            }

        InntektSummert(
            filter = inntekt.filter,
            inntekter = alleInntektsmaaneder,
        )
    }

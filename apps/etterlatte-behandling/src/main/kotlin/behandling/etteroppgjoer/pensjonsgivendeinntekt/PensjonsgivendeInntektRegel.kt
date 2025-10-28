package no.nav.etterlatte.behandling.etteroppgjoer.pensjonsgivendeinntekt

import no.nav.etterlatte.behandling.etteroppgjoer.PensjonsgivendeInntektSummert
import no.nav.etterlatte.behandling.etteroppgjoer.sigrun.PensjonsgivendeInntektAarResponse
import no.nav.etterlatte.libs.regler.Regel
import no.nav.etterlatte.libs.regler.RegelMeta
import no.nav.etterlatte.libs.regler.RegelReferanse
import no.nav.etterlatte.libs.regler.benytter
import no.nav.etterlatte.libs.regler.finnFaktumIGrunnlag
import no.nav.etterlatte.libs.regler.med
import no.nav.etterlatte.libs.regler.og
import java.time.LocalDate

val OMS_GYLDIG_FRA: LocalDate = LocalDate.of(2024, 1, 1)

private val finnPensjonsgivendeInntektIGrunnlag: Regel<PensjonsgivendeInntektGrunnlag, PensjonsgivendeInntektAarResponse> =
    finnFaktumIGrunnlag(
        gjelderFra = OMS_GYLDIG_FRA,
        beskrivelse = "Henter inntekter fra grunnlaget",
        finnFaktum = { it.pensjonsgivendeInntektAarResponse },
        finnFelt = { it },
    )

private val finnAarIGrunnlag: Regel<PensjonsgivendeInntektGrunnlag, Int> =
    finnFaktumIGrunnlag(
        gjelderFra = OMS_GYLDIG_FRA,
        beskrivelse = "Henter året for inntekter fra grunnlaget",
        finnFaktum = { it.aar },
        finnFelt = { it },
    )

val summerPensjonsgivendeInntekter: Regel<PensjonsgivendeInntektGrunnlag, PensjonsgivendeInntektSummert> =
    RegelMeta(
        gjelderFra = OMS_GYLDIG_FRA,
        beskrivelse = "Summerer inntekter fra Skatteetaten",
        regelReferanse = RegelReferanse(id = "PENSJONSGIVENDE-INNTEKT-SUMMER", versjon = "1.0"),
    ) benytter finnPensjonsgivendeInntektIGrunnlag og finnAarIGrunnlag med { inntekt, aar ->

        val inntekter = inntekt.pensjonsgivendeInntekt

        PensjonsgivendeInntektSummert(
            loensinntekt =
                inntekter.sumOf {
                    it.pensjonsgivendeInntektAvLoennsinntekt ?: 0
                },
            naeringsinntekt =
                inntekter.sumOf {
                    listOfNotNull(
                        it.pensjonsgivendeInntektAvNaeringsinntekt,
                        it.pensjonsgivendeInntektAvNaeringsinntektFraFiskeFangstEllerFamiliebarnehage,
                    ).sum()
                },
        )
    }

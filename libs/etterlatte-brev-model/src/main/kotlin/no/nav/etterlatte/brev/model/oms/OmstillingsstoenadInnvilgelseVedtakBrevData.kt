package no.nav.etterlatte.brev.model.oms

import no.nav.etterlatte.beregning.grunnlag.InstitusjonsoppholdBeregningsgrunnlag
import no.nav.etterlatte.beregning.grunnlag.Reduksjon
import no.nav.etterlatte.brev.BrevFastInnholdData
import no.nav.etterlatte.brev.BrevInnholdVedlegg
import no.nav.etterlatte.brev.BrevRedigerbarInnholdData
import no.nav.etterlatte.brev.BrevVedleggKey
import no.nav.etterlatte.brev.BrevVedleggRedigerbarNy
import no.nav.etterlatte.brev.Brevkoder
import no.nav.etterlatte.brev.HarVedlegg
import no.nav.etterlatte.brev.Slate
import no.nav.etterlatte.brev.Vedlegg
import no.nav.etterlatte.brev.behandling.AvkortetBeregningsperiode
import no.nav.etterlatte.brev.model.OmstillingsstoenadBeregning
import no.nav.etterlatte.libs.common.IntBroek
import no.nav.etterlatte.libs.common.beregning.BeregningOgAvkortingPeriodeDto
import no.nav.etterlatte.libs.common.beregning.BeregningsMetode
import no.nav.etterlatte.libs.common.beregning.SanksjonertYtelse
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.trygdetid.BeregnetTrygdetidGrunnlagDto
import no.nav.etterlatte.regler.ANTALL_DESIMALER_INNTENKT
import no.nav.etterlatte.regler.roundingModeInntekt
import no.nav.etterlatte.trygdetid.TrygdetidType
import no.nav.pensjon.brevbaker.api.model.Foedselsnummer
import no.nav.pensjon.brevbaker.api.model.Kroner
import java.math.BigDecimal
import java.time.LocalDate

object OmstillingsstoenadInnvilgelseVedtakBrevData {
    data class Vedtak(
        val beregning: OmstillingsstoenadBeregning,
        val innvilgetMindreEnnFireMndEtterDoedsfall: Boolean,
        val omsRettUtenTidsbegrensning: Boolean,
        val etterbetaling: no.nav.etterlatte.brev.model.OmstillingsstoenadEtterbetaling?,
        val harUtbetaling: Boolean,
        val bosattUtland: Boolean,
        val erSluttbehandling: Boolean,
        val tidligereFamiliepleier: Boolean,
        val datoVedtakOmgjoering: LocalDate?,
    ) : BrevFastInnholdData() {
        override val type: String = "OMSTILLINGSSTOENAD_INNVILGELSE"

        override fun medVedleggInnhold(innhold: () -> List<BrevInnholdVedlegg>): BrevFastInnholdData {
            return this // TODO skal fjernes?
        }

        override val brevKode: Brevkoder = Brevkoder.OMS_INNVILGELSE
    }

    data class VedtakInnhold(
        val virkningsdato: LocalDate,
        val utbetalingsbeloep: Kroner,
        val etterbetaling: Boolean,
        val avdoed: no.nav.etterlatte.brev.behandling.Avdoed?,
        val harUtbetaling: Boolean,
        val beregning: OmstillingsstoenadBeregning,
        val erSluttbehandling: Boolean = false,
        val tidligereFamiliepleier: Boolean,
        val datoVedtakOmgjoering: LocalDate?,
    ) : BrevRedigerbarInnholdData() {
        override val type: String = "OMSTILLINGSSTOENAD_INNVILGELSE_UTFALL"

        override val brevKode: Brevkoder = Brevkoder.OMS_INNVILGELSE
    }

    fun beregningsvedleggInnhold(): BrevVedleggRedigerbarNy =
        BrevVedleggRedigerbarNy(
            data = null,
            vedlegg = Vedlegg.OMS_BEREGNING,
            vedleggId = BrevVedleggKey.OMS_BEREGNING,
        )

    data class OmstillingsstoenadBeregningsperiode(
        val datoFOM: LocalDate,
        val datoTOM: LocalDate?,
        val inntekt: Kroner,
        val oppgittInntekt: Kroner,
        val fratrekkInnAar: Kroner,
        val innvilgaMaaneder: Int,
        val grunnbeloep: Kroner,
        val ytelseFoerAvkorting: Kroner,
        val restanse: Kroner,
        val utbetaltBeloep: Kroner,
        val trygdetid: Int,
        val beregningsMetodeAnvendt: BeregningsMetode,
        val beregningsMetodeFraGrunnlag: BeregningsMetode,
        val sanksjon: Boolean,
        val institusjon: Boolean,
        val erOverstyrtInnvilgaMaaneder: Boolean,
    )

    data class TrygdetidMedBeregningsmetode(
        val navnAvdoed: String?,
        val trygdetidsperioder: List<Trygdetidsperiode>,
        val beregnetTrygdetidAar: Int,
        val prorataBroek: IntBroek?,
        val beregningsMetodeAnvendt: BeregningsMetode,
        val beregningsMetodeFraGrunnlag: BeregningsMetode,
        val mindreEnnFireFemtedelerAvOpptjeningstiden: Boolean,
        val ident: String,
    )

    data class Trygdetidsperiode(
        val datoFOM: LocalDate,
        val datoTOM: LocalDate?,
        val landkode: String,
        val land: String?,
        val opptjeningsperiode: BeregnetTrygdetidGrunnlagDto?,
        val type: TrygdetidType,
    )

    data class OmstillingsstoenadEtterbetaling(
        val fraDato: LocalDate,
        val tilDato: LocalDate,
        val etterbetalingsperioder: List<OmstillingsstoenadBeregningsperiode>,
    )

    data class Avdoed(
        val fnr: Foedselsnummer,
        val navn: String,
        val doedsdato: LocalDate,
    )
}

fun BeregningOgAvkortingPeriodeDto.toAvkortetBeregningsperiode(): AvkortetBeregningsperiode =
    AvkortetBeregningsperiode(
        datoFOM = periode.fom.atDay(1),
        datoTOM = periode.tom?.atEndOfMonth(),
        grunnbeloep = Kroner(grunnbelop),
        // (vises i brev) maanedsinntekt regel burde eksponert dette, krever omskrivning av regler som vi må bli enige om
        inntekt =
            Kroner(
                BigDecimal(oppgittInntekt - fratrekkInnAar)
                    .setScale(
                        ANTALL_DESIMALER_INNTENKT,
                        roundingModeInntekt,
                    ).toInt(),
            ),
        oppgittInntekt = Kroner(oppgittInntekt),
        fratrekkInnAar = Kroner(fratrekkInnAar),
        innvilgaMaaneder = innvilgaMaaneder,
        ytelseFoerAvkorting = Kroner(ytelseFoerAvkorting),
        restanse = Kroner(restanse),
        utbetaltBeloep = Kroner(ytelseEtterAvkorting),
        trygdetid = trygdetid,
        beregningsMetodeAnvendt =
            beregningsMetode
                ?: throw InternfeilException("OMS Brevdata krever anvendt beregningsmetode"),
        beregningsMetodeFraGrunnlag =
            beregningsMetodeFraGrunnlag
                ?: throw InternfeilException("OMS Brevdata krever valgt beregningsmetode fra beregningsgrunnlag"),
        // ved manuelt overstyrt beregning har vi ikke grunnlag
        sanksjon = sanksjon,
        institusjon = institusjonsopphold,
        erOverstyrtInnvilgaMaaneder = erOverstyrtInnvilgaMaaneder,
    )

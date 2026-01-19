package no.nav.etterlatte.brev.model

import no.nav.etterlatte.brev.BrevData
import no.nav.etterlatte.brev.HarVedlegg
import no.nav.etterlatte.brev.Slate
import no.nav.etterlatte.brev.behandling.Avdoed
import no.nav.etterlatte.brev.behandling.Beregningsperiode
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.pensjon.brevbaker.api.model.Kroner
import java.time.LocalDate
import java.util.UUID

data class BarnepensjonEtterbetaling(
    val frivilligSkattetrekk: Boolean?,
)

data class BarnepensjonBeregning(
    override val innhold: List<Slate.Element>,
    val antallBarn: Int,
    val virkningsdato: LocalDate,
    val grunnbeloep: Kroner,
    val beregningsperioder: List<BarnepensjonBeregningsperiode>,
    val sisteBeregningsperiode: BarnepensjonBeregningsperiode,
    val bruktTrygdetid: TrygdetidMedBeregningsmetode,
    val trygdetid: List<TrygdetidMedBeregningsmetode>,
    val erForeldreloes: Boolean,
    val forskjelligTrygdetid: ForskjelligTrygdetid?,
    val erYrkesskade: Boolean,
) : HarVedlegg

data class BarnepensjonBeregningsperiode(
    val datoFOM: LocalDate,
    val datoTOM: LocalDate?,
    val grunnbeloep: Kroner,
    val antallBarn: Int,
    val avdoedeForeldre: List<String?>?,
    val trygdetidForIdent: String?,
    var utbetaltBeloep: Kroner,
    val harForeldreloessats: Boolean,
) {
    companion object {
        fun fra(
            beregningsperiode: Beregningsperiode,
            erForeldreloes: Boolean,
        ): BarnepensjonBeregningsperiode =
            BarnepensjonBeregningsperiode(
                datoFOM = beregningsperiode.datoFOM,
                datoTOM = beregningsperiode.datoTOM,
                grunnbeloep = beregningsperiode.grunnbeloep,
                utbetaltBeloep = beregningsperiode.utbetaltBeloep,
                antallBarn = beregningsperiode.antallBarn,
                avdoedeForeldre = beregningsperiode.avdoedeForeldre,
                trygdetidForIdent = beregningsperiode.trygdetidForIdent,
                harForeldreloessats = beregningsperiode.harForeldreloessats ?: erForeldreloes,
            )
    }
}

data class OmstillingsstoenadBeregningRedigerbartUtfall(
    val virkningsdato: LocalDate,
    val beregningsperioder: List<OmstillingsstoenadBeregningsperiode>,
    val sisteBeregningsperiode: OmstillingsstoenadBeregningsperiode,
    val sisteBeregningsperiodeNesteAar: OmstillingsstoenadBeregningsperiode?,
    val oppphoersdato: LocalDate?,
    val opphoerNesteAar: Boolean,
) : BrevData

data class ForskjelligTrygdetid(
    val foersteTrygdetid: TrygdetidMedBeregningsmetode,
    val foersteVirkningsdato: LocalDate,
    val senereVirkningsdato: LocalDate,
    val harForskjelligMetode: Boolean,
    val erForskjellig: Boolean,
)

data class ForskjelligAvdoedPeriode(
    val foersteAvdoed: Avdoed,
    val senereAvdoed: Avdoed,
    val senereVirkningsdato: LocalDate,
)

enum class FeilutbetalingType {
    FEILUTBETALING_UTEN_VARSEL,
    FEILUTBETALING_4RG_UTEN_VARSEL,
    FEILUTBETALING_MED_VARSEL,
    INGEN_FEILUTBETALING,
}

class ManglerAvdoedBruktTilTrygdetid :
    UgyldigForespoerselException(
        code = "MANGLER_AVDOED_INFO_I_BEREGNING",
        detail = "Det mangler avdød i beregning. Utfør beregning på nytt og prøv igjen.",
    )

class FantIkkeIdentTilTrygdetidBlantAvdoede :
    UgyldigForespoerselException(
        code = "FANT_IKKE_TRYGDETID_IDENT_BLANT_AVDOEDE",
        detail = "Ident knyttet til trygdetid er ikke blant avdøde knyttet til sak",
    )

class OverstyrtTrygdetidManglerAvdoed :
    UgyldigForespoerselException(
        code = "OVERSTYRT_TRYGDETID_MANGLER_AVDOED",
        detail = "Overstyrt trygdetid mangler avdød. Trygdetiden må overskrives med ny overstyrt trygdetid",
    )

class ManglerFrivilligSkattetrekk(
    behandlingId: UUID?,
) : UgyldigForespoerselException(
        code = "BEHANDLING_MANGLER_FRIVILLIG_SKATTETREKK",
        detail =
            "Behandling mangler informasjon om frivillig skattetrekk, som er påkrevd for barnepensjon. " +
                "Du kan legge til dette i Valg av utfall i brev.",
        meta = mapOf("behandlingId" to behandlingId.toString()),
    )

class ManglerBrevutfall(
    behandlingId: UUID?,
) : UgyldigForespoerselException(
        code = "BEHANDLING_MANGLER_BREVUTFALL",
        detail = "Behandling mangler brevutfall, som er påkrevd. Legg til dette ved å lagre Valg av utfall i brev.",
        meta = mapOf("behandlingId" to behandlingId.toString()),
    )

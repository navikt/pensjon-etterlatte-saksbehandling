package no.nav.etterlatte.beregning.regler.barnepensjon.trygdetidsfaktor

import no.nav.etterlatte.beregning.regler.AnvendtTrygdetid
import no.nav.etterlatte.beregning.regler.barnepensjon.BP_1967_DATO
import no.nav.etterlatte.beregning.regler.barnepensjon.BarnepensjonGrunnlag
import no.nav.etterlatte.libs.common.beregning.BeregningsMetode
import no.nav.etterlatte.libs.common.beregning.SamletTrygdetidMedBeregningsMetode
import no.nav.etterlatte.libs.regler.Regel
import no.nav.etterlatte.libs.regler.RegelMeta
import no.nav.etterlatte.libs.regler.RegelReferanse
import no.nav.etterlatte.libs.regler.benytter
import no.nav.etterlatte.libs.regler.definerKonstant
import no.nav.etterlatte.libs.regler.finnFaktumIGrunnlag
import no.nav.etterlatte.libs.regler.med
import no.nav.etterlatte.libs.regler.og
import no.nav.etterlatte.regler.Beregningstall

val trygdetidRegel: Regel<BarnepensjonGrunnlag, SamletTrygdetidMedBeregningsMetode> =
    finnFaktumIGrunnlag(
        gjelderFra = BP_1967_DATO,
        beskrivelse = "Finner avdødes trygdetid",
        finnFaktum = BarnepensjonGrunnlag::avdoedesTrygdetid,
        finnFelt = { it },
    )

val nasjonalTrygdetidRegel =
    RegelMeta(
        gjelderFra = BP_1967_DATO,
        beskrivelse = "Finn trygdetid basert på faktisk nasjonal",
        regelReferanse = RegelReferanse(id = "BP-BEREGNING-1967-NASJONAL-TRYGDETID"),
    ) benytter trygdetidRegel med { trygdetid ->
        trygdetid.samletTrygdetidNorge ?: Beregningstall(0.0)
    }

val teoretiskTrygdetidRegel =
    RegelMeta(
        gjelderFra = BP_1967_DATO,
        beskrivelse = "Finn trygdetid basert på faktisk teoretisk og broek",
        regelReferanse = RegelReferanse(id = "BP-BEREGNING-1967-PRORATA-TRYGDETID"),
    ) benytter trygdetidRegel med { trygdetid ->
        trygdetid.samletTrygdetidTeoretisk?.multiply(trygdetid.broek()) ?: Beregningstall(0.0)
    }

val trygdetidBruktRegel =
    RegelMeta(
        gjelderFra = BP_1967_DATO,
        beskrivelse = "Finn trygdetid basert på faktisk, teoretisk og beregningsmetgode",
        regelReferanse = RegelReferanse(id = "BP-BEREGNING-1967-VALGT-TRYGDETID"),
    ) benytter trygdetidRegel og nasjonalTrygdetidRegel og teoretiskTrygdetidRegel med { trygdetid, nasjonal, teoretisk ->
        val nasjonalBeregning = AnvendtTrygdetid(BeregningsMetode.NASJONAL, nasjonal)
        val teoretiskBeregning = AnvendtTrygdetid(BeregningsMetode.PRORATA, teoretisk)

        when (trygdetid.beregningsMetode) {
            BeregningsMetode.NASJONAL -> nasjonalBeregning
            BeregningsMetode.PRORATA -> teoretiskBeregning
            BeregningsMetode.BEST -> {
                maxOf(nasjonalBeregning, teoretiskBeregning) { a, b -> a.trygdetid.compareTo(b.trygdetid) }
            }
        }
    }

val maksTrygdetid =
    definerKonstant<BarnepensjonGrunnlag, Beregningstall>(
        gjelderFra = BP_1967_DATO,
        beskrivelse = "Full trygdetidsopptjening er 40 år",
        regelReferanse = RegelReferanse("BP-BEREGNING-1967-MAKS-TRYGDETID"),
        verdi = Beregningstall(40),
    )

val trygdetidsFaktor =
    RegelMeta(
        gjelderFra = BP_1967_DATO,
        beskrivelse = "Finn trygdetidsfaktor",
        regelReferanse = RegelReferanse(id = "BP-BEREGNING-1967-TRYGDETIDSFAKTOR"),
    ) benytter maksTrygdetid og trygdetidBruktRegel med { maksTrygdetid, trygdetid ->
        minOf(trygdetid.trygdetid, maksTrygdetid).divide(maksTrygdetid)
    }

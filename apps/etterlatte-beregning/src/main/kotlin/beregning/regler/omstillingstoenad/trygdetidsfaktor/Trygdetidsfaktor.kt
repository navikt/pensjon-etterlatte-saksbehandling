package no.nav.etterlatte.beregning.regler.omstillingstoenad.trygdetidsfaktor

import no.nav.etterlatte.beregning.regler.AnvendtTrygdetid
import no.nav.etterlatte.beregning.regler.omstillingstoenad.Avdoed
import no.nav.etterlatte.beregning.regler.omstillingstoenad.OMS_GYLDIG_FRA
import no.nav.etterlatte.beregning.regler.omstillingstoenad.OmstillingstoenadGrunnlag
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

val trygdetidRegel: Regel<OmstillingstoenadGrunnlag, SamletTrygdetidMedBeregningsMetode> =
    finnFaktumIGrunnlag(
        gjelderFra = OMS_GYLDIG_FRA,
        beskrivelse = "Finner avdødes trygdetid",
        finnFaktum = OmstillingstoenadGrunnlag::avdoed,
        finnFelt = Avdoed::trygdetid,
    )

val nasjonalTrygdetidRegel =
    RegelMeta(
        gjelderFra = OMS_GYLDIG_FRA,
        beskrivelse = "Finn trygdetid basert på faktisk nasjonal",
        regelReferanse = RegelReferanse(id = "OMS-BEREGNING-2024-NASJONAL-TRYGDETID"),
    ) benytter trygdetidRegel med { trygdetid ->
        trygdetid.samletTrygdetidNorge ?: Beregningstall(0.0)
    }

val teoretiskTrygdetidRegel =
    RegelMeta(
        gjelderFra = OMS_GYLDIG_FRA,
        beskrivelse = "Finn trygdetid basert på faktisk teoretisk og broek",
        regelReferanse = RegelReferanse(id = "OMS-BEREGNING-2024-PRORATA-TRYGDETID"),
    ) benytter trygdetidRegel med { trygdetid ->
        trygdetid.samletTrygdetidTeoretisk?.multiply(trygdetid.broek()) ?: Beregningstall(0.0)
    }

val trygdetidBruktRegel =
    RegelMeta(
        gjelderFra = OMS_GYLDIG_FRA,
        beskrivelse = "Finn trygdetid basert på faktisk, teoretisk og beregningsmetgode",
        regelReferanse = RegelReferanse(id = "OMS-BEREGNING-2024-VALGT-TRYGDETID"),
    ) benytter trygdetidRegel og nasjonalTrygdetidRegel og teoretiskTrygdetidRegel med {
        trygdetid,
        nasjonal,
        teoretisk,
        ->
        val nasjonalBeregning = AnvendtTrygdetid(BeregningsMetode.NASJONAL, nasjonal, trygdetid.ident)
        val teoretiskBeregning = AnvendtTrygdetid(BeregningsMetode.PRORATA, teoretisk, trygdetid.ident)

        when (trygdetid.beregningsMetode) {
            BeregningsMetode.NASJONAL -> nasjonalBeregning
            BeregningsMetode.PRORATA -> teoretiskBeregning
            BeregningsMetode.BEST -> {
                maxOf(nasjonalBeregning, teoretiskBeregning) { a, b -> a.trygdetid.compareTo(b.trygdetid) }
            }
        }
    }

val maksTrygdetid =
    definerKonstant<OmstillingstoenadGrunnlag, Beregningstall>(
        gjelderFra = OMS_GYLDIG_FRA,
        beskrivelse = "Full trygdetidsopptjening er 40 år",
        regelReferanse = RegelReferanse("OMS-BEREGNING-2024-MAKS-TRYGDETID"),
        verdi = Beregningstall(40),
    )

val trygdetidsFaktor =
    RegelMeta(
        gjelderFra = OMS_GYLDIG_FRA,
        beskrivelse = "Finn trygdetidsfaktor",
        regelReferanse = RegelReferanse(id = "OMS-BEREGNING-2024-TRYGDETIDSFAKTOR"),
    ) benytter maksTrygdetid og trygdetidBruktRegel med {
        maksTrygdetid,
        trygdetid,
        ->
        minOf(trygdetid.trygdetid, maksTrygdetid).divide(maksTrygdetid)
    }

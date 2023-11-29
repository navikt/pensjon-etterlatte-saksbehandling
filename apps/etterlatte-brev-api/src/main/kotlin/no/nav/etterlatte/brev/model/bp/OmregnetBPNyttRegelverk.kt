package no.nav.etterlatte.brev.model.bp

import no.nav.etterlatte.brev.MigreringBrevRequest
import no.nav.etterlatte.brev.behandling.GenerellBrevData
import no.nav.etterlatte.brev.behandling.Utbetalingsinfo
import no.nav.etterlatte.brev.model.BrevData
import no.nav.etterlatte.libs.common.IntBroek
import no.nav.etterlatte.brev.model.Slate
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.UtenlandstilknytningType
import no.nav.pensjon.brevbaker.api.model.Kroner

data class OmregnetBPNyttRegelverk(
    val utbetaltFoerReform: Kroner,
    val utbetaltEtterReform: Kroner,
    val anvendtTrygdetid: Int,
    val prorataBroek: IntBroek?,
    val grunnbeloep: Kroner,
    val erBosattUtlandet: Boolean,
    val erYrkesskade: Boolean,
) : BrevData() {
    companion object {
        fun fra(
            generellBrevData: GenerellBrevData,
            utbetalingsinfo: Utbetalingsinfo,
            migreringRequest: MigreringBrevRequest?,
        ): OmregnetBPNyttRegelverk {
            val foersteBeregningsperiode = utbetalingsinfo.beregningsperioder.first()
            val defaultBrevdataOmregning =
                OmregnetBPNyttRegelverk(
                    utbetaltFoerReform = Kroner(0),
                    utbetaltEtterReform = Kroner(utbetalingsinfo.beloep.value),
                    anvendtTrygdetid = foersteBeregningsperiode.trygdetid,
                    prorataBroek = foersteBeregningsperiode.prorataBroek,
                    grunnbeloep = Kroner(foersteBeregningsperiode.grunnbeloep.value),
                    erBosattUtlandet = false,
                    erYrkesskade = false,
                )

            if (generellBrevData.systemkilde == Vedtaksloesning.PESYS) {
                val pesysUtbetaltFoerReform = migreringRequest?.brutto ?: 0
                val pesysUtenlandstilknytning =
                    requireNotNull(migreringRequest?.utenlandstilknytningType) {
                        "Kan ikke velge mellom bosatt utland eller bosatt norge i brev hvis migreringrequesten mangler grunnlag"
                    }
                return defaultBrevdataOmregning.copy(
                    utbetaltFoerReform = Kroner(pesysUtbetaltFoerReform),
                    erBosattUtlandet = pesysUtenlandstilknytning == UtenlandstilknytningType.BOSATT_UTLAND,
                    erYrkesskade = migreringRequest.yrkesskade,
                )
            }

            return defaultBrevdataOmregning
        }
    }
}

data class OmregnetBPNyttRegelverkFerdig(val innhold: List<Slate.Element>) : BrevData()
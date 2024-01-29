package no.nav.etterlatte.brev.model.bp

import no.nav.etterlatte.brev.MigreringBrevRequest
import no.nav.etterlatte.brev.behandling.GenerellBrevData
import no.nav.etterlatte.brev.behandling.Utbetalingsinfo
import no.nav.etterlatte.brev.model.BrevData
import no.nav.etterlatte.brev.model.InnholdMedVedlegg
import no.nav.etterlatte.brev.model.Slate
import no.nav.etterlatte.libs.common.IntBroek
import no.nav.etterlatte.libs.common.behandling.UtlandstilknytningType
import no.nav.etterlatte.libs.common.person.ForelderVerge
import no.nav.etterlatte.token.Fagsaksystem
import no.nav.pensjon.brevbaker.api.model.Kroner

data class OmregnetBPNyttRegelverk(
    val utbetaltFoerReform: Kroner,
    val utbetaltEtterReform: Kroner,
    val anvendtTrygdetid: Int,
    val prorataBroek: IntBroek?,
    val grunnbeloep: Kroner,
    val erBosattUtlandet: Boolean,
    val erYrkesskade: Boolean,
    val erForeldreloes: Boolean,
    val erUnder18Aar: Boolean,
) : BrevData() {
    companion object {
        fun fra(
            generellBrevData: GenerellBrevData,
            utbetalingsinfo: Utbetalingsinfo,
            migreringRequest: MigreringBrevRequest?,
        ): OmregnetBPNyttRegelverk {
            val foersteBeregningsperiode = utbetalingsinfo.beregningsperioder.first()
            val erUnder18Aar =
                requireNotNull(generellBrevData.personerISak.soeker.under18) {
                    "Klarte ikke å bestemme om alder på søker er under eller over 18 år. Kan dermed ikke velge riktig brev"
                }
            val defaultBrevdataOmregning =
                OmregnetBPNyttRegelverk(
                    utbetaltFoerReform = Kroner(0),
                    utbetaltEtterReform = Kroner(utbetalingsinfo.beloep.value),
                    anvendtTrygdetid = foersteBeregningsperiode.trygdetid,
                    prorataBroek = foersteBeregningsperiode.prorataBroek,
                    grunnbeloep = Kroner(foersteBeregningsperiode.grunnbeloep.value),
                    erBosattUtlandet = false,
                    erYrkesskade = false,
                    erForeldreloes =
                        generellBrevData.personerISak.soeker.foreldreloes ||
                            (
                                generellBrevData.personerISak.avdoede.size > 1 &&
                                    generellBrevData.personerISak.verge !is ForelderVerge
                            ),
                    erUnder18Aar = erUnder18Aar,
                )
            if (generellBrevData.erMigrering()) {
                val pesysUtbetaltFoerReform = migreringRequest?.brutto ?: 0
                val (pesysUtenlandstilknytning, yrkesskade) =
                    when (migreringRequest) {
                        null -> {
                            val utlandstilknytning =
                                requireNotNull(generellBrevData.utlandstilknytning) {
                                    "Mangler utlandstilknytning for behandling=${generellBrevData.behandlingId}"
                                }
                            val yrkesskade = false // Må redigere brev manuelt hvis yrkesskade
                            Pair(utlandstilknytning.type, yrkesskade)
                        }

                        else -> Pair(migreringRequest.utlandstilknytningType, migreringRequest.yrkesskade)
                    }

                if (generellBrevData.forenkletVedtak?.saksbehandlerIdent == Fagsaksystem.EY.navn &&
                    defaultBrevdataOmregning.erForeldreloes
                ) {
                    throw IllegalStateException(
                        "Vi har en automatisk migrering som setter foreldreløs. " +
                            "Dette skal ikke skje, siden dette brevet må redigeres av saksbehandler",
                    )
                }
                return defaultBrevdataOmregning.copy(
                    utbetaltFoerReform = Kroner(pesysUtbetaltFoerReform),
                    erBosattUtlandet = pesysUtenlandstilknytning == UtlandstilknytningType.BOSATT_UTLAND,
                    erYrkesskade = yrkesskade,
                )
            }

            return defaultBrevdataOmregning
        }
    }
}

data class OmregnetBPNyttRegelverkFerdig(
    val innhold: List<Slate.Element>,
    val data: OmregnetBPNyttRegelverk,
) : BrevData() {
    companion object {
        fun fra(
            innhold: InnholdMedVedlegg,
            data: OmregnetBPNyttRegelverk,
        ) = OmregnetBPNyttRegelverkFerdig(
            innhold = innhold.innhold(),
            data = data,
        )
    }
}

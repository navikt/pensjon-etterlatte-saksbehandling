package no.nav.etterlatte.brev.model.bp

import no.nav.etterlatte.brev.MigreringBrevRequest
import no.nav.etterlatte.brev.behandling.GenerellBrevData
import no.nav.etterlatte.brev.behandling.Trygdetid
import no.nav.etterlatte.brev.behandling.Utbetalingsinfo
import no.nav.etterlatte.brev.model.BarnepensjonBeregning
import no.nav.etterlatte.brev.model.BarnepensjonEtterbetaling
import no.nav.etterlatte.brev.model.BrevData
import no.nav.etterlatte.brev.model.Etterbetaling
import no.nav.etterlatte.brev.model.EtterbetalingDTO
import no.nav.etterlatte.brev.model.InnholdMedVedlegg
import no.nav.etterlatte.brev.model.Slate
import no.nav.etterlatte.grunnbeloep.Grunnbeloep
import no.nav.etterlatte.libs.common.behandling.UtlandstilknytningType
import no.nav.etterlatte.libs.common.person.ForelderVerge
import no.nav.etterlatte.token.Fagsaksystem
import no.nav.pensjon.brevbaker.api.model.Kroner

data class OmregnetBPNyttRegelverk(
    val utbetaltFoerReform: Kroner,
    val utbetaltEtterReform: Kroner,
    val erForeldreloes: Boolean,
    val erBosattUtlandet: Boolean,
) : BrevData() {
    companion object {
        fun fra(
            generellBrevData: GenerellBrevData,
            utbetalingsinfo: Utbetalingsinfo,
            migreringRequest: MigreringBrevRequest?,
        ): OmregnetBPNyttRegelverk {
            val defaultBrevdataOmregning =
                OmregnetBPNyttRegelverk(
                    utbetaltFoerReform = Kroner(0),
                    utbetaltEtterReform = Kroner(utbetalingsinfo.beloep.value),
                    erBosattUtlandet = false,
                    erForeldreloes =
                        generellBrevData.personerISak.soeker.foreldreloes ||
                            (
                                generellBrevData.personerISak.avdoede.size > 1 &&
                                    generellBrevData.personerISak.verge !is ForelderVerge
                            ),
                )
            if (generellBrevData.erMigrering()) {
                val pesysUtbetaltFoerReform = migreringRequest?.brutto ?: 0
                val pesysUtenlandstilknytning =
                    migreringRequest?.utlandstilknytningType ?: requireNotNull(generellBrevData.utlandstilknytning) {
                        "Mangler utlandstilknytning for behandling=${generellBrevData.behandlingId}"
                    }.type

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
                )
            }

            return defaultBrevdataOmregning
        }
    }
}

data class OmregnetBPNyttRegelverkFerdig(
    val innhold: List<Slate.Element>,
    val beregning: BarnepensjonBeregning,
    val etterbetaling: BarnepensjonEtterbetaling?,
    val erUnder18Aar: Boolean,
    val erBosattUtlandet: Boolean,
) : BrevData() {
    companion object {
        fun fra(
            innhold: InnholdMedVedlegg,
            erUnder18Aar: Boolean,
            utbetalingsinfo: Utbetalingsinfo,
            trygdetid: Trygdetid,
            grunnbeloep: Grunnbeloep,
            etterbetaling: EtterbetalingDTO?,
            migreringRequest: MigreringBrevRequest?,
            utlandstilknytning: UtlandstilknytningType?,
        ): OmregnetBPNyttRegelverkFerdig {
            val beregningsperioder = barnepensjonBeregningsperiodes(utbetalingsinfo)

            return OmregnetBPNyttRegelverkFerdig(
                innhold = innhold.innhold(),
                erUnder18Aar = erUnder18Aar,
                beregning = barnepensjonBeregning(innhold, utbetalingsinfo, grunnbeloep, beregningsperioder, trygdetid),
                etterbetaling =
                    etterbetaling
                        ?.let { dto -> Etterbetaling.fraBarnepensjonBeregningsperioder(dto, beregningsperioder) },
                erBosattUtlandet =
                    (
                        migreringRequest?.utlandstilknytningType
                            ?: requireNotNull(utlandstilknytning)
                    ) == UtlandstilknytningType.BOSATT_UTLAND,
            )
        }
    }
}

package no.nav.etterlatte.brev.model.bp

import no.nav.etterlatte.brev.behandling.Avdoed
import no.nav.etterlatte.brev.behandling.Utbetalingsinfo
import no.nav.etterlatte.brev.model.BarnepensjonBeregning
import no.nav.etterlatte.brev.model.BarnepensjonEtterbetaling
import no.nav.etterlatte.brev.model.BrevDataFerdigstilling
import no.nav.etterlatte.brev.model.BrevDataRedigerbar
import no.nav.etterlatte.brev.model.Etterbetaling
import no.nav.etterlatte.brev.model.EtterbetalingDTO
import no.nav.etterlatte.brev.model.InnholdMedVedlegg
import no.nav.etterlatte.brev.model.Slate
import no.nav.etterlatte.grunnbeloep.Grunnbeloep
import no.nav.etterlatte.libs.common.behandling.UtlandstilknytningType
import no.nav.etterlatte.libs.common.trygdetid.TrygdetidDto
import no.nav.pensjon.brevbaker.api.model.Kroner
import java.util.UUID

data class BarnepensjonOmregnetNyttRegelverkRedigerbartUtfall(
    val utbetaltFoerReform: Kroner,
    val utbetaltEtterReform: Kroner,
    val erForeldreloes: Boolean,
    val erBosattUtlandet: Boolean,
) : BrevDataRedigerbar {
    companion object {
        fun fra(
            utbetalingsinfo: Utbetalingsinfo,
            loependeIPesys: Boolean,
            utlandstilknytningType: UtlandstilknytningType?,
            behandlingId: UUID?,
            erSystembruker: Boolean,
            erForeldreloes: Boolean,
        ): BarnepensjonOmregnetNyttRegelverkRedigerbartUtfall {
            val defaultBrevdataOmregning =
                BarnepensjonOmregnetNyttRegelverkRedigerbartUtfall(
                    utbetaltFoerReform = Kroner(0),
                    utbetaltEtterReform = Kroner(utbetalingsinfo.beloep.value),
                    erBosattUtlandet = false,
                    erForeldreloes =
                    erForeldreloes,
                )

            // TODO På tide å fjerne?
            if (loependeIPesys) {
                val pesysUtbetaltFoerReform = 0
                val pesysUtenlandstilknytning =
                    requireNotNull(utlandstilknytningType) {
                        "Mangler utlandstilknytning-type for behandling=$behandlingId"
                    }

                if (erSystembruker && defaultBrevdataOmregning.erForeldreloes) {
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

data class BarnepensjonOmregnetNyttRegelverk(
    override val innhold: List<Slate.Element>,
    val beregning: BarnepensjonBeregning,
    val etterbetaling: BarnepensjonEtterbetaling?,
    val erUnder18Aar: Boolean,
    val erBosattUtlandet: Boolean,
) : BrevDataFerdigstilling {
    companion object {
        fun fra(
            innhold: InnholdMedVedlegg,
            erUnder18Aar: Boolean?,
            utbetalingsinfo: Utbetalingsinfo,
            trygdetid: List<TrygdetidDto>,
            grunnbeloep: Grunnbeloep,
            etterbetaling: EtterbetalingDTO?,
            utlandstilknytning: UtlandstilknytningType?,
            avdoede: List<Avdoed>,
        ): BarnepensjonOmregnetNyttRegelverk {
            val erUnder18AarNonNull =
                requireNotNull(erUnder18Aar) {
                    "Klarte ikke å bestemme om alder på søker er under eller over 18 år. Kan dermed ikke velge riktig brev"
                }

            val beregningsperioder = barnepensjonBeregningsperioder(utbetalingsinfo)

            return BarnepensjonOmregnetNyttRegelverk(
                innhold = innhold.innhold(),
                erUnder18Aar = erUnder18AarNonNull,
                beregning =
                    barnepensjonBeregning(
                        innhold,
                        avdoede,
                        utbetalingsinfo,
                        grunnbeloep,
                        beregningsperioder,
                        trygdetid,
                    ),
                etterbetaling =
                    etterbetaling
                        ?.let { dto -> Etterbetaling.fraBarnepensjonBeregningsperioder(dto, beregningsperioder) },
                erBosattUtlandet =
                    (
                        requireNotNull(utlandstilknytning)
                    ) == UtlandstilknytningType.BOSATT_UTLAND,
            )
        }
    }
}

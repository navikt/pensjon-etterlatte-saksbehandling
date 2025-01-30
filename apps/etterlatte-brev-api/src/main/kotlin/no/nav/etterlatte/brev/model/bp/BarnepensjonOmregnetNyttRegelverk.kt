package no.nav.etterlatte.brev.model.bp

import no.nav.etterlatte.brev.BrevDataFerdigstilling
import no.nav.etterlatte.brev.BrevDataRedigerbar
import no.nav.etterlatte.brev.Slate
import no.nav.etterlatte.brev.behandling.Avdoed
import no.nav.etterlatte.brev.behandling.Utbetalingsinfo
import no.nav.etterlatte.brev.model.BarnepensjonBeregning
import no.nav.etterlatte.brev.model.EtterbetalingDTO
import no.nav.etterlatte.brev.model.InnholdMedVedlegg
import no.nav.etterlatte.grunnbeloep.Grunnbeloep
import no.nav.etterlatte.libs.common.behandling.BrevutfallDto
import no.nav.etterlatte.libs.common.behandling.UtlandstilknytningType
import no.nav.etterlatte.libs.common.feilhaandtering.krevIkkeNull
import no.nav.etterlatte.libs.common.kodeverk.LandDto
import no.nav.etterlatte.libs.common.trygdetid.TrygdetidDto
import no.nav.pensjon.brevbaker.api.model.Kroner

data class BarnepensjonOmregnetNyttRegelverkRedigerbartUtfall(
    val utbetaltFoerReform: Kroner,
    val utbetaltEtterReform: Kroner,
    val erForeldreloes: Boolean,
    val erBosattUtlandet: Boolean,
) : BrevDataRedigerbar {
    companion object {
        fun fra(
            utbetalingsinfo: Utbetalingsinfo,
            erForeldreloes: Boolean,
            loependeIPesys: Boolean,
            utlandstilknytningType: UtlandstilknytningType,
            erSystembruker: Boolean,
        ): BarnepensjonOmregnetNyttRegelverkRedigerbartUtfall {
            val defaultBrevdataOmregning =
                BarnepensjonOmregnetNyttRegelverkRedigerbartUtfall(
                    utbetaltFoerReform = Kroner(0),
                    utbetaltEtterReform = Kroner(utbetalingsinfo.beloep.value),
                    erBosattUtlandet = false,
                    erForeldreloes = erForeldreloes,
                )

            // TODO På tide å fjerne?
            if (loependeIPesys) {
                val pesysUtbetaltFoerReform = 0

                if (erSystembruker && defaultBrevdataOmregning.erForeldreloes) {
                    throw IllegalStateException(
                        "Vi har en automatisk migrering som setter foreldreløs. " +
                            "Dette skal ikke skje, siden dette brevet må redigeres av saksbehandler",
                    )
                }
                return defaultBrevdataOmregning.copy(
                    utbetaltFoerReform = Kroner(pesysUtbetaltFoerReform),
                    erBosattUtlandet = utlandstilknytningType == UtlandstilknytningType.BOSATT_UTLAND,
                )
            }

            return defaultBrevdataOmregning
        }
    }
}

data class BarnepensjonOmregnetNyttRegelverk(
    override val innhold: List<Slate.Element>,
    val beregning: BarnepensjonBeregning,
    val frivilligSkattetrekk: Boolean?,
    val erUnder18Aar: Boolean,
    val erBosattUtlandet: Boolean,
    val erEtterbetaling: Boolean,
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
            brevutfall: BrevutfallDto?,
            landKodeverk: List<LandDto>,
        ): BarnepensjonOmregnetNyttRegelverk {
            val erUnder18AarNonNull =
                krevIkkeNull(erUnder18Aar) {
                    "Klarte ikke å bestemme om alder på søker er under eller over 18 år. Kan dermed ikke velge riktig brev"
                }

            return BarnepensjonOmregnetNyttRegelverk(
                innhold = innhold.innhold(),
                erUnder18Aar = erUnder18AarNonNull,
                beregning =
                    barnepensjonBeregning(
                        innhold,
                        avdoede,
                        utbetalingsinfo,
                        grunnbeloep,
                        trygdetid,
                        erForeldreloes = false,
                        landKodeverk,
                    ),
                frivilligSkattetrekk = brevutfall?.frivilligSkattetrekk ?: false,
                erBosattUtlandet =
                    krevIkkeNull(utlandstilknytning) { "Utlandstilknytning mangler" } == UtlandstilknytningType.BOSATT_UTLAND,
                erEtterbetaling = etterbetaling != null,
            )
        }
    }
}

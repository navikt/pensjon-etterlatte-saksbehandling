package no.nav.etterlatte.rivers

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.brev.BREVMAL_RIVER_KEY
import no.nav.etterlatte.brev.BrevHendelseType
import no.nav.etterlatte.brev.BrevRequestHendelseType
import no.nav.etterlatte.brev.Brevkoder
import no.nav.etterlatte.brev.Brevoppretter
import no.nav.etterlatte.brev.behandling.Avdoed
import no.nav.etterlatte.brev.hentinformasjon.grunnlag.GrunnlagService
import no.nav.etterlatte.brev.model.BrevID
import no.nav.etterlatte.brev.model.ManueltBrevData
import no.nav.etterlatte.brev.model.bp.BarnepensjonInformasjonDoedsfall
import no.nav.etterlatte.brev.model.bp.BarnepensjonInformasjonDoedsfallMellomAttenOgTjueVedReformtidspunkt
import no.nav.etterlatte.brev.model.oms.OmstillingsstoenadInformasjonDoedsfall
import no.nav.etterlatte.brev.model.oms.OmstillingsstoenadInntektsjustering
import no.nav.etterlatte.brev.oppgave.OppgaveService
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.rapidsandrivers.setEventNameForHendelseType
import no.nav.etterlatte.libs.common.retryOgPakkUt
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.libs.ktor.token.HardkodaSystembruker
import no.nav.etterlatte.rapidsandrivers.BOR_I_UTLAND_KEY
import no.nav.etterlatte.rapidsandrivers.ER_OVER_18_AAR
import no.nav.etterlatte.rapidsandrivers.EventNames
import no.nav.etterlatte.rapidsandrivers.Kontekst
import no.nav.etterlatte.rapidsandrivers.ListenerMedLogging
import no.nav.etterlatte.rapidsandrivers.SAK_ID_KEY
import no.nav.etterlatte.rapidsandrivers.brevId
import no.nav.etterlatte.rapidsandrivers.sakId
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory

class OpprettJournalfoerOgDistribuerRiverException(
    override val detail: String,
    override val cause: Throwable?,
) : InternfeilException(detail, cause)

class OpprettJournalfoerOgDistribuerRiver(
    rapidsConnection: RapidsConnection,
    private val grunnlagService: GrunnlagService,
    private val oppgaveService: OppgaveService,
    private val brevoppretter: Brevoppretter,
    private val ferdigstillJournalfoerOgDistribuerBrev: FerdigstillJournalfoerOgDistribuerBrev,
) : ListenerMedLogging() {
    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        initialiserRiver(rapidsConnection, BrevRequestHendelseType.OPPRETT_JOURNALFOER_OG_DISTRIBUER) {
            validate { it.requireKey(SAK_ID_KEY) }
            validate { it.requireKey(BREVMAL_RIVER_KEY) }
            validate { it.interestedIn(BOR_I_UTLAND_KEY) }
            validate { it.interestedIn(ER_OVER_18_AAR) }
        }
    }

    override fun kontekst() = Kontekst.BREV

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val brevkode = packet[BREVMAL_RIVER_KEY].asText().let { Brevkoder.valueOf(it) }
        // TODO: prøver å finne fornavn etternavn for Systembruker.brev altså "brev"

        try {
            val (brevID, erDistribuert) =
                runBlocking {
                    opprettJournalfoerOgDistribuer(packet.sakId, brevkode, HardkodaSystembruker.river, packet)
                }

            if (erDistribuert) {
                packet.brevId = brevID
                packet.setEventNameForHendelseType(BrevHendelseType.DISTRIBUERT)
                context.publish(packet.toJson())
            } else {
                runBlocking {
                    oppgaveService.opprettOppgaveForFeiletBrev(packet.sakId, brevID, HardkodaSystembruker.river)
                }

                packet.setEventNameForHendelseType(EventNames.FEILA)
                context.publish(packet.toJson())
            }
        } catch (e: Exception) {
            logger.error(
                "Vi klarte ikke opprette brevet med brevkode $brevkode for sak=${packet.sakId}, på grunn " +
                    "av feil i oppretting av selve brevet. DETTE MÅ FØLGES OPP MANUELT. " +
                    "Dette ble heller ikke håndtert ved at vi laget en oppgave til saksbehandler, siden vi " +
                    "ikke har en brevId (opprettet ingenting), og da ville jeg ikke legge på noe junky data i " +
                    "en flyt jeg ikke kjenner til. Dette er en håndtering enn så lenge som ikke tar ned brev-api.",
                e,
            )

            packet.setEventNameForHendelseType(EventNames.FEILA)
            context.publish(packet.toJson())
        }
    }

    private suspend fun opprettJournalfoerOgDistribuer(
        sakId: SakId,
        brevKode: Brevkoder,
        brukerTokenInfo: BrukerTokenInfo,
        packet: JsonMessage,
    ): Pair<BrevID, Boolean> {
        logger.info("Oppretter $brevKode-brev i sak $sakId")

        val (brev, enhetsnummer) =
            try {
                retryOgPakkUt {
                    val brevdata =
                        when (brevKode) {
                            Brevkoder.BP_INFORMASJON_DOEDSFALL -> {
                                val borIutland = packet.hentVerdiEllerKastFeil(BOR_I_UTLAND_KEY).toBoolean()
                                val erOver18aar = packet.hentVerdiEllerKastFeil(ER_OVER_18_AAR).toBoolean()
                                opprettBarnepensjonInformasjonDoedsfall(sakId, borIutland, erOver18aar)
                            }

                            Brevkoder.BP_INFORMASJON_DOEDSFALL_MELLOM_ATTEN_OG_TJUE_VED_REFORMTIDSPUNKT -> {
                                val borIutland = packet.hentVerdiEllerKastFeil(BOR_I_UTLAND_KEY).toBoolean()
                                opprettBarnepensjonInformasjonDoedsfallMellomAttenOgTjueVedReformtidspunkt(
                                    sakId,
                                    borIutland,
                                )
                            }

                            Brevkoder.OMS_INFORMASJON_DOEDSFALL -> {
                                val borIutland = packet.hentVerdiEllerKastFeil(BOR_I_UTLAND_KEY).toBoolean()
                                opprettOmstillingsstoenadInformasjonDoedsfall(
                                    sakId,
                                    borIutland,
                                )
                            }

                            Brevkoder.OMS_INNTEKTSJUSTERING -> {
                                OmstillingsstoenadInntektsjustering()
                            }

                            else -> ManueltBrevData()
                        }
                    brevoppretter.opprettBrevSomHarInnhold(
                        sakId = sakId,
                        behandlingId = null,
                        bruker = brukerTokenInfo,
                        brevKode = brevKode,
                        brevData = brevdata,
                    )
                }
            } catch (e: Exception) {
                val feilMelding = "Fikk feil ved opprettelse av brev for sak $sakId for brevkode: $brevKode"
                logger.error(feilMelding, e)
                throw OpprettJournalfoerOgDistribuerRiverException(
                    feilMelding,
                    e,
                )
            }

        if (brev.mottaker
                .erGyldig()
                .isNotEmpty()
        ) {
            return Pair(brev.id, false)
        }

        try {
            val brevID =
                ferdigstillJournalfoerOgDistribuerBrev.ferdigstillOgGenererPDF(
                    brevKode,
                    sakId,
                    brukerTokenInfo,
                    enhetsnummer,
                    brev.id,
                )
            ferdigstillJournalfoerOgDistribuerBrev.journalfoerOgDistribuer(
                brevKode,
                sakId,
                brevID,
                brukerTokenInfo,
            )
            return Pair(brevID, true)
        } catch (e: Exception) {
            logger.error("Feil opp sto under ferdigstill/journalfør/distribuer av brevID=${brev.id}...", e)
            return Pair(brev.id, false)
        }
    }

    private suspend fun opprettBarnepensjonInformasjonDoedsfall(
        sakId: SakId,
        borIutland: Boolean,
        erOver18aar: Boolean,
    ) = BarnepensjonInformasjonDoedsfall.fra(
        borIutland,
        erOver18aar,
        hentAvdoede(sakId),
    )

    private suspend fun opprettBarnepensjonInformasjonDoedsfallMellomAttenOgTjueVedReformtidspunkt(
        sakId: SakId,
        borIutland: Boolean,
    ) = BarnepensjonInformasjonDoedsfallMellomAttenOgTjueVedReformtidspunkt.fra(
        borIutland,
        hentAvdoede(sakId),
    )

    private suspend fun opprettOmstillingsstoenadInformasjonDoedsfall(
        sakId: SakId,
        borIutland: Boolean,
    ) = OmstillingsstoenadInformasjonDoedsfall.fra(
        borIutland,
        hentAvdoede(sakId),
    )

    private suspend fun hentAvdoede(sakId: SakId): List<Avdoed> =
        grunnlagService
            .hentPersonerISak(
                grunnlagService.hentGrunnlagForSak(sakId, HardkodaSystembruker.river),
                null,
                null,
            ).avdoede
}

private fun JsonMessage.hentVerdiEllerKastFeil(key: String): String {
    val verdi = this[key].toString()
    if (verdi.isEmpty()) {
        throw RuntimeException("Må ha verdi for key $key")
    } else {
        return verdi
    }
}

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
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.retryOgPakkUt
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.libs.ktor.token.HardkodaSystembruker
import no.nav.etterlatte.rapidsandrivers.BOR_I_UTLAND_KEY
import no.nav.etterlatte.rapidsandrivers.BREV_ID_KEY
import no.nav.etterlatte.rapidsandrivers.BREV_KODE
import no.nav.etterlatte.rapidsandrivers.ER_OVER_18_AAR
import no.nav.etterlatte.rapidsandrivers.Kontekst
import no.nav.etterlatte.rapidsandrivers.ListenerMedLogging
import no.nav.etterlatte.rapidsandrivers.SAK_ID_KEY
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
    private val rapidsConnection: RapidsConnection,
    private val grunnlagService: GrunnlagService,
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
        runBlocking {
            val brevkode = packet[BREVMAL_RIVER_KEY].asText().let { Brevkoder.valueOf(it) }
            // TODO: prøver å finne fornavn etternavn for Systembruker.brev altså "brev"
            val brevId = opprettJournalfoerOgDistribuer(packet.sakId, brevkode, HardkodaSystembruker.river, packet)
            rapidsConnection.svarSuksess(packet.sakId, brevId, brevkode)
        }
    }

    private suspend fun opprettJournalfoerOgDistribuer(
        sakId: Long,
        brevKode: Brevkoder,
        brukerTokenInfo: BrukerTokenInfo,
        packet: JsonMessage,
    ): BrevID {
        logger.info("Oppretter $brevKode-brev i sak $sakId")

        val brevOgData =
            try {
                retryOgPakkUt {
                    brevoppretter.opprettBrev(
                        sakId = sakId,
                        behandlingId = null,
                        bruker = brukerTokenInfo,
                        brevKodeMapping = { brevKode },
                        brevtype = brevKode.brevtype,
                    ) {
                        when (brevKode) {
                            Brevkoder.BP_INFORMASJON_DOEDSFALL -> {
                                val borIutland = packet.hentVerdiEllerKastFeil(BOR_I_UTLAND_KEY).toBoolean()
                                val erOver18aar = packet.hentVerdiEllerKastFeil(ER_OVER_18_AAR).toBoolean()
                                opprettBarnepensjonInformasjonDoedsfall(sakId, borIutland, erOver18aar)
                            }
                            Brevkoder.BP_INFORMASJON_DOEDSFALL_MELLOM_ATTEN_OG_TJUE_VED_REFORMTIDSPUNKT -> {
                                val borIutland = packet.hentVerdiEllerKastFeil(BOR_I_UTLAND_KEY).toBoolean()
                                opprettBarnepensjonInformasjonDoedsfallMellomAttenOgTjueVedReformtidspunkt(sakId, borIutland)
                            }
                            Brevkoder.OMS_INFORMASJON_DOEDSFALL -> {
                                val borIutland = packet.hentVerdiEllerKastFeil(BOR_I_UTLAND_KEY).toBoolean()
                                opprettOmstillingsstoenadInformasjonDoedsfall(
                                    sakId,
                                    borIutland,
                                )
                            }
                            else -> ManueltBrevData()
                        }
                    }
                }
            } catch (e: Exception) {
                val feilMelding = "Fikk feil ved opprettelse av brev for sak $sakId for brevkode: $brevKode"
                logger.error(feilMelding, e)
                throw OpprettJournalfoerOgDistribuerRiverException(
                    feilMelding,
                    e,
                )
            }

        val brevID =
            ferdigstillJournalfoerOgDistribuerBrev.ferdigstillOgGenererPDF(
                brevKode,
                sakId,
                brukerTokenInfo,
                brevOgData.second,
                brevOgData.first.id,
            )
        ferdigstillJournalfoerOgDistribuerBrev.journalfoerOgDistribuer(
            brevKode,
            sakId,
            brevID,
            brukerTokenInfo,
        )
        return brevID
    }

    private fun RapidsConnection.svarSuksess(
        sakId: Long,
        brevID: BrevID,
        brevkode: Brevkoder,
    ) {
        logger.info("Brev har blitt distribuert. Svarer tilbake med bekreftelse.")

        publish(
            sakId.toString(),
            JsonMessage
                .newMessage(
                    BrevHendelseType.DISTRIBUERT.lagEventnameForType(),
                    mapOf(
                        BREV_ID_KEY to brevID,
                        SAK_ID_KEY to sakId,
                        BREV_KODE to brevkode.name,
                    ),
                ).toJson(),
        )
    }

    private suspend fun opprettBarnepensjonInformasjonDoedsfall(
        sakId: Long,
        borIutland: Boolean,
        erOver18aar: Boolean,
    ) = BarnepensjonInformasjonDoedsfall.fra(
        borIutland,
        erOver18aar,
        hentAvdoede(sakId),
    )

    private suspend fun opprettBarnepensjonInformasjonDoedsfallMellomAttenOgTjueVedReformtidspunkt(
        sakId: Long,
        borIutland: Boolean,
    ) = BarnepensjonInformasjonDoedsfallMellomAttenOgTjueVedReformtidspunkt.fra(
        borIutland,
        hentAvdoede(sakId),
    )

    private suspend fun opprettOmstillingsstoenadInformasjonDoedsfall(
        sakId: Long,
        borIutland: Boolean,
    ) = OmstillingsstoenadInformasjonDoedsfall.fra(
        borIutland,
        hentAvdoede(sakId),
    )

    private suspend fun hentAvdoede(sakId: Long): List<Avdoed> =
        grunnlagService.hentPersonerISak(grunnlagService.hentGrunnlagForSak(sakId, HardkodaSystembruker.river), null, null).avdoede
}

private fun JsonMessage.hentVerdiEllerKastFeil(key: String): String {
    val verdi = this[key].toString()
    if (verdi.isEmpty()) {
        throw RuntimeException("Må ha verdi for key $key")
    } else {
        return verdi
    }
}

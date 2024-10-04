package no.nav.etterlatte.rivers

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.brev.BREVMAL_RIVER_KEY
import no.nav.etterlatte.brev.BrevHendelseType
import no.nav.etterlatte.brev.BrevRequestHendelseType
import no.nav.etterlatte.brev.Brevkoder
import no.nav.etterlatte.brev.ManueltBrevData
import no.nav.etterlatte.brev.SaksbehandlerOgAttestant
import no.nav.etterlatte.brev.behandling.Avdoed
import no.nav.etterlatte.brev.behandling.mapAvdoede
import no.nav.etterlatte.brev.model.BarnepensjonInformasjonDoedsfall
import no.nav.etterlatte.brev.model.BarnepensjonInformasjonDoedsfallMellomAttenOgTjueVedReformtidspunkt
import no.nav.etterlatte.brev.model.BrevDistribusjonResponse
import no.nav.etterlatte.brev.model.OmstillingsstoenadInformasjonDoedsfall
import no.nav.etterlatte.brev.model.OmstillingsstoenadInntektsjustering
import no.nav.etterlatte.brev.model.OpprettJournalfoerOgDistribuerRequest
import no.nav.etterlatte.klienter.BrevapiKlient
import no.nav.etterlatte.klienter.GrunnlagKlient
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.rapidsandrivers.setEventNameForHendelseType
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.ktor.token.Fagsaksystem
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
    private val brevapiKlient: BrevapiKlient,
    private val grunnlagKlient: GrunnlagKlient,
    rapidsConnection: RapidsConnection,
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
            val brevErdistribuert = opprettJournalfoerOgDistribuer(packet.sakId, brevkode, packet)
            packet.brevId = brevErdistribuert.brevId
            if (brevErdistribuert.erDistribuert) {
                packet.setEventNameForHendelseType(BrevHendelseType.DISTRIBUERT)
            } else {
                // Oppgave har blitt opprettet i brev-api hvis vi har kommet hit
                packet.setEventNameForHendelseType(EventNames.FEILA)
            }
            context.publish(packet.toJson())
        }
    }

    private suspend fun opprettJournalfoerOgDistribuer(
        sakId: SakId,
        brevKode: Brevkoder,
        packet: JsonMessage,
    ): BrevDistribusjonResponse {
        logger.info("Oppretter $brevKode-brev i sak $sakId")
        val brevdata =
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
                // TODO: @Andreas Balevik
                Brevkoder.OMS_INNTEKTSJUSTERING_VARSEL -> {
                    OmstillingsstoenadInntektsjustering()
                }
                else -> ManueltBrevData()
            }

        try {
            val req =
                OpprettJournalfoerOgDistribuerRequest(
                    brevKode = brevKode,
                    brevDataRedigerbar = brevdata,
                    avsenderRequest = SaksbehandlerOgAttestant(Fagsaksystem.EY.navn, Fagsaksystem.EY.navn),
                    sakId = sakId,
                )
            val brevDistribusjonResponse = brevapiKlient.opprettJournalFoerOgDistribuer(sakId, req)
            return brevDistribusjonResponse
        } catch (e: Exception) {
            val feilMelding = "Fikk feil ved opprettelse av brev for sak $sakId for brevkode: $brevKode"
            logger.error(feilMelding, e)
            throw OpprettJournalfoerOgDistribuerRiverException(
                feilMelding,
                e,
            )
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

    private suspend fun hentAvdoede(sakId: SakId): List<Avdoed> = grunnlagKlient.hentGrunnlagForSak(sakId).mapAvdoede()
}

private fun JsonMessage.hentVerdiEllerKastFeil(key: String): String {
    val verdi = this[key].toString()
    if (verdi.isEmpty()) {
        throw RuntimeException("Må ha verdi for key $key")
    } else {
        return verdi
    }
}

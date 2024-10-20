package no.nav.etterlatte.behandling.omregning

import no.nav.etterlatte.Context
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.behandling.domain.Foerstegangsbehandling
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.kafka.JsonMessage
import no.nav.etterlatte.kafka.KafkaProdusent
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.logging.getCorrelationId
import no.nav.etterlatte.libs.common.rapidsandrivers.CORRELATION_ID_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.TEKNISK_TID_KEY
import no.nav.etterlatte.libs.common.sak.KjoeringStatus
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.rapidsandrivers.HENDELSE_DATA_KEY
import no.nav.etterlatte.rapidsandrivers.OmregningData
import no.nav.etterlatte.rapidsandrivers.OmregningHendelseType
import no.nav.etterlatte.rapidsandrivers.SAK_ID_KEY
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Inntil videre er denne kun opprettet for å trigge omregning av saker som skal ha ny klassifikasjonskode ifm ny kode
 * mot skatt for 2023.
 */
class OmregningJobService(
    private val omregningDao: OmregningDao,
    private val behandlingService: BehandlingService,
    private val kafkaProdusent: KafkaProdusent<String, String>,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun setupKontekstAndRun(context: Context) {
        Kontekst.set(context)
        run()
    }

    private fun run() {
        logger.info("Starter omregning av $antall sak(er) for kjøring $kjoering")

        val sakerTilOmregning: List<Pair<SakId, KjoeringStatus>> =
            inTransaction {
                omregningDao.hentSakerTilOmregning(kjoering, antall)
            }

        logger.info("${sakerTilOmregning.size} sak(er) hentet for omregning")

        sakerTilOmregning.forEach { (sakId, status) ->
            logger.info("Starter omregning av sak $sakId med status $status")

            val foerstegangsbehandling = behandlingService.hentFoerstegangsbehandling(sakId)
            val alleBehandlingerForSak = behandlingService.hentBehandlingerForSak(sakId)

            verifiserAtFoerstegangsbehandlingErIverksatt(foerstegangsbehandling)
            verifiserAtIngenPabegynteBehandlingerFinnes(sakId, alleBehandlingerForSak)

            val foersteVirkningstidspunktForSak = hentFoersteVirkningstidspunktForSak(sakId)
            logger.info("Sak $sakId omregnes med virkningstidspunkt fra $foersteVirkningstidspunktForSak")

            val omregningData =
                OmregningData(
                    kjoering = kjoering,
                    sakId = sakId,
                    revurderingaarsak = Revurderingaarsak.OMREGNING,
                    fradato = foersteVirkningstidspunktForSak,
                ).apply {
                    // Bruker simulering til å se at fattet vedtak ikke medfører etterbetaling eller tilbakekreving
                    endreVerifiserUtbetalingUendret(true)
                }

            logger.info("Publiserer omregningshendelse for sak $sakId på kafka")
            publiserHendelse(omregningData)
        }
    }

    private fun verifiserAtFoerstegangsbehandlingErIverksatt(foerstegangsbehandling: Foerstegangsbehandling) {
        logger.info("Verifiserer at førstegangsbehandling er iverksatt")
        if (foerstegangsbehandling.status != BehandlingStatus.IVERKSATT) {
            throw InternfeilException(
                "Omregning feilet for førstegangsbehandling ${foerstegangsbehandling.id} er ikke iverksatt (status=${foerstegangsbehandling.status})",
            )
        }
    }

    private fun verifiserAtIngenPabegynteBehandlingerFinnes(
        sakId: SakId,
        behandlinger: List<Behandling>,
    ) {
        logger.info("Verifiserer at ingen behandlinger er under arbeid")
        behandlinger.forEach {
            if (it.status !in listOf(BehandlingStatus.IVERKSATT, BehandlingStatus.AVBRUTT, BehandlingStatus.AVSLAG)) {
                throw InternfeilException(
                    "Omregning feilet fordi sak $sakId har en behandling ${it.id} med status ${it.status} - dette er ikke tillatt ved omregning",
                )
            }
        }
    }

    private fun hentFoersteVirkningstidspunktForSak(sakId: SakId): LocalDate {
        val foerstegangsbehandling = behandlingService.hentFoerstegangsbehandling(sakId)
        val foersteVirkningstidspunktForSak =
            foerstegangsbehandling.virkningstidspunkt?.dato?.atDay(1)
                ?: throw InternfeilException("Behandling ${foerstegangsbehandling.id} manglet virkningstidspunkt")
        return foersteVirkningstidspunktForSak
    }

    private fun publiserHendelse(omregningData: OmregningData) {
        val correlationId = getCorrelationId()
        val key = omregningData.sakId.toString()
        val eventType = OmregningHendelseType.KLAR_FOR_OMREGNING.lagEventnameForType()

        val params =
            mapOf(
                SAK_ID_KEY to omregningData.sakId,
                TEKNISK_TID_KEY to LocalDateTime.now(),
                CORRELATION_ID_KEY to correlationId,
                HENDELSE_DATA_KEY to omregningData.toPacket(),
            )

        val (partition, offset) = kafkaProdusent.publiser(key, JsonMessage.newMessage(eventType, params).toJson())

        logger.info(
            "Hendelse med eventType $eventType for sak ${omregningData.sakId} publisert på kafka med partition $partition, " +
                "offset $offset og correlationId $correlationId",
        )
    }

    companion object {
        val kjoering = "ENDRE_KLASSIFIKASJONSKODE_FOR_PERIODER_2023"
        val antall = 1
        val ekskluderteSaker = emptyList<Long>() // TODO
    }
}

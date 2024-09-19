package no.nav.etterlatte.behandling.omregning

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.GrunnlagServiceImpl
import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.behandling.revurdering.AutomatiskRevurderingService
import no.nav.etterlatte.behandling.revurdering.RevurderingOgOppfoelging
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.kafka.JsonMessage
import no.nav.etterlatte.kafka.KafkaProdusent
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.Omregningshendelse
import no.nav.etterlatte.libs.common.behandling.Prosesstype
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.logging.getCorrelationId
import no.nav.etterlatte.libs.common.rapidsandrivers.CORRELATION_ID_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.TEKNISK_TID_KEY
import no.nav.etterlatte.libs.common.retryOgPakkUt
import no.nav.etterlatte.libs.common.sak.KjoeringRequest
import no.nav.etterlatte.libs.common.sak.KjoeringStatus
import no.nav.etterlatte.libs.common.sak.LagreKjoeringRequest
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.rapidsandrivers.BEHANDLING_ID_KEY
import no.nav.etterlatte.rapidsandrivers.BEHANDLING_VI_OMREGNER_FRA_KEY
import no.nav.etterlatte.rapidsandrivers.DATO_KEY
import no.nav.etterlatte.rapidsandrivers.HENDELSE_DATA_KEY
import no.nav.etterlatte.rapidsandrivers.ReguleringHendelseType
import no.nav.etterlatte.rapidsandrivers.SAK_ID_KEY
import no.nav.etterlatte.rapidsandrivers.SAK_TYPE
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class OmregningService(
    private val behandlingService: BehandlingService,
    private val grunnlagService: GrunnlagServiceImpl,
    private val revurderingService: AutomatiskRevurderingService,
    private val omregningDao: OmregningDao,
    private val rapid: KafkaProdusent<String, String>,
) {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    fun hentForrigeBehandling(sakId: SakId) =
        behandlingService.hentSisteIverksatte(sakId)
            ?: throw IllegalArgumentException("Fant ikke forrige behandling i sak $sakId")

    fun hentPersongalleri(id: UUID) = runBlocking { grunnlagService.hentPersongalleri(id) }

    suspend fun opprettOmregning(
        sakId: SakId,
        fraDato: LocalDate,
        revurderingAarsak: Revurderingaarsak,
        prosessType: Prosesstype,
        forrigeBehandling: Behandling,
        oppgavefrist: Tidspunkt?,
    ): RevurderingOgOppfoelging {
        val omregning =
            inTransaction {
                if (prosessType == Prosesstype.MANUELL) {
                    throw StoetterIkkeProsesstypeManuell()
                }

                revurderingService.validerSakensTilstand(sakId, revurderingAarsak)
                requireNotNull(
                    revurderingService.opprettAutomatiskRevurdering(
                        sakId = sakId,
                        forrigeBehandling = forrigeBehandling,
                        revurderingAarsak = revurderingAarsak,
                        virkningstidspunkt = fraDato,
                        kilde = Vedtaksloesning.GJENNY,
                        persongalleri = hentPersongalleri(forrigeBehandling.id),
                        frist = oppgavefrist,
                    ),
                ) { "Opprettelse av revurdering feilet for $sakId" }
            }

        return omregning.also {
            retryOgPakkUt { it.leggInnGrunnlag() }
            retryOgPakkUt {
                inTransaction {
                    it.opprettOgTildelOppgave()
                }
            }
            retryOgPakkUt { it.sendMeldingForHendelse() }
        }
    }

    fun oppdaterKjoering(
        request: KjoeringRequest,
        bruker: BrukerTokenInfo,
    ) {
        if (request.status == KjoeringStatus.FEILA) {
            behandlingService.hentAapenRegulering(request.sakId)?.let {
                behandlingService.avbrytBehandling(it, bruker)
            }
        }
        omregningDao.oppdaterKjoering(request)
    }

    fun kjoeringFullfoert(request: LagreKjoeringRequest) {
        if (request.status != KjoeringStatus.FERDIGSTILT) {
            throw IllegalStateException("Prøver å lagre at kjøring er fullført, men status er ikke ferdigstilt.")
        }
        omregningDao.lagreKjoering(request)
    }

    suspend fun opprettOmregningManuelt(
        sakId: SakId,
        fraDato: LocalDate,
        forrigeBehandling: Behandling,
    ) {
        val hendelseData =
            Omregningshendelse(
                sakId = sakId,
                fradato = fraDato,
                prosesstype = Prosesstype.AUTOMATISK,
                revurderingaarsak = Revurderingaarsak.OMREGNING,
            )

        val omregning =
            opprettOmregning(
                sakId,
                fraDato,
                hendelseData.revurderingaarsak,
                hendelseData.prosesstype,
                forrigeBehandling,
                null,
            ).revurdering

        val correlationId = getCorrelationId()
        val hendelseType = ReguleringHendelseType.BEHANDLING_OPPRETTA.lagEventnameForType()
        rapid
            .publiser(
                omregning.id.toString(),
                JsonMessage
                    .newMessage(
                        hendelseType,
                        mapOf(
                            CORRELATION_ID_KEY to correlationId,
                            TEKNISK_TID_KEY to LocalDateTime.now(),
                            SAK_ID_KEY to sakId,
                            SAK_TYPE to omregning.sak.sakType,
                            BEHANDLING_ID_KEY to omregning.id,
                            BEHANDLING_VI_OMREGNER_FRA_KEY to forrigeBehandling.id,
                            HENDELSE_DATA_KEY to hendelseData,
                            DATO_KEY to fraDato,
                        ),
                    ).toJson(),
            ).also { (partition, offset) ->
                logger.info(
                    "Posted event $hendelseType for under manuell omregning med behandlingsid ${omregning.id}" +
                        " to partiton $partition, offset $offset correlationid: $correlationId",
                )
            }
    }
}

class StoetterIkkeProsesstypeManuell :
    UgyldigForespoerselException(
        code = "StoetterIkkeProsesstypeManuell",
        detail = "Støtter ikke omregning for manuell behandling",
    )

package no.nav.etterlatte.behandling

import no.nav.etterlatte.kafka.JsonMessage
import no.nav.etterlatte.kafka.KafkaProdusent
import no.nav.etterlatte.libs.common.Enhetsnummer
import no.nav.etterlatte.libs.common.aktivitetsplikt.AKTIVITETSPLIKT_DTO_RIVER_KEY
import no.nav.etterlatte.libs.common.aktivitetsplikt.AktivitetspliktDto
import no.nav.etterlatte.libs.common.aktivitetsplikt.AktivitetspliktHendelse
import no.nav.etterlatte.libs.common.behandling.BEHANDLING_ID_PAA_VENT_RIVER_KEY
import no.nav.etterlatte.libs.common.behandling.BehandlingHendelseType
import no.nav.etterlatte.libs.common.behandling.NY_ENHET_KEY
import no.nav.etterlatte.libs.common.behandling.PAA_VENT_AARSAK_KEY
import no.nav.etterlatte.libs.common.behandling.PaaVentAarsak
import no.nav.etterlatte.libs.common.behandling.REFERANSE_ENDRET_ENHET_KEY
import no.nav.etterlatte.libs.common.behandling.STATISTIKKBEHANDLING_RIVER_KEY
import no.nav.etterlatte.libs.common.behandling.StatistikkBehandling
import no.nav.etterlatte.libs.common.logging.getCorrelationId
import no.nav.etterlatte.libs.common.rapidsandrivers.CORRELATION_ID_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.TEKNISK_TID_KEY
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.UUID

interface BehandlingHendelserKafkaProducer {
    fun sendMeldingForHendelseStatistikk(
        statistikkBehandling: StatistikkBehandling,
        hendelseType: BehandlingHendelseType,
        overstyrtTekniskTid: Tidspunkt? = null,
    )

    fun sendMeldingForEndretEnhet(
        oppgaveReferanse: String,
        enhet: Enhetsnummer,
        overstyrtTekniskTid: Tidspunkt? = null,
    )

    fun sendMeldingForHendelsePaaVent(
        behandlingId: UUID,
        hendelseType: BehandlingHendelseType,
        aarsak: PaaVentAarsak,
    )

    fun sendMeldingForHendelseAvVent(
        behandlingId: UUID,
        hendelseType: BehandlingHendelseType,
    )

    fun sendMeldingOmAktivitetsplikt(aktivitetspliktDto: AktivitetspliktDto)
}

class BehandlingsHendelserKafkaProducerImpl(
    private val rapid: KafkaProdusent<String, String>,
) : BehandlingHendelserKafkaProducer {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    override fun sendMeldingForHendelseStatistikk(
        statistikkBehandling: StatistikkBehandling,
        hendelseType: BehandlingHendelseType,
        overstyrtTekniskTid: Tidspunkt?,
    ) {
        val correlationId = getCorrelationId()

        rapid
            .publiser(
                statistikkBehandling.id.toString(),
                JsonMessage
                    .newMessage(
                        hendelseType.lagEventnameForType(),
                        mapOf(
                            CORRELATION_ID_KEY to correlationId,
                            TEKNISK_TID_KEY to (overstyrtTekniskTid ?: Tidspunkt.now()),
                            STATISTIKKBEHANDLING_RIVER_KEY to statistikkBehandling,
                        ),
                    ).toJson(),
            ).also { (partition, offset) ->
                logger.info(
                    "Posted event ${hendelseType.lagEventnameForType()} for behandling ${statistikkBehandling.id}" +
                        " to partiton $partition, offset $offset correlationid: $correlationId",
                )
            }
    }

    override fun sendMeldingForEndretEnhet(
        oppgaveReferanse: String,
        enhet: Enhetsnummer,
        overstyrtTekniskTid: Tidspunkt?,
    ) {
        val correlationId = getCorrelationId()
        val hendelse = BehandlingHendelseType.ENDRET_ENHET
        rapid
            .publiser(
                oppgaveReferanse,
                JsonMessage
                    .newMessage(
                        hendelse.lagEventnameForType(),
                        mapOf(
                            CORRELATION_ID_KEY to correlationId,
                            TEKNISK_TID_KEY to (overstyrtTekniskTid ?: Tidspunkt.now()),
                            REFERANSE_ENDRET_ENHET_KEY to oppgaveReferanse,
                            NY_ENHET_KEY to enhet.enhetNr,
                        ),
                    ).toJson(),
            ).also { (partition, offset) ->
                logger.info(
                    "Postet hendelse ${hendelse.lagEventnameForType()} for oppgave med referanse " +
                        "$oppgaveReferanse til partisjon $partition, offset $offset correlationid: $correlationId",
                )
            }
    }

    override fun sendMeldingForHendelsePaaVent(
        behandlingId: UUID,
        hendelseType: BehandlingHendelseType,
        aarsak: PaaVentAarsak,
    ) {
        val correlationId = getCorrelationId()

        rapid
            .publiser(
                behandlingId.toString(),
                JsonMessage
                    .newMessage(
                        hendelseType.lagEventnameForType(),
                        mapOf(
                            CORRELATION_ID_KEY to correlationId,
                            TEKNISK_TID_KEY to Tidspunkt.now(),
                            BEHANDLING_ID_PAA_VENT_RIVER_KEY to behandlingId,
                            PAA_VENT_AARSAK_KEY to aarsak.name,
                        ),
                    ).toJson(),
            ).also { (partition, offset) ->
                logger.info(
                    "Posted event ${hendelseType.lagEventnameForType()} for behandling $behandlingId" +
                        " to partiton $partition, offset $offset correlationid: $correlationId",
                )
            }
    }

    override fun sendMeldingForHendelseAvVent(
        behandlingId: UUID,
        hendelseType: BehandlingHendelseType,
    ) {
        val correlationId = getCorrelationId()

        rapid
            .publiser(
                behandlingId.toString(),
                JsonMessage
                    .newMessage(
                        hendelseType.lagEventnameForType(),
                        mapOf(
                            CORRELATION_ID_KEY to correlationId,
                            TEKNISK_TID_KEY to Tidspunkt.now(),
                            BEHANDLING_ID_PAA_VENT_RIVER_KEY to behandlingId,
                        ),
                    ).toJson(),
            ).also { (partition, offset) ->
                logger.info(
                    "Posted event ${hendelseType.lagEventnameForType()} for behandling $behandlingId" +
                        " to partiton $partition, offset $offset correlationid: $correlationId",
                )
            }
    }

    override fun sendMeldingOmAktivitetsplikt(aktivitetspliktDto: AktivitetspliktDto) {
        val correlationId = getCorrelationId()
        rapid
            .publiser(
                "aktivitetsplikt-${aktivitetspliktDto.sakId}",
                JsonMessage
                    .newMessage(
                        AktivitetspliktHendelse.OPPDATERT.lagEventnameForType(),
                        mapOf(
                            CORRELATION_ID_KEY to correlationId,
                            TEKNISK_TID_KEY to Tidspunkt.now(),
                            AKTIVITETSPLIKT_DTO_RIVER_KEY to aktivitetspliktDto,
                        ),
                    ).toJson(),
            ).also { (partition, offset) ->
                logger.info(
                    "Sendte hendelse om aktivitetsplikt for sak ${aktivitetspliktDto.sakId} på partition " +
                        "$partition, offset $offset, correlationid: $correlationId",
                )
            }
    }
}

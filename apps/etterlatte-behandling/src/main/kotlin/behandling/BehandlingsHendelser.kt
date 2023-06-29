package no.nav.etterlatte.behandling

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.asContextElement
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import no.nav.etterlatte.Context
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.Self
import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.common.DatabaseContext
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.kafka.JsonMessage
import no.nav.etterlatte.kafka.KafkaProdusent
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.event.BehandlingRiverKey
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.logging.getCorrelationId
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.rapidsandrivers.BEHOV_NAME_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.CORRELATION_ID_KEY
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.Serializable
import java.util.*
import javax.sql.DataSource

enum class BehandlingHendelseType {
    OPPRETTET, AVBRUTT
}

class BehandlingsHendelser(
    private val rapid: KafkaProdusent<String, String>,
    private val behandlingDao: BehandlingDao,
    private val datasource: DataSource
) {
    private val kanal: Channel<Triple<UUID, BehandlingHendelseType, BehandlingType>> = Channel(Channel.UNLIMITED)
    val hendelserKanal: BehandlingHendelserKanal get() = BehandlingHendelserKanal(kanal)

    private val logger: Logger = LoggerFactory.getLogger(BehandlingsHendelser::class.java)

    fun start() {
        @OptIn(DelicateCoroutinesApi::class)
        GlobalScope.launch {
            withContext(
                Dispatchers.Default + Kontekst.asContextElement(
                    value = Context(Self("hendelsespubliserer"), DatabaseContext(datasource))
                )
            ) {
                for (hendelse in kanal) {
                    try {
                        handleEnHendelse(hendelse)
                    } catch (ex: Exception) {
                        logger.warn("Handtering av behandlingshendelse feilet", ex)
                    }
                }
            }
            Kontekst.remove()
        }.invokeOnCompletion {
            rapid.close()
            if (it == null || it is CancellationException) {
                logger.info("BehandlingsHendelser finished")
            } else {
                logger.error("BehandlingsHendelser ended exeptionally", it)
            }
        }
    }

    private fun handleEnHendelse(hendelse: Triple<UUID, BehandlingHendelseType, BehandlingType>) {
        val (id, hendelseType, behandlingType) = hendelse
        val behandling = inTransaction {
            requireNotNull(behandlingDao.hentBehandling(id))
        }
        val correlationId = getCorrelationId()
        if (behandlingType == BehandlingType.REVURDERING && hendelseType == BehandlingHendelseType.OPPRETTET) {
            sendBehovForNyttGrunnlag(behandling)
        }
        rapid.publiser(
            id.toString(),
            JsonMessage.newMessage(
                "BEHANDLING:${hendelseType.name}",
                mapOf(
                    CORRELATION_ID_KEY to correlationId,
                    BehandlingRiverKey.behandlingObjectKey to behandling
                )
            ).toJson()
        ).also { (partition, offset) ->
            logger.info(
                "Posted event BEHANDLING:${hendelse.second.name} for behandling ${hendelse.first}" +
                    " to partiton $partition, offset $offset correlationid: $correlationId"
            )
        }
    }

    private fun sendBehovForNyttGrunnlag(behandling: Behandling) {
        grunnlagsbehov(behandling).forEach {
            rapid.publiser(behandling.id.toString(), it.toJson())
        }
    }

    fun grunnlagsbehov(behandling: Behandling): List<JsonMessage> {
        fun behovForSoeker(fellesInfo: Map<String, Serializable>, sakType: SakType, fnr: String): JsonMessage {
            val rolle = when (sakType) {
                SakType.OMSTILLINGSSTOENAD -> PersonRolle.GJENLEVENDE
                SakType.BARNEPENSJON -> PersonRolle.BARN
            }
            return JsonMessage.newMessage(
                mapOf(BEHOV_NAME_KEY to Opplysningstype.SOEKER_PDL_V1, "fnr" to fnr, "rolle" to rolle) + fellesInfo
            )
        }

        fun behovForAvdoede(fellesInfo: Map<String, Serializable>, fnr: List<String>): List<JsonMessage> {
            return fnr.map {
                JsonMessage.newMessage(
                    mapOf(
                        BEHOV_NAME_KEY to Opplysningstype.AVDOED_PDL_V1,
                        "rolle" to PersonRolle.AVDOED,
                        "fnr" to it
                    ) + fellesInfo
                )
            }
        }

        fun behovForGjenlevende(fellesInfo: Map<String, Serializable>, fnr: List<String>): List<JsonMessage> {
            return fnr.map {
                JsonMessage.newMessage(
                    mapOf(
                        BEHOV_NAME_KEY to Opplysningstype.GJENLEVENDE_FORELDER_PDL_V1,
                        "rolle" to PersonRolle.AVDOED,
                        "fnr" to it
                    ) + fellesInfo
                )
            }
        }

        val behandlingsData = mapOf(
            "sakId" to behandling.sak.id,
            "sakType" to behandling.sak.sakType
        )
        val persongalleri = behandling.persongalleri

        return listOf(behovForSoeker(behandlingsData, behandling.sak.sakType, persongalleri.soeker)) +
            behovForAvdoede(behandlingsData, persongalleri.avdoed) +
            behovForGjenlevende(behandlingsData, persongalleri.gjenlevende)
    }
}
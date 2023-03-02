package no.nav.etterlatte.behandling

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.asContextElement
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import no.nav.etterlatte.Context
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.Self
import no.nav.etterlatte.behandling.domain.Foerstegangsbehandling
import no.nav.etterlatte.behandling.domain.ManueltOpphoer
import no.nav.etterlatte.behandling.domain.Regulering
import no.nav.etterlatte.behandling.domain.Revurdering
import no.nav.etterlatte.behandling.domain.toDetaljertBehandling
import no.nav.etterlatte.common.DatabaseContext
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.kafka.JsonMessage
import no.nav.etterlatte.kafka.KafkaProdusent
import no.nav.etterlatte.libs.common.event.BehandlingGrunnlagEndret
import no.nav.etterlatte.libs.common.logging.getCorrelationId
import no.nav.etterlatte.libs.common.rapidsandrivers.CORRELATION_ID_KEY
import no.nav.etterlatte.sak.SakService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.UUID
import javax.sql.DataSource

enum class BehandlingHendelseType {
    OPPRETTET, GRUNNLAGENDRET, AVBRUTT, GYLDIG_FREMSATT
}

class BehandlingsHendelser(
    private val rapid: KafkaProdusent<String, String>,
    private val behandlingDao: BehandlingDao,
    private val datasource: DataSource,
    private val sakService: SakService
) {
    private val kanal: Channel<Pair<UUID, BehandlingHendelseType>> = Channel(Channel.UNLIMITED)
    val nyHendelse: SendChannel<Pair<UUID, BehandlingHendelseType>> get() = kanal

    private val logger: Logger = LoggerFactory.getLogger(BehandlingsHendelser::class.java)

    fun start() {
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

    private fun handleEnHendelse(hendelse: Pair<UUID, BehandlingHendelseType>) {
        inTransaction {
            val behandling = requireNotNull(behandlingDao.hentBehandling(hendelse.first))

            rapid.publiser(
                hendelse.first.toString(),
                JsonMessage.newMessage(
                    "BEHANDLING:${hendelse.second.name}",
                    mapOf(
                        "behandlingId" to behandling.id,
                        CORRELATION_ID_KEY to getCorrelationId(),
                        BehandlingGrunnlagEndret.behandlingObjectKey to behandling.toDetaljertBehandling(),
                        BehandlingGrunnlagEndret.sakIdKey to behandling.sak.id,
                        BehandlingGrunnlagEndret.sakObjectKey to behandling.sak,
                        BehandlingGrunnlagEndret.behandlingOpprettetKey to behandling.behandlingOpprettet
                    )
                ).also {
                    when (behandling) {
                        is Foerstegangsbehandling -> {
                            it[BehandlingGrunnlagEndret.fnrSoekerKey] = behandling.persongalleri.soeker
                            it[BehandlingGrunnlagEndret.persongalleriKey] = behandling.persongalleri
                        }
                        is Revurdering -> {
                            it[BehandlingGrunnlagEndret.fnrSoekerKey] = behandling.persongalleri.soeker
                            it[BehandlingGrunnlagEndret.persongalleriKey] = behandling.persongalleri
                            it[BehandlingGrunnlagEndret.revurderingAarsakKey] = behandling.revurderingsaarsak
                        }
                        is ManueltOpphoer -> {
                            it[BehandlingGrunnlagEndret.fnrSoekerKey] = behandling.persongalleri.soeker
                            it[BehandlingGrunnlagEndret.persongalleriKey] = behandling.persongalleri
                            it[BehandlingGrunnlagEndret.manueltOpphoerAarsakKey] = behandling.opphoerAarsaker
                            it[BehandlingGrunnlagEndret.manueltOpphoerfritekstAarsakKey] =
                                behandling.fritekstAarsak ?: ""
                        }
                        is Regulering -> {
                            it[BehandlingGrunnlagEndret.fnrSoekerKey] = behandling.persongalleri.soeker
                            it[BehandlingGrunnlagEndret.persongalleriKey] = behandling.persongalleri
                        }
                    }
                }.toJson()
            ).also {
                logger.info(
                    "Posted event ${hendelse.second.name} for behandling ${hendelse.first} to partiton ${it.first}, " +
                        "offset ${it.second}"
                )
            }
        }
    }
}
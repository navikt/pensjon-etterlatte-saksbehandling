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
import no.nav.etterlatte.behandling.foerstegangsbehandling.FoerstegangsbehandlingFactory
import no.nav.etterlatte.behandling.revurdering.RevurderingFactory
import no.nav.etterlatte.database.DatabaseContext
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.kafka.JsonMessage
import no.nav.etterlatte.kafka.KafkaProdusent
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.event.BehandlingGrunnlagEndret
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*
import javax.sql.DataSource

enum class BehandlingHendelseType {
    OPPRETTET, GRUNNLAGENDRET, AVBRUTT
}

class BehandlingsHendelser(
    private val rapid: KafkaProdusent<String, String>,
    private val behandlingDao: BehandlingDao,
    private val foerstegangsbehandlingFactory: FoerstegangsbehandlingFactory,
    private val revurderingFactory: RevurderingFactory,
    private val datasource: DataSource
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
                    when (inTransaction { behandlingDao.hentBehandlingType(hendelse.first) }) {
                        BehandlingType.FØRSTEGANGSBEHANDLING -> {

                            val behandling = inTransaction {
                                foerstegangsbehandlingFactory.hentFoerstegangsbehandling(
                                    hendelse.first
                                )
                            }.serialiserbarUtgave()

                            rapid.publiser(
                                hendelse.first.toString(),
                                JsonMessage.newMessage(
                                    "BEHANDLING:${hendelse.second.name}",
                                    mapOf(
                                        BehandlingGrunnlagEndret.behandlingObjectKey to behandling,
                                        BehandlingGrunnlagEndret.sakIdKey to behandling.sak,
                                        BehandlingGrunnlagEndret.behandlingIdKey to behandling.id,
                                        BehandlingGrunnlagEndret.fnrSoekerKey to behandling.persongalleri.soeker,
                                        BehandlingGrunnlagEndret.behandlingOpprettetKey to behandling.behandlingOpprettet,
                                        BehandlingGrunnlagEndret.persongalleriKey to behandling.persongalleri
                                    )
                                ).toJson()
                            ).also {
                                logger.info("Posted event ${hendelse.second.name} for behandling ${hendelse.first} to partiton ${it.first}, offset ${it.second}")
                            }
                        }
                        BehandlingType.REVURDERING -> {
                            val behandling = inTransaction {
                                revurderingFactory.hentRevurdering(
                                    hendelse.first
                                )
                            }.serialiserbarUtgave()
                            rapid.publiser(
                                hendelse.first.toString(),
                                JsonMessage.newMessage(
                                    "BEHANDLING:${hendelse.second.name}",
                                    mapOf(
                                        BehandlingGrunnlagEndret.behandlingObjectKey to behandling,
                                        BehandlingGrunnlagEndret.sakIdKey to behandling.sak,
                                        BehandlingGrunnlagEndret.behandlingIdKey to behandling.id,
                                        BehandlingGrunnlagEndret.fnrSoekerKey to behandling.persongalleri.soeker,
                                        BehandlingGrunnlagEndret.behandlingOpprettetKey to behandling.behandlingOpprettet,
                                        BehandlingGrunnlagEndret.persongalleriKey to behandling.persongalleri,
                                        BehandlingGrunnlagEndret.revurderingAarsakKey to behandling.revurderingsaarsak
                                    )
                                ).toJson()

                            ).also {
                                logger.info("Posted event ${hendelse.second.name} for behandling ${hendelse.first} to partiton ${it.first}, offset ${it.second}")
                            }
                        }
                        null -> {}
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
}

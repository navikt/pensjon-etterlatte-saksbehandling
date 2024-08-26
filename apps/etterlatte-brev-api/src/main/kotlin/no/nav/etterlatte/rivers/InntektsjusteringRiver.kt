package no.nav.etterlatte.rivers

import no.nav.etterlatte.rapidsandrivers.Kontekst
import no.nav.etterlatte.rapidsandrivers.ListenerMedLoggingOgFeilhaandtering
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory
import rapidsandrivers.InntektsjusteringHendelseType

internal class InntektsjusteringRiver(
    rapidsConnection: RapidsConnection,
) : ListenerMedLoggingOgFeilhaandtering() {
    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        initialiserRiver(rapidsConnection, InntektsjusteringHendelseType.SEND_VARSEL) {
            // TODO: ??
        }
    }

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        /*
        kjoerIBatch(
            logger = logger,
            antall = antall,
            finnSaker = { antallIDenneRunden ->
                behandlingService.hentAlleSaker(
                    kjoering,
                    antallIDenneRunden,
                    spesifikkeSaker,
                    ekskluderteSaker,
                    sakType,
                )
            },
            haandterSaker = { sakerTilOmregning ->
                val sakListe = flyttBehandlingerUnderArbeidTilbakeTilTrygdetidOppdatert(sakerTilOmregning)
                sakerTilOmregning.saker.forEach {
                    publiserSak(it, kjoering, packet, sakListe, context)
                }
            },
        )*/
    }

    // TODO: ???
    override fun kontekst(): Kontekst {
        TODO("Not yet implemented")
    }
}

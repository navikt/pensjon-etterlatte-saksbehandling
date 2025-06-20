package no.nav.etterlatte.vedtaksvurdering.dollybehandling

import no.nav.etterlatte.kafka.KafkaProdusent
import no.nav.etterlatte.rapidsandrivers.Behandlingssteg
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.UUID

class DollyService(
    private val rapid: KafkaProdusent<String, String>,
) {
    private val logger: Logger = LoggerFactory.getLogger(DollyService::class.java)

    fun sendSoeknadFraDolly(
        request: NySoeknadRequest,
        navIdent: String,
    ) {
        val noekkel = UUID.randomUUID()
        logger.info("Publiserer melding om ytelse som skal fra dolly, med nøkkel: $noekkel. Sendt inn av ident $navIdent")
        rapid.publiser(
            noekkel.toString(),
            DollySoeknadMapper
                .opprettJsonMessage(
                    type = request.type,
                    gjenlevendeFnr = request.gjenlevende,
                    avdoedFnr = request.avdoed,
                    barn = request.barn,
                    soeker = request.soeker,
                    behandlingssteg = Behandlingssteg.IVERKSATT,
                ).toJson(),
            mapOf("NavIdent" to (navIdent.toByteArray())),
        )
    }
}

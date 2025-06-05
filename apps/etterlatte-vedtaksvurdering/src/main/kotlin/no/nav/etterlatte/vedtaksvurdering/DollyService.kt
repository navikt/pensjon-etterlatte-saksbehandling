package no.nav.etterlatte.vedtaksvurdering

import no.nav.etterlatte.rapidsandrivers.Behandlingssteg
import java.util.UUID

class DollyService(
    private val rapidService: VedtaksvurderingRapidService,
) {
    fun sendSoeknadFraDolly(
        request: NySoeknadRequest,
        navIdent: String?,
        behandlingssteg: Behandlingssteg,
    ) {
        val noekkel = UUID.randomUUID()

        rapidService.sendTestVedtakToRapid(
            noekkel,
            SoeknadMapper
                .opprettJsonMessage(
                    type = request.type,
                    gjenlevendeFnr = request.gjenlevende,
                    avdoedFnr = request.avdoed,
                    barn = request.barn,
                    soeker = request.soeker,
                    behandlingssteg = behandlingssteg,
                ).toJson(),
            mapOf("NavIdent" to (navIdent!!.toByteArray())),
        )
    }
}

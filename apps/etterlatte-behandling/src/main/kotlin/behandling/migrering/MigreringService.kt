package no.nav.etterlatte.behandling.omregning

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.BehandlingHendelseType
import no.nav.etterlatte.behandling.BehandlingHendelserKanal
import no.nav.etterlatte.behandling.foerstegangsbehandling.FoerstegangsbehandlingFactory
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.sak.SakService
import rapidsandrivers.migrering.MigreringRequest
import java.time.format.DateTimeFormatter
import java.util.*

class MigreringService(
    private val sakService: SakService,
    private val foerstegangsbehandlingFactory: FoerstegangsbehandlingFactory,
    private val behandlingsHendelser: BehandlingHendelserKanal
) {
    fun migrer(request: MigreringRequest): UUID {
        val behandlingId = inTransaction {
            opprettSakOgBehandling(request)
        }
        runBlocking { sendHendelse(behandlingId) }
        return behandlingId
    }

    private fun opprettSakOgBehandling(request: MigreringRequest): UUID =
        foerstegangsbehandlingFactory.opprettFoerstegangsbehandling(
            finnEllerOpprettSak(request).id,
            request.mottattDato.format(DateTimeFormatter.ISO_DATE),
            request.persongalleri
        ).lagretBehandling.id

    private fun finnEllerOpprettSak(request: MigreringRequest) =
        sakService.finnEllerOpprettSak(request.fnr.value, SakType.BARNEPENSJON)

    private suspend fun sendHendelse(behandlingId: UUID) =
        behandlingsHendelser.send(behandlingId to BehandlingHendelseType.OPPRETTET)
}
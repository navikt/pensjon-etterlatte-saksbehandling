package no.nav.etterlatte.behandling.statistikk

import no.nav.etterlatte.behandling.BehandlingHenter
import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.behandling.klienter.GrunnlagKlient
import no.nav.etterlatte.libs.common.behandling.BehandlingForStatistikk
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.token.BrukerTokenInfo
import java.util.UUID

internal class StatistikkService(
    private val grunnlagKlient: GrunnlagKlient,
    private val behandlingHenter: BehandlingHenter,
) {
    suspend fun hentDetaljertBehandling(
        behandlingsId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): BehandlingForStatistikk? =
        behandlingHenter.hentBehandling(behandlingsId)?.let {
            val persongalleri: Persongalleri =
                grunnlagKlient.hentPersongalleri(it.sak.id, brukerTokenInfo)
                    ?.opplysning
                    ?: throw NoSuchElementException("Persongalleri mangler for sak ${it.sak.id}")

            it.toBehandlingForStatistikk(persongalleri)
        }
}

private fun Behandling.toBehandlingForStatistikk(persongalleri: Persongalleri) =
    BehandlingForStatistikk(
        id = id,
        sak = sak.id,
        sakType = sak.sakType,
        behandlingOpprettet = behandlingOpprettet,
        soeknadMottattDato = mottattDato(),
        innsender = persongalleri.innsender,
        soeker = persongalleri.soeker,
        gjenlevende = persongalleri.gjenlevende,
        avdoed = persongalleri.avdoed,
        soesken = persongalleri.soesken,
        status = status,
        behandlingType = type,
        virkningstidspunkt = virkningstidspunkt,
        boddEllerArbeidetUtlandet = boddEllerArbeidetUtlandet,
        revurderingsaarsak = revurderingsaarsak(),
        prosesstype = prosesstype,
        revurderingInfo = revurderingInfo()?.revurderingInfo,
        enhet = sak.enhet,
    )

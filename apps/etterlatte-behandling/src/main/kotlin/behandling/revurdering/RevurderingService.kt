package no.nav.etterlatte.behandling.revurdering

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.BehandlingHendelseType
import no.nav.etterlatte.behandling.BehandlingHendelserKanal
import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.behandling.domain.Revurdering
import no.nav.etterlatte.funksjonsbrytere.FeatureToggle
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak

interface RevurderingService {
    fun opprettRevurdering(forrigeBehandling: Behandling, revurderingAarsak: RevurderingAarsak): Revurdering
}

enum class RevurderingServiceFeatureToggle(private val key: String) : FeatureToggle {
    OpprettManuellRevurdering("pensjon-etterlatte.opprett-manuell-revurdering");

    override fun key() = key
}

class RealRevurderingService(
    private val revurderingFactory: RevurderingFactory,
    private val behandlingHendelser: BehandlingHendelserKanal,
    private val featureToggleService: FeatureToggleService
) : RevurderingService {

    override fun opprettRevurdering(
        forrigeBehandling: Behandling,
        revurderingAarsak: RevurderingAarsak
    ): Revurdering {
        if (featureToggleService.isEnabled(RevurderingServiceFeatureToggle.OpprettManuellRevurdering, false)) {
            return inTransaction {
                revurderingFactory.opprettManuellRevurdering(
                    forrigeBehandling.sak.id,
                    forrigeBehandling,
                    revurderingAarsak
                )
            }
                .also {
                    runBlocking {
                        behandlingHendelser.send(it.lagretBehandling.id to BehandlingHendelseType.OPPRETTET)
                    }
                }.serialiserbarUtgave()
        }

        throw NotImplementedError("Feature togglet av")
    }
}
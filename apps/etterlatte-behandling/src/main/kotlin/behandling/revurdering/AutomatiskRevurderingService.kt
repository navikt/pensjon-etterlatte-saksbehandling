package no.nav.etterlatte.behandling.revurdering

import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.Prosesstype
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.tilVirkningstidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.ktor.token.Fagsaksystem
import java.time.LocalDate
import java.time.YearMonth

class AutomatiskRevurderingService(private val revurderingService: RevurderingService) {
    fun opprettAutomatiskRevurdering(
        sakId: Long,
        forrigeBehandling: Behandling,
        revurderingAarsak: Revurderingaarsak,
        virkningstidspunkt: LocalDate? = null,
        kilde: Vedtaksloesning,
        persongalleri: Persongalleri,
        mottattDato: String? = null,
        begrunnelse: String? = null,
        frist: Tidspunkt? = null,
        opphoerFraOgMed: YearMonth? = null,
    ) = forrigeBehandling.let {
        revurderingService.opprettRevurdering(
            sakId = sakId,
            persongalleri = persongalleri,
            forrigeBehandling = forrigeBehandling.id,
            mottattDato = mottattDato,
            prosessType = Prosesstype.AUTOMATISK,
            kilde = kilde,
            revurderingAarsak = revurderingAarsak,
            virkningstidspunkt = virkningstidspunkt?.tilVirkningstidspunkt("Opprettet automatisk"),
            utlandstilknytning = forrigeBehandling.utlandstilknytning,
            boddEllerArbeidetUtlandet = forrigeBehandling.boddEllerArbeidetUtlandet,
            begrunnelse = begrunnelse ?: "Automatisk revurdering - ${revurderingAarsak.name.lowercase()}",
            saksbehandlerIdent = Fagsaksystem.EY.navn,
            frist = frist,
            // TODO kun relevant for regulering 2024 da feltet opphoerFraOgMed er innført i forkant av reguleringen 2024
            opphoerFraOgMed = forrigeBehandling.opphoerFraOgMed ?: opphoerFraOgMed,
        )
    }

    fun validerSakensTilstand(
        sakId: Long,
        revurderingAarsak: Revurderingaarsak,
    ) {
        if (revurderingAarsak == Revurderingaarsak.ALDERSOVERGANG) {
            revurderingService.maksEnOppgaveUnderbehandlingForKildeBehandling(sakId)
        }
    }
}

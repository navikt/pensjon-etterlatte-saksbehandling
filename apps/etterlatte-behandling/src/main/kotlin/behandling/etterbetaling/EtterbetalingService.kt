package no.nav.etterlatte.behandling.etterbetaling

import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.libs.common.behandling.Etterbetaling
import no.nav.etterlatte.libs.common.feilhaandtering.GenerellIkkeFunnetException
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeTillattException
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

internal class EtterbetalingService(private val dao: EtterbetalingDao, private val behandlingService: BehandlingService) {
    fun lagreEtterbetaling(
        behandlingId: UUID,
        fraDato: LocalDate?,
        tilDato: LocalDate?,
    ) {
        val behandling = hentBehandlingHvisKanEndres(behandlingId)
        val etterbetaling = validerEtterbetaling(behandling, fraDato, tilDato)
        dao.lagreEtterbetaling(etterbetaling)
    }

    fun hentEtterbetaling(behandlingId: UUID): Etterbetaling? = dao.hentEtterbetaling(behandlingId)

    fun slettEtterbetaling(behandlingId: UUID) {
        hentBehandlingHvisKanEndres(behandlingId)
        dao.slettEtterbetaling(behandlingId)
    }

    fun validerEtterbetaling(
        behandling: Behandling,
        fraDato: LocalDate?,
        tilDato: LocalDate?,
    ): Etterbetaling {
        if (fraDato == null || tilDato == null) {
            throw EtterbetalingUgyldigException.ManglerDato
        }
        val fraMaaned = YearMonth.from(fraDato)
        val tilMaaned = YearMonth.from(tilDato)
        if (fraMaaned > tilMaaned) {
            throw EtterbetalingUgyldigException.FraEtterTil
        }
        if (tilMaaned > YearMonth.now()) {
            throw EtterbetalingUgyldigException.TilErFramITid
        }
        val virkningstidspunkt = behandling.virkningstidspunkt ?: throw EtterbetalingUgyldigException.FraFoerVirk
        if (fraMaaned < virkningstidspunkt.dato) {
            throw EtterbetalingUgyldigException.FraFoerVirk
        }

        return Etterbetaling(behandlingId = behandling.id, fra = fraMaaned, til = tilMaaned)
    }

    private fun hentBehandlingHvisKanEndres(behandlingId: UUID): Behandling {
        val behandling = behandlingService.hentBehandling(behandlingId) ?: throw GenerellIkkeFunnetException()
        if (!behandling.status.kanEndres()) {
            throw IkkeTillattException("STATUS_LAAST", "Behandlingen har status ${behandling.status} og kan ikke endres")
        }
        return behandling
    }
}

sealed class EtterbetalingUgyldigException {
    object ManglerDato :
        UgyldigForespoerselException("DATO_ER_NULL", "Etterbetaling må gjelde fra og med til og med en dato.")

    object FraEtterTil : UgyldigForespoerselException("FRA_ETTER_TIL", "Fra-måned kan ikke være etter til-måned.")

    object TilErFramITid : UgyldigForespoerselException("ETTERBETALING_I_FRAMTIDEN", "Til-dato er i framtiden")

    object FraFoerVirk : UgyldigForespoerselException("FRA_FOER_VIRK", "Fra er før virkningstidspunkt")
}

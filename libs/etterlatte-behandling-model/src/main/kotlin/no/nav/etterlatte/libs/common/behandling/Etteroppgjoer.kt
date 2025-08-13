package no.nav.etterlatte.libs.common.behandling

import no.nav.etterlatte.libs.common.event.EventnameHendelseType
import no.nav.etterlatte.libs.common.periode.Periode
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import java.util.UUID

enum class EtteroppgjoerForbehandlingStatus {
    OPPRETTET,
    BEREGNET,
    FERDIGSTILT,
    AVBRUTT,
}

data class EtteroppgjoerForbehandlingDto(
    val id: UUID,
    val hendelseId: UUID,
    val opprettet: Tidspunkt,
    val status: EtteroppgjoerForbehandlingStatus,
    val sak: Sak,
    val aar: Int,
    val innvilgetPeriode: Periode,
    val brevId: Long?,
    val kopiertFra: UUID? = null, // hvis vi oppretter en kopi av forbehandling for å bruke i en revurdering
    val sisteIverksatteBehandlingId: UUID, // siste iverksatte behandling når forbehandling ble opprettet
    val harMottattNyInformasjon: JaNei?,
    val endringErTilUgunstForBruker: JaNei?,
    val beskrivelseAvUgunst: String?,
)

enum class EtteroppgjoerHendelseType(
    val skalSendeStatistikk: Boolean,
) : EventnameHendelseType {
    OPPRETTET(true),
    BEREGNET(false),
    FERDIGSTILT(true),
    AVBRUTT(true),
    ;

    override fun lagEventnameForType(): String = "ETTEROPPGJOER_FORBEHANDLING:${this.name}"
}

const val ETTEROPPGJOER_STATISTIKK_RIVER_KEY = "etteroppgjoer_statistikk"
const val ETTEROPPGJOER_RESULTAT_RIVER_KEY = "etteroppgave_resultat"

data class EtteroppgjoerForbehandlingStatistikkDto(
    val forbehandling: EtteroppgjoerForbehandlingDto,
    val utlandstilknytningType: UtlandstilknytningType?,
    val saksbehandler: String?,
)

data class AvbrytForbehandlingRequest(
    val aarsakTilAvbrytelse: AarsakTilAvbryteForbehandling,
    val kommentar: String?,
)

enum class AarsakTilAvbryteForbehandling {
    IKKE_LENGER_AKTUELL,
    FEILREGISTRERT,
    AVBRUTT_PAA_GRUNN_AV_FEIL,
    ANNET,
}

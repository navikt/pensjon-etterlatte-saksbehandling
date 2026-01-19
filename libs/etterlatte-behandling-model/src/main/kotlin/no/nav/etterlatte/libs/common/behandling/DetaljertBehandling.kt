package no.nav.etterlatte.libs.common.behandling

import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.feilhaandtering.krevIkkeNull
import no.nav.etterlatte.libs.common.sak.SakId
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID

data class DetaljertBehandling(
    val id: UUID,
    val sak: SakId,
    val sakType: SakType,
    val soeker: String,
    val status: BehandlingStatus,
    val behandlingType: BehandlingType,
    val virkningstidspunkt: Virkningstidspunkt?,
    val boddEllerArbeidetUtlandet: BoddEllerArbeidetUtlandet?,
    val utlandstilknytning: Utlandstilknytning?,
    val revurderingsaarsak: Revurderingaarsak? = null,
    val revurderingInfo: RevurderingInfo?,
    val prosesstype: Prosesstype,
    val vedtaksloesning: Vedtaksloesning,
    val sendeBrev: Boolean,
    val opphoerFraOgMed: YearMonth?,
    val relatertBehandlingId: String?,
    val tidligereFamiliepleier: TidligereFamiliepleier?,
    val opprinnelse: BehandlingOpprinnelse,
    val erSluttbehandling: Boolean = false,
    val mottattDato: LocalDateTime? = null,
) {
    fun erBrukermeldtEtteroppgjoer(): Boolean =
        revurderingsaarsak == Revurderingaarsak.ETTEROPPGJOER && opprinnelse != BehandlingOpprinnelse.AUTOMATISK_JOBB

    fun erBosattUtland(): Boolean = utlandstilknytning?.type === UtlandstilknytningType.BOSATT_UTLAND
}

enum class BehandlingOpprinnelse {
    @Deprecated("Ukjent skal kun brukes i tester eller som en verdi hentet ut for gamle behandlinger")
    UKJENT,
    SAKSBEHANDLER,
    MELD_INN_ENDRING_SKJEMA,
    SVAR_I_MODIA,
    BARNEPENSJON_SOEKNAD,
    OMSTILLINGSSTOENAD_SOEKNAD,
    JOURNALFOERING,
    AUTOMATISK_JOBB,
    HENDELSE,
    OMGJOERING,
}

fun DetaljertBehandling.virkningstidspunkt() =
    krevIkkeNull(virkningstidspunkt) {
        "Mangler virkningstidspunkt for behandling=$id"
    }

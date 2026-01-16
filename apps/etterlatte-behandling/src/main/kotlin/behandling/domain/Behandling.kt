package no.nav.etterlatte.behandling.domain

import no.nav.etterlatte.behandling.ViderefoertOpphoer
import no.nav.etterlatte.behandling.revurdering.RevurderingInfoMedBegrunnelse
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.BehandlingOpprinnelse
import no.nav.etterlatte.libs.common.behandling.BehandlingSammendrag
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.BoddEllerArbeidetUtlandet
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.KommerBarnetTilgode
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.Prosesstype
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.StatistikkBehandling
import no.nav.etterlatte.libs.common.behandling.TidligereFamiliepleier
import no.nav.etterlatte.libs.common.behandling.Utlandstilknytning
import no.nav.etterlatte.libs.common.behandling.UtlandstilknytningType
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsResultat
import no.nav.etterlatte.libs.common.sak.Sak
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID

internal sealed class TilstandException(
    open val detail: String,
) : Exception(detail) {
    internal class UgyldigTilstand(
        override val detail: String,
    ) : InternfeilException(detail)

    internal class IkkeFyltUt(
        override val detail: String,
    ) : UgyldigForespoerselException("BEHANDLING_ER_IKKE_FYLT_UT", detail)

    internal class KanIkkeRedigere(
        override val detail: String,
    ) : UgyldigForespoerselException("BEHANDLING_KAN_IKKE_REDIGERES", detail)
}

sealed class Behandling {
    abstract val id: UUID
    abstract val sak: Sak
    abstract val behandlingOpprettet: LocalDateTime
    abstract val sistEndret: LocalDateTime
    abstract val status: BehandlingStatus
    abstract val type: BehandlingType
    abstract val opprinnelse: BehandlingOpprinnelse
    abstract val kommerBarnetTilgode: KommerBarnetTilgode?
    abstract val virkningstidspunkt: Virkningstidspunkt?
    abstract val utlandstilknytning: Utlandstilknytning?
    abstract val boddEllerArbeidetUtlandet: BoddEllerArbeidetUtlandet?
    abstract val soeknadMottattDato: LocalDateTime?
    abstract val vedtaksloesning: Vedtaksloesning
    abstract val sendeBrev: Boolean
    abstract val opphoerFraOgMed: YearMonth?
    abstract val tidligereFamiliepleier: TidligereFamiliepleier?

    abstract fun erSluttbehandling(): Boolean

    open val relatertBehandlingId: String? = null
    open val prosesstype: Prosesstype = Prosesstype.MANUELL

    protected val logger: Logger = LoggerFactory.getLogger(this.javaClass.name)

    private val kanRedigeres: Boolean
        get() = this.status.kanEndres()

    fun mottattDato(): LocalDateTime? =
        when (this) {
            is Foerstegangsbehandling -> {
                this.soeknadMottattDato
            }

            else -> {
                when (revurderingsaarsak()) {
                    Revurderingaarsak.INNTEKTSENDRING -> this.soeknadMottattDato
                    else -> this.behandlingOpprettet
                }
            }
        }

    fun erBosattUtland(): Boolean = utlandstilknytning?.type === UtlandstilknytningType.BOSATT_UTLAND

    fun gyldighetsproeving(): GyldighetsResultat? =
        when (this) {
            is Foerstegangsbehandling -> this.gyldighetsproeving
            else -> null
        }

    fun revurderingsaarsak(): Revurderingaarsak? =
        when (this) {
            is Revurdering -> this.revurderingsaarsak
            else -> null
        }

    fun revurderingInfo(): RevurderingInfoMedBegrunnelse? =
        when (this) {
            is Revurdering -> this.revurderingInfo
            else -> null
        }

    open fun begrunnelse(): String? = null

    open fun oppdaterVirkningstidspunkt(virkningstidspunkt: Virkningstidspunkt): Behandling =
        throw NotImplementedError(
            "Kan ikke oppdatere virkningstidspunkt på behandling $id. " +
                "Denne behandlingstypen støtter ikke oppdatering av virkningstidspunkt.",
        )

    open fun oppdaterBoddEllerArbeidetUtlandet(boddEllerArbeidetUtlandet: BoddEllerArbeidetUtlandet): Behandling =
        throw NotImplementedError(
            "Kan ikke oppdatere bodd eller arbeidet utlandet på behandling $id. " +
                "Denne behandlingstypen støtter ikke oppdatering av bodd eller arbeidet utlandet.",
        )

    open fun oppdaterUtlandstilknytning(utlandstilknytning: Utlandstilknytning): Behandling =
        throw NotImplementedError(
            "Kan ikke oppdatere utlandstilknytning på behandling $id. " +
                "Denne behandlingstypen støtter ikke oppdatering av utlandstilknyting.",
        )

    open fun oppdaterViderefoertOpphoer(viderefoertOpphoer: ViderefoertOpphoer?): Behandling =
        throw NotImplementedError(
            "Kan ikke oppdatere videreført opphør på behandling $id. " +
                "Denne behandlingstypen støtter ikke oppdatering av videreført opphør.",
        )

    open fun oppdaterTidligereFamiliepleier(tidligereFamiliepleier: TidligereFamiliepleier): Behandling =
        throw NotImplementedError(
            "Kan ikke oppdatere tidligere famililiepleier på behandling $id. " +
                "Denne behandlingstypen støtter ikke oppdatering av tidligere famililiepleier.",
        )

    protected fun <T : Behandling> hvisRedigerbar(block: () -> T): T {
        if (kanRedigeres) {
            return block()
        } else {
            throw TilstandException.KanIkkeRedigere("behandling ($id) med status $status kan ikke redigeres")
        }
    }

    protected fun <T : Behandling> hvisTilstandEr(
        behandlingStatus: BehandlingStatus,
        endreTilStatus: BehandlingStatus,
        block: (endreTilStatus: BehandlingStatus) -> T,
    ): T {
        if (status == behandlingStatus) {
            return block(endreTilStatus)
        } else {
            throw TilstandException.UgyldigTilstand(
                "Ugyldig operasjon på behandling ($id) med status $status, prøver å endre til status ${endreTilStatus.name}." +
                    " Forventet status er ${behandlingStatus.name}",
            )
        }
    }

    protected fun <T : Behandling> hvisTilstandEr(
        behandlingStatuser: List<BehandlingStatus>,
        endreTilStatus: BehandlingStatus,
        block: (endreTilStatus: BehandlingStatus) -> T,
    ): T {
        if (status in behandlingStatuser) {
            return block(endreTilStatus)
        } else {
            throw TilstandException.UgyldigTilstand(
                "Ugyldig operasjon på behandling ($id) med status $status, prøver å endre til status ${endreTilStatus.name}." +
                    " Forventet statuser er ${behandlingStatuser.joinToString(",")}",
            )
        }
    }

    fun erAvslagNySoeknad(): Boolean =
        this is Revurdering && this.revurderingsaarsak == Revurderingaarsak.NY_SOEKNAD && this.status == BehandlingStatus.AVSLAG

    open fun tilOpprettet(): Behandling = throw BehandlingStoetterIkkeStatusEndringException(BehandlingStatus.OPPRETTET)

    open fun tilVilkaarsvurdert(): Behandling = throw BehandlingStoetterIkkeStatusEndringException(BehandlingStatus.VILKAARSVURDERT)

    open fun tilTrygdetidOppdatert(): Behandling = throw BehandlingStoetterIkkeStatusEndringException(BehandlingStatus.TRYGDETID_OPPDATERT)

    open fun tilBeregnet(): Behandling = throw BehandlingStoetterIkkeStatusEndringException(BehandlingStatus.BEREGNET)

    open fun tilAvkortet(): Behandling = throw BehandlingStoetterIkkeStatusEndringException(BehandlingStatus.AVKORTET)

    open fun tilFattetVedtak(): Behandling = throw BehandlingStoetterIkkeStatusEndringException(BehandlingStatus.FATTET_VEDTAK)

    open fun tilAttestert(): Behandling = throw BehandlingStoetterIkkeStatusEndringException(BehandlingStatus.ATTESTERT)

    open fun tilAvslag(): Behandling = throw BehandlingStoetterIkkeStatusEndringException(BehandlingStatus.AVSLAG)

    open fun tilReturnert(): Behandling = throw BehandlingStoetterIkkeStatusEndringException(BehandlingStatus.RETURNERT)

    open fun tilTilSamordning(): Behandling = throw BehandlingStoetterIkkeStatusEndringException(BehandlingStatus.TIL_SAMORDNING)

    open fun tilSamordnet(): Behandling = throw BehandlingStoetterIkkeStatusEndringException(BehandlingStatus.SAMORDNET)

    open fun tilIverksatt(): Behandling = throw BehandlingStoetterIkkeStatusEndringException(BehandlingStatus.IVERKSATT)

    open fun tilAttestertIngenEndring(): Behandling =
        throw BehandlingStoetterIkkeStatusEndringException(
            BehandlingStatus.ATTESTERT_INGEN_ENDRING,
        )

    class BehandlingStoetterIkkeStatusEndringException(
        behandlingStatus: BehandlingStatus,
        message: String = "Behandlingen støtter ikke statusendringen til status $behandlingStatus",
    ) : Exception(message)
}

internal fun Behandling.toStatistikkBehandling(
    persongalleri: Persongalleri,
    pesysId: Long? = null,
): StatistikkBehandling =
    StatistikkBehandling(
        id = id,
        sak = sak,
        sistEndret = sistEndret,
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
        kilde = vedtaksloesning,
        pesysId = pesysId,
        relatertBehandlingId = relatertBehandlingId,
        utlandstilknytning = utlandstilknytning,
    )

internal fun Behandling.toDetaljertBehandlingWithPersongalleri(persongalleri: Persongalleri): DetaljertBehandling =
    DetaljertBehandling(
        id = id,
        sak = sak.id,
        sakType = sak.sakType,
        soeker = persongalleri.soeker,
        status = status,
        behandlingType = type,
        virkningstidspunkt = virkningstidspunkt,
        utlandstilknytning = utlandstilknytning,
        boddEllerArbeidetUtlandet = boddEllerArbeidetUtlandet,
        revurderingsaarsak = revurderingsaarsak(),
        prosesstype = prosesstype,
        revurderingInfo = revurderingInfo()?.revurderingInfo,
        vedtaksloesning = vedtaksloesning,
        opprinnelse = opprinnelse,
        sendeBrev = sendeBrev,
        opphoerFraOgMed = opphoerFraOgMed,
        relatertBehandlingId = relatertBehandlingId,
        tidligereFamiliepleier = tidligereFamiliepleier,
        erSluttbehandling = erSluttbehandling(),
        mottattDato = mottattDato(),
    )

fun Behandling.toBehandlingSammendrag() =
    BehandlingSammendrag(
        id = this.id,
        sak = this.sak.id,
        sakType = this.sak.sakType,
        status = this.status,
        soeknadMottattDato = this.mottattDato(),
        behandlingOpprettet = this.behandlingOpprettet,
        behandlingType = this.type,
        aarsak =
            when (this) {
                is Foerstegangsbehandling -> "SOEKNAD"
                is Revurdering -> this.revurderingsaarsak?.name ?: "REVURDERING"
            },
        virkningstidspunkt = this.virkningstidspunkt,
        boddEllerArbeidetUtlandet = this.boddEllerArbeidetUtlandet,
        kilde = this.vedtaksloesning,
    )

internal fun List<Behandling>.hentUtlandstilknytning(): Utlandstilknytning? =
    this
        .filter { it.status != BehandlingStatus.AVBRUTT }
        .maxByOrNull { it.behandlingOpprettet }
        ?.utlandstilknytning

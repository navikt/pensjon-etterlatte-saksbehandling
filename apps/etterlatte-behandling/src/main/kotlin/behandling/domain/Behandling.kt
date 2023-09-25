package no.nav.etterlatte.behandling.domain

import no.nav.etterlatte.behandling.BehandlingSammendrag
import no.nav.etterlatte.behandling.revurdering.RevurderingMedBegrunnelse
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus.ATTESTERT
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus.AVKORTET
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus.BEREGNET
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus.FATTET_VEDTAK
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus.OPPRETTET
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus.RETURNERT
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus.TRYGDETID_OPPDATERT
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus.VILKAARSVURDERT
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.BoddEllerArbeidetUtlandet
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.KommerBarnetTilgode
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.Prosesstype
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import no.nav.etterlatte.libs.common.behandling.Utenlandstilsnitt
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsResultat
import no.nav.etterlatte.libs.common.sak.Sak
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.UUID

internal sealed class TilstandException : IllegalStateException() {
    internal object UgyldigTilstand : TilstandException()
    internal object IkkeFyltUt : TilstandException()
}

sealed class Behandling {
    abstract val id: UUID
    abstract val sak: Sak
    abstract val behandlingOpprettet: LocalDateTime
    abstract val sistEndret: LocalDateTime
    abstract val status: BehandlingStatus
    abstract val type: BehandlingType
    abstract val kommerBarnetTilgode: KommerBarnetTilgode?
    abstract val virkningstidspunkt: Virkningstidspunkt?
    abstract val utenlandstilsnitt: Utenlandstilsnitt?
    abstract val boddEllerArbeidetUtlandet: BoddEllerArbeidetUtlandet?
    abstract val kilde: Vedtaksloesning
    open val prosesstype: Prosesstype = Prosesstype.MANUELL

    protected val logger: Logger = LoggerFactory.getLogger(this.javaClass.name)

    private val kanRedigeres: Boolean
        get() = this.status.kanEndres()

    fun mottattDato(): LocalDateTime? =
        when (this) {
            is Foerstegangsbehandling -> this.soeknadMottattDato
            else -> this.behandlingOpprettet
        }

    fun gyldighetsproeving(): GyldighetsResultat? =
        when (this) {
            is Foerstegangsbehandling -> this.gyldighetsproeving
            else -> null
        }

    fun revurderingsaarsak(): RevurderingAarsak? =
        when (this) {
            is Revurdering -> this.revurderingsaarsak
            else -> null
        }

    fun revurderingInfo(): RevurderingMedBegrunnelse? =
        when (this) {
            is Revurdering -> this.revurderingInfo
            else -> null
        }

    open fun begrunnelse(): String? = null

    open fun oppdaterVirkningstidspunkt(virkningstidspunkt: Virkningstidspunkt): Behandling {
        throw NotImplementedError(
            "Kan ikke oppdatere virkningstidspunkt på behandling $id. " +
                "Denne behandlingstypen støtter ikke oppdatering av virkningstidspunkt.",
        )
    }

    open fun oppdaterUtenlandstilsnitt(utenlandstilsnitt: Utenlandstilsnitt): Behandling {
        throw NotImplementedError(
            "Kan ikke oppdatere utenlandstilsnitt på behandling $id. " +
                "Denne behandlingstypen støtter ikke oppdatering av utenlandstilsnitt.",
        )
    }

    open fun oppdaterBoddEllerArbeidetUtlandnet(boddEllerArbeidetUtlandet: BoddEllerArbeidetUtlandet): Behandling {
        throw NotImplementedError(
            "Kan ikke oppdatere bodd eller arbeidet utlandet på behandling $id. " +
                "Denne behandlingstypen støtter ikke oppdatering av bodd eller arbeidet utlandet.",
        )
    }

    protected fun <T : Behandling> hvisRedigerbar(block: () -> T): T {
        if (kanRedigeres) {
            return block()
        } else {
            logger.info("behandling ($id) med status $status kan ikke redigeres")
            throw TilstandException.UgyldigTilstand
        }
    }

    protected fun <T : Behandling> hvisTilstandEr(
        behandlingStatus: BehandlingStatus,
        block: () -> T,
    ): T {
        if (status == behandlingStatus) {
            return block()
        } else {
            logger.info("Ugyldig operasjon på behandling ($id) med status $status")
            throw TilstandException.UgyldigTilstand
        }
    }

    protected fun <T : Behandling> hvisTilstandEr(
        behandlingStatuser: List<BehandlingStatus>,
        block: () -> T,
    ): T {
        if (status in behandlingStatuser) {
            return block()
        } else {
            logger.info("Ugyldig operasjon på behandling ($id) med status $status")
            throw TilstandException.UgyldigTilstand
        }
    }

    open fun tilOpprettet(): Behandling {
        throw BehandlingStoetterIkkeStatusEndringException(OPPRETTET)
    }

    open fun tilVilkaarsvurdert(): Behandling {
        throw BehandlingStoetterIkkeStatusEndringException(VILKAARSVURDERT)
    }

    open fun tilTrygdetidOppdatert(): Behandling {
        throw BehandlingStoetterIkkeStatusEndringException(TRYGDETID_OPPDATERT)
    }

    open fun tilBeregnet(fastTrygdetid: Boolean): Behandling {
        throw BehandlingStoetterIkkeStatusEndringException(BEREGNET)
    }

    open fun tilAvkortet(): Behandling {
        throw BehandlingStoetterIkkeStatusEndringException(AVKORTET)
    }

    open fun tilFattetVedtak(): Behandling {
        throw BehandlingStoetterIkkeStatusEndringException(FATTET_VEDTAK)
    }

    open fun tilAttestert(): Behandling {
        throw BehandlingStoetterIkkeStatusEndringException(ATTESTERT)
    }

    open fun tilReturnert(): Behandling {
        throw BehandlingStoetterIkkeStatusEndringException(RETURNERT)
    }

    open fun tilIverksatt(): Behandling {
        throw BehandlingStoetterIkkeStatusEndringException(OPPRETTET)
    }

    class BehandlingStoetterIkkeStatusEndringException(
        behandlingStatus: BehandlingStatus,
        message: String = "Behandlingen støtter ikke statusendringen til status $behandlingStatus",
    ) : Exception(message)
}

internal fun Behandling.toDetaljertBehandling(persongalleri: Persongalleri): DetaljertBehandling {
    return DetaljertBehandling(
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
        kilde = kilde,
    )
}

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
                is ManueltOpphoer -> "MANUELT_OPPHOER"
            },
        virkningstidspunkt = this.virkningstidspunkt,
        utenlandstilsnitt = this.utenlandstilsnitt,
        boddEllerArbeidetUtlandet = this.boddEllerArbeidetUtlandet,
    )

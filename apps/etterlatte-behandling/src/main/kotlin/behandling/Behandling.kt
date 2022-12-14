package no.nav.etterlatte.behandling

import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus.ATTESTERT
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus.BEREGNET
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus.FATTET_VEDTAK
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus.IVERKSATT
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus.OPPRETTET
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus.RETURNERT
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus.VILKAARSVURDERT
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.KommerBarnetTilgode
import no.nav.etterlatte.libs.common.behandling.ManueltOpphoerAarsak
import no.nav.etterlatte.libs.common.behandling.OppgaveStatus
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsResultat
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.*

internal sealed class TilstandException : IllegalStateException() {
    internal object UgyldigtTilstand : TilstandException()
    internal object IkkeFyltUt : TilstandException()
}

sealed class Behandling {
    abstract val id: UUID
    abstract val sak: Long
    abstract val behandlingOpprettet: LocalDateTime
    abstract val sistEndret: LocalDateTime
    abstract val status: BehandlingStatus
    abstract val type: BehandlingType
    abstract val persongalleri: Persongalleri
    abstract val kommerBarnetTilgode: KommerBarnetTilgode?

    protected val logger: Logger = LoggerFactory.getLogger(this.javaClass.name)
    val oppgaveStatus get() = OppgaveStatus.from(status)

    private val kanRedigeres: Boolean
        get() = this.status.kanRedigeres()

    protected fun <T : Behandling> hvisRedigerbar(block: () -> T): T {
        if (kanRedigeres) return block() else kastFeilTilstand()
    }

    protected fun <T : Behandling> hvisTilstandEr(behandlingStatus: BehandlingStatus, block: () -> T): T {
        if (status == behandlingStatus) return block() else kastFeilTilstand()
    }

    protected fun <T : Behandling> hvisTilstandEr(behandlingStatuser: List<BehandlingStatus>, block: () -> T): T {
        if (status in behandlingStatuser) return block() else kastFeilTilstand()
    }

    private fun kastFeilTilstand(): Nothing {
        logger.info("kan ikke oppdatere en behandling ($id) som ikke er under behandling")
        throw TilstandException.UgyldigtTilstand
    }
}

data class Foerstegangsbehandling(
    override val id: UUID,
    override val sak: Long,
    override val behandlingOpprettet: LocalDateTime,
    override val sistEndret: LocalDateTime,
    override val status: BehandlingStatus,
    override val persongalleri: Persongalleri,
    override val kommerBarnetTilgode: KommerBarnetTilgode?,
    val virkningstidspunkt: Virkningstidspunkt?,
    val soeknadMottattDato: LocalDateTime,
    val gyldighetsproeving: GyldighetsResultat?
) : Behandling() {
    override val type: BehandlingType = BehandlingType.FØRSTEGANGSBEHANDLING

    private val erFyltUt: Boolean
        get() {
            return (virkningstidspunkt != null) &&
                (gyldighetsproeving != null) &&
                (kommerBarnetTilgode != null)
        }

    fun oppdaterGyldighetsproeving(gyldighetsResultat: GyldighetsResultat): Foerstegangsbehandling = hvisRedigerbar {
        endreTilStatus(OPPRETTET).copy(gyldighetsproeving = gyldighetsResultat)
    }

    fun oppdaterVirkningstidspunkt(dato: YearMonth, kilde: Grunnlagsopplysning.Saksbehandler) =
        hvisRedigerbar {
            endreTilStatus(OPPRETTET).copy(virkningstidspunkt = Virkningstidspunkt(dato, kilde))
        }

    fun oppdaterKommerBarnetTilgode(kommerBarnetTilgode: KommerBarnetTilgode): Foerstegangsbehandling = hvisRedigerbar {
        endreTilStatus(OPPRETTET).copy(kommerBarnetTilgode = kommerBarnetTilgode)
    }

    fun tilVilkaarsvurdert(): Foerstegangsbehandling {
        if (!erFyltUt) {
            logger.info(("Behandling ($id) må være fylt ut for å settes til vilkårsvurdert"))
            throw TilstandException.IkkeFyltUt
        }

        return hvisRedigerbar { endreTilStatus(VILKAARSVURDERT) }
    }

    fun tilBeregnet(): Foerstegangsbehandling = hvisTilstandEr(VILKAARSVURDERT) {
        endreTilStatus(BEREGNET)
    }

    fun tilFattetVedtak(): Foerstegangsbehandling {
        if (!erFyltUt) {
            logger.info(("Behandling ($id) må være fylt ut for å settes til fattet vedtak"))
            throw TilstandException.IkkeFyltUt
        }

        return hvisTilstandEr(listOf(VILKAARSVURDERT, BEREGNET)) {
            endreTilStatus(FATTET_VEDTAK)
        }
    }

    fun tilAttestert() = hvisTilstandEr(FATTET_VEDTAK) {
        endreTilStatus(ATTESTERT)
    }

    fun tilReturnert() = hvisTilstandEr(FATTET_VEDTAK) {
        endreTilStatus(RETURNERT)
    }

    fun tilIverksatt() = hvisTilstandEr(ATTESTERT) {
        endreTilStatus(IVERKSATT)
    }

    /* TODO ai: Legg denne på Behandling-klassen når flere behandlingstyper skal støtte ny behandlingsflyt */
    private fun endreTilStatus(status: BehandlingStatus) =
        this.copy(status = status, sistEndret = LocalDateTime.now())
}

data class Revurdering(
    override val id: UUID,
    override val sak: Long,
    override val behandlingOpprettet: LocalDateTime,
    override val sistEndret: LocalDateTime,
    override val status: BehandlingStatus,
    override val persongalleri: Persongalleri,
    override val kommerBarnetTilgode: KommerBarnetTilgode?,
    val revurderingsaarsak: RevurderingAarsak
) : Behandling() {
    override val type: BehandlingType = BehandlingType.REVURDERING
}

data class ManueltOpphoer(
    override val id: UUID,
    override val sak: Long,
    override val behandlingOpprettet: LocalDateTime,
    override val sistEndret: LocalDateTime,
    override val status: BehandlingStatus,
    override val persongalleri: Persongalleri,
    val opphoerAarsaker: List<ManueltOpphoerAarsak>,
    val fritekstAarsak: String?
) : Behandling() {
    override val type: BehandlingType = BehandlingType.MANUELT_OPPHOER

    constructor(
        sak: Long,
        persongalleri: Persongalleri,
        opphoerAarsaker: List<ManueltOpphoerAarsak>,
        fritekstAarsak: String?
    ) : this(
        id = UUID.randomUUID(),
        sak = sak,
        behandlingOpprettet = LocalDateTime.now(),
        sistEndret = LocalDateTime.now(),
        status = OPPRETTET,
        persongalleri = persongalleri,
        opphoerAarsaker = opphoerAarsaker,
        fritekstAarsak = fritekstAarsak
    )

    override val kommerBarnetTilgode: KommerBarnetTilgode?
        get() = null
}

internal fun Behandling.toDetaljertBehandling(): DetaljertBehandling {
    val (soeknadMottatDato, gyldighetsproeving) = when (this) {
        is Foerstegangsbehandling -> this.soeknadMottattDato to this.gyldighetsproeving
        // TODO øh 24.10.2022 er det riktig at søknadMottatDato er behandlingOpprettet der vi ikke har søknad?
        else -> this.behandlingOpprettet to null
    }

    return DetaljertBehandling(
        id = id,
        sak = sak,
        behandlingOpprettet = behandlingOpprettet,
        sistEndret = sistEndret,
        soeknadMottattDato = soeknadMottatDato,
        innsender = persongalleri.innsender,
        soeker = persongalleri.soeker,
        gjenlevende = persongalleri.gjenlevende,
        avdoed = persongalleri.avdoed,
        soesken = persongalleri.soesken,
        gyldighetsproeving = gyldighetsproeving,
        status = status,
        behandlingType = type,
        virkningstidspunkt = when (this) {
            is Foerstegangsbehandling -> this.virkningstidspunkt
            is ManueltOpphoer -> null
            is Revurdering -> null
        },
        kommerBarnetTilgode = kommerBarnetTilgode,
        revurderingsaarsak = when (this) {
            is Revurdering -> revurderingsaarsak
            else -> null
        }
    )
}
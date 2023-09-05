package no.nav.etterlatte.behandling.revurdering

import io.ktor.server.plugins.BadRequestException
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.behandling.BehandlingDao
import no.nav.etterlatte.behandling.BehandlingHendelseType
import no.nav.etterlatte.behandling.BehandlingHendelserKafkaProducer
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.GrunnlagService
import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.behandling.domain.OpprettBehandling
import no.nav.etterlatte.behandling.domain.Revurdering
import no.nav.etterlatte.behandling.domain.toBehandlingOpprettet
import no.nav.etterlatte.behandling.filterBehandlingerForEnheter
import no.nav.etterlatte.behandling.hendelse.HendelseDao
import no.nav.etterlatte.behandling.kommerbarnettilgode.KommerBarnetTilGodeService
import no.nav.etterlatte.funksjonsbrytere.FeatureToggle
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.grunnlagsendring.GrunnlagsendringshendelseDao
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.Prosesstype
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import no.nav.etterlatte.libs.common.behandling.RevurderingInfo
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.behandling.tilVirkningstidspunkt
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.oppgaveNy.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgaveNy.OppgaveType
import no.nav.etterlatte.libs.common.oppgaveNy.Status
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeUTC
import no.nav.etterlatte.oppgaveny.OppgaveServiceNy
import no.nav.etterlatte.token.Fagsaksystem
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

interface RevurderingService {

    fun opprettManuellRevurderingWrapper(
        sakId: Long,
        aarsak: RevurderingAarsak,
        paaGrunnAvHendelseId: String? = null,
        begrunnelse: String? = null,
        fritekstAarsak: String? = null,
        saksbehandlerIdent: String
    ): Revurdering?

    fun opprettAutomatiskRevurdering(
        sakId: Long,
        forrigeBehandling: Behandling,
        revurderingAarsak: RevurderingAarsak,
        virkningstidspunkt: LocalDate? = null,
        kilde: Vedtaksloesning,
        persongalleri: Persongalleri,
        mottattDato: String? = null,
        merknad: String? = null,
        begrunnelse: String? = null
    ): Revurdering?

    fun lagreRevurderingInfo(behandlingsId: UUID, info: RevurderingInfo, navIdent: String): Boolean
}

enum class RevurderingServiceFeatureToggle(private val key: String) : FeatureToggle {
    OpprettManuellRevurdering("pensjon-etterlatte.opprett-manuell-revurdering");

    override fun key() = key
}

class MaksEnBehandlingsOppgaveUnderbehandlingException(message: String) : Exception(message)

class RevurderingaarsakIkkeStoettetIMiljoeException(message: String) : Exception(message)

class RevurderingManglerIverksattBehandlingException(message: String) : Exception(message)

class RevurderingServiceImpl(
    private val oppgaveService: OppgaveServiceNy,
    private val grunnlagService: GrunnlagService,
    private val behandlingHendelser: BehandlingHendelserKafkaProducer,
    private val featureToggleService: FeatureToggleService,
    private val behandlingDao: BehandlingDao,
    private val hendelseDao: HendelseDao,
    private val grunnlagsendringshendelseDao: GrunnlagsendringshendelseDao,
    private val kommerBarnetTilGodeService: KommerBarnetTilGodeService,
    private val revurderingDao: RevurderingDao,
    private val behandlingService: BehandlingService
) : RevurderingService {
    private val logger = LoggerFactory.getLogger(RevurderingServiceImpl::class.java)

    fun hentBehandling(id: UUID): Revurdering? =
        (behandlingDao.hentBehandling(id) as? Revurdering)?.sjekkEnhet()

    private fun kunEnBehandlingUnderBehandling(sakId: Long) {
        val oppgaverForSak = oppgaveService.hentOppgaverForSak(sakId)
        val ingenBehandlingerUnderarbeid = oppgaverForSak.filter {
            it.kilde == OppgaveKilde.BEHANDLING
        }.none { it.status === Status.UNDER_BEHANDLING }
        if (ingenBehandlingerUnderarbeid) {
            return
        } else {
            throw MaksEnBehandlingsOppgaveUnderbehandlingException(
                "Sak $sakId har allerede en" +
                    " oppgave under behandling"
            )
        }
    }

    override fun opprettManuellRevurderingWrapper(
        sakId: Long,
        aarsak: RevurderingAarsak,
        paaGrunnAvHendelseId: String?,
        begrunnelse: String?,
        fritekstAarsak: String?,
        saksbehandlerIdent: String
    ): Revurdering? {
        if (!aarsak.kanBrukesIMiljo()) {
            throw RevurderingaarsakIkkeStoettetIMiljoeException(
                "Feil revurderingsårsak $aarsak, foreløpig ikke støttet"
            )
        }
        val paaGrunnAvHendelseUuid = try {
            paaGrunnAvHendelseId?.let { UUID.fromString(it) }
        } catch (e: Exception) {
            throw BadRequestException(
                "$aarsak har en ugyldig hendelse id for sakid" +
                    " $sakId. " +
                    "Hendelsesid: $paaGrunnAvHendelseId"
            )
        }

        kunEnBehandlingUnderBehandling(sakId)

        val forrigeIverksatteBehandling = behandlingService.hentSisteIverksatte(sakId)
        if (forrigeIverksatteBehandling != null) {
            val sakType = forrigeIverksatteBehandling.sak.sakType
            if (!aarsak.gyldigForSakType(sakType)) {
                throw BadRequestException("$aarsak er ikke støttet for $sakType")
            }
            return opprettManuellRevurdering(
                sakId = forrigeIverksatteBehandling.sak.id,
                forrigeBehandling = forrigeIverksatteBehandling,
                revurderingAarsak = aarsak,
                paaGrunnAvHendelse = paaGrunnAvHendelseUuid,
                begrunnelse = begrunnelse,
                fritekstAarsak = fritekstAarsak,
                saksbehandlerIdent = saksbehandlerIdent
            )
        } else {
            throw RevurderingManglerIverksattBehandlingException(
                "Kan ikke revurdere en sak uten iverksatt behandling sakid:" +
                    " $sakId"
            )
        }
    }

    private fun opprettManuellRevurdering(
        sakId: Long,
        forrigeBehandling: Behandling,
        revurderingAarsak: RevurderingAarsak,
        paaGrunnAvHendelse: UUID?,
        begrunnelse: String?,
        fritekstAarsak: String?,
        saksbehandlerIdent: String
    ): Revurdering? = forrigeBehandling.sjekkEnhet()?.let {
        return if (featureToggleService.isEnabled(RevurderingServiceFeatureToggle.OpprettManuellRevurdering, false)) {
            val persongalleri = runBlocking { grunnlagService.hentPersongalleri(sakId) }

            inTransaction {
                opprettRevurdering(
                    sakId,
                    persongalleri,
                    forrigeBehandling.id,
                    Tidspunkt.now().toLocalDatetimeUTC().toString(),
                    Prosesstype.MANUELL,
                    Vedtaksloesning.GJENNY,
                    null,
                    revurderingAarsak,
                    virkningstidspunkt = null,
                    begrunnelse = begrunnelse,
                    fritekstAarsak = fritekstAarsak,
                    saksbehandlerIdent = saksbehandlerIdent
                ).also { revurdering ->
                    if (paaGrunnAvHendelse != null) {
                        grunnlagsendringshendelseDao.settBehandlingIdForTattMedIRevurdering(
                            paaGrunnAvHendelse,
                            revurdering.id
                        )
                        try {
                            oppgaveService.ferdigStillOppgaveUnderBehandling(
                                paaGrunnAvHendelse.toString(),
                                saksbehandlerIdent
                            )
                        } catch (e: Exception) {
                            logger.error(
                                "Kunne ikke ferdigstille oppgaven til hendelsen på grunn av feil, " +
                                    "men oppgave er ikke i bruk i miljø så feilen svelges.",
                                e
                            )
                        }
                    }
                }
            }
        } else {
            null
        }
    }

    override fun opprettAutomatiskRevurdering(
        sakId: Long,
        forrigeBehandling: Behandling,
        revurderingAarsak: RevurderingAarsak,
        virkningstidspunkt: LocalDate?,
        kilde: Vedtaksloesning,
        persongalleri: Persongalleri,
        mottattDato: String?,
        merknad: String?,
        begrunnelse: String?
    ) = forrigeBehandling.sjekkEnhet()?.let {
        inTransaction {
            opprettRevurdering(
                sakId,
                persongalleri,
                forrigeBehandling.id,
                mottattDato,
                Prosesstype.AUTOMATISK,
                kilde,
                merknad,
                revurderingAarsak,
                virkningstidspunkt?.tilVirkningstidspunkt("Opprettet automatisk"),
                begrunnelse = begrunnelse ?: "Automatisk revurdering - ${revurderingAarsak.name.lowercase()}",
                saksbehandlerIdent = Fagsaksystem.EY.navn
            )
        }
    }

    private fun kanLagreRevurderingInfo(behandlingsId: UUID): Boolean {
        val behandling = hentBehandling(behandlingsId)
        if (behandling?.type != BehandlingType.REVURDERING) {
            return false
        }
        return behandling.status.kanEndres()
    }

    override fun lagreRevurderingInfo(behandlingsId: UUID, info: RevurderingInfo, navIdent: String): Boolean {
        return inTransaction(true) {
            if (!kanLagreRevurderingInfo(behandlingsId)) {
                return@inTransaction false
            }
            val kilde = Grunnlagsopplysning.Saksbehandler.create(navIdent)
            revurderingDao.lagreRevurderingInfo(behandlingsId, info, kilde)
            return@inTransaction true
        }
    }

    private fun opprettRevurdering(
        sakId: Long,
        persongalleri: Persongalleri,
        forrigeBehandling: UUID?,
        mottattDato: String?,
        prosessType: Prosesstype,
        kilde: Vedtaksloesning,
        merknad: String?,
        revurderingAarsak: RevurderingAarsak,
        virkningstidspunkt: Virkningstidspunkt?,
        begrunnelse: String?,
        fritekstAarsak: String? = null,
        saksbehandlerIdent: String
    ): Revurdering = OpprettBehandling(
        type = BehandlingType.REVURDERING,
        sakId = sakId,
        status = BehandlingStatus.OPPRETTET,
        soeknadMottattDato = mottattDato?.let { LocalDateTime.parse(it) },
        revurderingsAarsak = revurderingAarsak,
        persongalleri = persongalleri,
        virkningstidspunkt = virkningstidspunkt,
        kilde = kilde,
        prosesstype = prosessType,
        merknad = merknad,
        begrunnelse = begrunnelse,
        fritekstAarsak = fritekstAarsak
    ).let { opprettBehandling ->
        behandlingDao.opprettBehandling(opprettBehandling)

        fritekstAarsak?.let {
            lagreRevurderingsaarsakFritekst(fritekstAarsak, opprettBehandling.id, saksbehandlerIdent)
        }

        forrigeBehandling?.let {
            kommerBarnetTilGodeService.hentKommerBarnetTilGode(it)
                ?.copy(behandlingId = opprettBehandling.id)
                ?.let { kopiert -> kommerBarnetTilGodeService.lagreKommerBarnetTilgode(kopiert) }
        }
        hendelseDao.behandlingOpprettet(opprettBehandling.toBehandlingOpprettet())

        logger.info("Opprettet behandling ${opprettBehandling.id} i sak ${opprettBehandling.sakId}")

        behandlingDao.hentBehandling(opprettBehandling.id)!! as Revurdering
    }.also { revurdering ->
        grunnlagService.leggInnNyttGrunnlag(revurdering, persongalleri)

        val oppgave = oppgaveService.opprettNyOppgaveMedSakOgReferanse(
            referanse = revurdering.id.toString(),
            sakId = sakId,
            oppgaveKilde = OppgaveKilde.BEHANDLING,
            oppgaveType = OppgaveType.REVURDERING,
            merknad = begrunnelse
        )
        oppgaveService.tildelSaksbehandler(oppgave.id, saksbehandlerIdent)
        behandlingHendelser.sendMeldingForHendelse(revurdering, BehandlingHendelseType.OPPRETTET)
    }

    private fun lagreRevurderingsaarsakFritekst(
        fritekstAarsak: String,
        behandlingId: UUID,
        saksbehandlerIdent: String
    ) {
        val revurderingInfo = RevurderingInfo.RevurderingAarsakAnnen(fritekstAarsak)
        lagreRevurderingInfo(behandlingId, revurderingInfo, saksbehandlerIdent)
    }

    private fun <T : Behandling> T?.sjekkEnhet() = this?.let { behandling ->
        listOf(behandling).filterBehandlingerForEnheter(
            featureToggleService,
            Kontekst.get().AppUser
        ).firstOrNull()
    }
}
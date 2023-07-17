package no.nav.etterlatte.behandling.revurdering

import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.behandling.BehandlingDao
import no.nav.etterlatte.behandling.BehandlingHendelseType
import no.nav.etterlatte.behandling.BehandlingHendelserKafkaProducer
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
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeUTC
import no.nav.etterlatte.oppgaveny.OppgaveServiceNy
import no.nav.etterlatte.oppgaveny.OppgaveType
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

interface RevurderingService {
    fun opprettManuellRevurdering(
        sakId: Long,
        forrigeBehandling: Behandling,
        revurderingAarsak: RevurderingAarsak,
        kilde: Vedtaksloesning,
        paaGrunnAvHendelse: UUID?
    ): Revurdering?

    fun opprettAutomatiskRevurdering(
        sakId: Long,
        forrigeBehandling: Behandling,
        revurderingAarsak: RevurderingAarsak,
        fraDato: LocalDate,
        kilde: Vedtaksloesning
    ): Revurdering?

    fun lagreRevurderingInfo(behandlingsId: UUID, info: RevurderingInfo, navIdent: String): Boolean

    fun opprettRevurdering(
        sakId: Long,
        persongalleri: Persongalleri,
        forrigeBehandling: UUID?,
        mottattDato: String? = null,
        prosessType: Prosesstype,
        kilde: Vedtaksloesning,
        merknad: String? = null,
        revurderingAarsak: RevurderingAarsak,
        fraDato: Virkningstidspunkt? = null
    ): Revurdering?
}

enum class RevurderingServiceFeatureToggle(private val key: String) : FeatureToggle {
    OpprettManuellRevurdering("pensjon-etterlatte.opprett-manuell-revurdering");

    override fun key() = key
}

class RevurderingServiceImpl(
    private val oppgaveService: OppgaveServiceNy,
    private val grunnlagService: GrunnlagService,
    private val behandlingHendelser: BehandlingHendelserKafkaProducer,
    private val featureToggleService: FeatureToggleService,
    private val behandlingDao: BehandlingDao,
    private val hendelseDao: HendelseDao,
    private val grunnlagsendringshendelseDao: GrunnlagsendringshendelseDao,
    private val kommerBarnetTilGodeService: KommerBarnetTilGodeService
) : RevurderingService {
    private val logger = LoggerFactory.getLogger(RevurderingServiceImpl::class.java)

    fun hentBehandling(id: UUID): Revurdering? =
        (behandlingDao.hentBehandling(id) as? Revurdering)?.sjekkEnhet()

    override fun opprettManuellRevurdering(
        sakId: Long,
        forrigeBehandling: Behandling,
        revurderingAarsak: RevurderingAarsak,
        kilde: Vedtaksloesning,
        paaGrunnAvHendelse: UUID?
    ): Revurdering? = forrigeBehandling.sjekkEnhet()?.let {
        return if (featureToggleService.isEnabled(RevurderingServiceFeatureToggle.OpprettManuellRevurdering, false)) {
            opprettRevurdering(
                sakId,
                forrigeBehandling.persongalleri,
                forrigeBehandling.id,
                Tidspunkt.now().toLocalDatetimeUTC().toString(),
                Prosesstype.MANUELL,
                kilde,
                null,
                revurderingAarsak
            )?.also { revurdering ->
                if (paaGrunnAvHendelse != null) {
                    inTransaction {
                        grunnlagsendringshendelseDao.settBehandlingIdForTattMedIRevurdering(
                            paaGrunnAvHendelse,
                            revurdering.id
                        )
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
        fraDato: LocalDate,
        kilde: Vedtaksloesning
    ) = forrigeBehandling.sjekkEnhet()?.let {
        opprettRevurdering(
            sakId,
            forrigeBehandling.persongalleri,
            forrigeBehandling.id,
            null,
            Prosesstype.AUTOMATISK,
            kilde,
            null,
            revurderingAarsak,
            fraDato.tilVirkningstidspunkt("Opprettet automatisk")
        )
    }

    private fun kanLagreRevurderingInfo(behandlingsId: UUID): Boolean {
        val behandling = hentBehandling(behandlingsId)
        if (behandling?.type != BehandlingType.REVURDERING) {
            return false
        }
        return behandling.status.kanEndres()
    }

    override fun lagreRevurderingInfo(behandlingsId: UUID, info: RevurderingInfo, navIdent: String): Boolean {
        return inTransaction {
            if (!kanLagreRevurderingInfo(behandlingsId)) {
                return@inTransaction false
            }
            val kilde = Grunnlagsopplysning.Saksbehandler.create(navIdent)
            behandlingDao.lagreRevurderingInfo(behandlingsId, info, kilde)
            return@inTransaction true
        }
    }

    override fun opprettRevurdering(
        sakId: Long,
        persongalleri: Persongalleri,
        forrigeBehandling: UUID?,
        mottattDato: String?,
        prosessType: Prosesstype,
        kilde: Vedtaksloesning,
        merknad: String?,
        revurderingAarsak: RevurderingAarsak,
        fraDato: Virkningstidspunkt?
    ): Revurdering? {
        return inTransaction {
            OpprettBehandling(
                type = BehandlingType.REVURDERING,
                sakId = sakId,
                status = BehandlingStatus.OPPRETTET,
                soeknadMottattDato = mottattDato?.let { LocalDateTime.parse(it) },
                revurderingsAarsak = revurderingAarsak,
                persongalleri = persongalleri,
                kilde = kilde,
                prosesstype = prosessType,
                merknad = merknad
            ).let { opprettBehandling ->
                behandlingDao.opprettBehandling(opprettBehandling)
                forrigeBehandling?.let {
                    kommerBarnetTilGodeService.hentKommerBarnetTilGode(it)
                        ?.copy(behandlingId = opprettBehandling.id)
                        ?.let { kopiert -> kommerBarnetTilGodeService.lagreKommerBarnetTilgode(kopiert) }
                }
                hendelseDao.behandlingOpprettet(opprettBehandling.toBehandlingOpprettet())

                logger.info("Opprettet behandling ${opprettBehandling.id} i sak ${opprettBehandling.sakId}")

                behandlingDao.hentBehandling(opprettBehandling.id) as? Revurdering
            }.also { behandling ->
                behandling?.let {
                    grunnlagService.leggInnNyttGrunnlag(it)
                    oppgaveService.opprettNyOppgaveMedSakOgReferanse(
                        referanse = behandling.id.toString(),
                        sakId = sakId,
                        oppgaveType = OppgaveType.REVUDERING
                    )
                    behandlingHendelser.sendMeldingForHendelse(it, BehandlingHendelseType.OPPRETTET)
                }
            }
        }
    }

    private fun <T : Behandling> T?.sjekkEnhet() = this?.let { behandling ->
        listOf(behandling).filterBehandlingerForEnheter(
            featureToggleService,
            Kontekst.get().AppUser
        ).firstOrNull()
    }
}
package no.nav.etterlatte.behandling.revurdering

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.Saksbehandler
import no.nav.etterlatte.behandling.BehandlingDao
import no.nav.etterlatte.behandling.BehandlingHendelseType
import no.nav.etterlatte.behandling.BehandlingHendelserKanal
import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.behandling.domain.OpprettBehandling
import no.nav.etterlatte.behandling.domain.Revurdering
import no.nav.etterlatte.behandling.domain.toBehandlingOpprettet
import no.nav.etterlatte.behandling.filterBehandlingerForEnheter
import no.nav.etterlatte.behandling.hendelse.HendelseDao
import no.nav.etterlatte.funksjonsbrytere.FeatureToggle
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.grunnlagsendring.GrunnlagsendringshendelseDao
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.Prosesstype
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import no.nav.etterlatte.libs.common.behandling.RevurderingInfo
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.behandling.tilVirkningstidspunkt
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import org.slf4j.LoggerFactory
import java.time.LocalDate
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

    fun lagreRevurderingInfo(behandlingsId: UUID, info: RevurderingInfo, saksbehandlerIdent: String): Boolean
}

enum class RevurderingServiceFeatureToggle(private val key: String) : FeatureToggle {
    OpprettManuellRevurdering("pensjon-etterlatte.opprett-manuell-revurdering");

    override fun key() = key
}

class RealRevurderingService(
    private val behandlingHendelser: BehandlingHendelserKanal,
    private val featureToggleService: FeatureToggleService,
    private val behandlingDao: BehandlingDao,
    private val hendelseDao: HendelseDao,
    private val grunnlagsendringshendelseDao: GrunnlagsendringshendelseDao
) : RevurderingService {
    private val logger = LoggerFactory.getLogger(RealRevurderingService::class.java)

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
            inTransaction {
                opprettRevurdering(
                    sakId,
                    forrigeBehandling,
                    revurderingAarsak,
                    null,
                    Prosesstype.MANUELL,
                    kilde
                )?.also { revurdering ->
                    if (paaGrunnAvHendelse != null) {
                        grunnlagsendringshendelseDao.settBehandlingIdForTattMedIBehandling(
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
        inTransaction {
            opprettRevurdering(
                sakId,
                forrigeBehandling,
                revurderingAarsak,
                fraDato.tilVirkningstidspunkt("Opprettet automatisk"),
                Prosesstype.AUTOMATISK,
                kilde
            )
        }
    }

    override fun lagreRevurderingInfo(behandlingsId: UUID, info: RevurderingInfo, saksbehandler: String): Boolean {
        return inTransaction {
            val behandling = hentBehandling(behandlingsId)
            if (behandling?.type != BehandlingType.REVURDERING || !behandling.status.kanEndres()) {
                return@inTransaction false
            }
            val kilde = Grunnlagsopplysning.Saksbehandler.create(saksbehandler)
            behandlingDao.lagreRevurderingInfo(behandlingsId, info, kilde)
            return@inTransaction true
        }
    }

    private fun opprettRevurdering(
        sakId: Long,
        forrigeBehandling: Behandling,
        revurderingAarsak: RevurderingAarsak,
        fraDato: Virkningstidspunkt?,
        prosessType: Prosesstype,
        kilde: Vedtaksloesning
    ) = OpprettBehandling(
        type = BehandlingType.REVURDERING,
        sakId = sakId,
        status = BehandlingStatus.OPPRETTET,
        persongalleri = forrigeBehandling.persongalleri,
        revurderingsAarsak = revurderingAarsak,
        kommerBarnetTilgode = forrigeBehandling.kommerBarnetTilgode,
        virkningstidspunkt = fraDato,
        prosesstype = prosessType,
        kilde = kilde
    ).let { opprettBehandling ->
        behandlingDao.opprettBehandling(opprettBehandling)
        hendelseDao.behandlingOpprettet(opprettBehandling.toBehandlingOpprettet())
        logger.info("Opprettet revurdering ${opprettBehandling.id} i sak ${opprettBehandling.sakId}")

        hentBehandling(opprettBehandling.id)
    }.also { revurdering ->
        revurdering?.let {
            runBlocking {
                behandlingHendelser.send(revurdering.id to BehandlingHendelseType.OPPRETTET)
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
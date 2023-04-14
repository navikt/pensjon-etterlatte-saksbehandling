package no.nav.etterlatte.behandling.revurdering

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.BehandlingDao
import no.nav.etterlatte.behandling.BehandlingHendelseType
import no.nav.etterlatte.behandling.BehandlingHendelserKanal
import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.behandling.domain.OpprettBehandling
import no.nav.etterlatte.behandling.domain.Revurdering
import no.nav.etterlatte.behandling.domain.toBehandlingOpprettet
import no.nav.etterlatte.behandling.hendelse.HendelseDao
import no.nav.etterlatte.funksjonsbrytere.FeatureToggle
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.Prosesstype
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import no.nav.etterlatte.libs.common.behandling.tilVirkningstidspunkt
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.util.UUID

interface RevurderingService {
    fun opprettManuellRevurdering(
        sakId: Long,
        forrigeBehandling: Behandling,
        revurderingAarsak: RevurderingAarsak
    ): Revurdering

    fun opprettAutomatiskRevurdering(
        sakId: Long,
        forrigeBehandling: Behandling,
        revurderingAarsak: RevurderingAarsak,
        fraDato: LocalDate
    ): Revurdering
}

enum class RevurderingServiceFeatureToggle(private val key: String) : FeatureToggle {
    OpprettManuellRevurdering("pensjon-etterlatte.opprett-manuell-revurdering");

    override fun key() = key
}

class RealRevurderingService(
    private val behandlingHendelser: BehandlingHendelserKanal,
    private val featureToggleService: FeatureToggleService,
    private val behandlingDao: BehandlingDao,
    private val hendelseDao: HendelseDao
) : RevurderingService {
    private val logger = LoggerFactory.getLogger(RealRevurderingService::class.java)

    fun hentBehandling(id: UUID): Revurdering = requireNotNull(
        behandlingDao.hentBehandling(id) as Revurdering
    )

    override fun opprettManuellRevurdering(
        sakId: Long,
        forrigeBehandling: Behandling,
        revurderingAarsak: RevurderingAarsak
    ): Revurdering {
        if (featureToggleService.isEnabled(RevurderingServiceFeatureToggle.OpprettManuellRevurdering, false)) {
            return inTransaction {
                opprettRevurdering(
                    sakId,
                    forrigeBehandling,
                    revurderingAarsak,
                    null,
                    Prosesstype.MANUELL
                )
            }
        }

        throw NotImplementedError("Feature togglet av")
    }

    override fun opprettAutomatiskRevurdering(
        sakId: Long,
        forrigeBehandling: Behandling,
        revurderingAarsak: RevurderingAarsak,
        fraDato: LocalDate
    ): Revurdering {
        return inTransaction {
            opprettRevurdering(
                sakId,
                forrigeBehandling,
                revurderingAarsak,
                fraDato,
                Prosesstype.AUTOMATISK
            )
        }
    }

    private fun opprettRevurdering(
        sakId: Long,
        forrigeBehandling: Behandling,
        revurderingAarsak: RevurderingAarsak,
        fraDato: LocalDate?,
        prosessType: Prosesstype
    ) = OpprettBehandling(
        type = BehandlingType.REVURDERING,
        sakId = sakId,
        status = BehandlingStatus.OPPRETTET,
        persongalleri = forrigeBehandling.persongalleri,
        revurderingsAarsak = revurderingAarsak,
        kommerBarnetTilgode = forrigeBehandling.kommerBarnetTilgode,
        virkningstidspunkt = fraDato?.tilVirkningstidspunkt("Opprettet automatisk"),
        prosesstype = prosessType
    ).let { opprettBehandling ->
        behandlingDao.opprettBehandling(opprettBehandling)
        hendelseDao.behandlingOpprettet(opprettBehandling.toBehandlingOpprettet())
        logger.info("Opprettet revurdering ${opprettBehandling.id} i sak ${opprettBehandling.sakId}")

        hentBehandling(opprettBehandling.id)
    }.also {
        runBlocking {
            behandlingHendelser.send(it.id to BehandlingHendelseType.OPPRETTET)
        }
    }
}
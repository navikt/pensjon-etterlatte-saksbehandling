package no.nav.etterlatte.behandling.revurdering

import no.nav.etterlatte.behandling.BehandlingDao
import no.nav.etterlatte.behandling.Revurdering
import no.nav.etterlatte.behandling.foerstegangsbehandling.FoerstegangsbehandlingAggregat
import no.nav.etterlatte.behandling.hendelse.HendelseDao
import no.nav.etterlatte.behandling.hendelse.HendelseType
import no.nav.etterlatte.behandling.hendelse.registrerVedtakHendelseFelles
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.OppgaveStatus
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*

class RevurderingAggregat(
    id: UUID,
    private val behandlinger: BehandlingDao,
    private val hendelser: HendelseDao
) {
    companion object {
        private val logger = LoggerFactory.getLogger(FoerstegangsbehandlingAggregat::class.java)

        fun opprettRevurdering(
            sak: Long,
            persongalleri: Persongalleri,
            revurderingAarsak: RevurderingAarsak,
            behandlinger: BehandlingDao,
            hendelser: HendelseDao
        ): RevurderingAggregat {
            logger.info("Oppretter en behandling på $sak")
            return Revurdering(
                id = UUID.randomUUID(),
                sak = sak,
                behandlingOpprettet = LocalDateTime.now(),
                sistEndret = LocalDateTime.now(),
                status = BehandlingStatus.OPPRETTET,
                persongalleri = persongalleri,
                oppgaveStatus = OppgaveStatus.NY,
                revurderingsaarsak = revurderingAarsak,
                kommerBarnetTilgode = null
            )
                .also {
                    behandlinger.opprettRevurdering(it)
                    hendelser.behandlingOpprettet(it)
                    logger.info("Opprettet revurdering ${it.id} i sak ${it.sak}")
                }
                .let { RevurderingAggregat(it.id, behandlinger, hendelser) }
        }
    }

    var lagretBehandling: Revurdering =
        requireNotNull(behandlinger.hentBehandling(id, BehandlingType.REVURDERING) as Revurdering)

    fun registrerVedtakHendelse(
        vedtakId: Long,
        hendelse: HendelseType,
        inntruffet: Tidspunkt,
        saksbehandler: String?,
        kommentar: String?,
        begrunnelse: String?
    ) {
        lagretBehandling = registrerVedtakHendelseFelles(
            vedtakId = vedtakId,
            hendelse = hendelse,
            inntruffet = inntruffet,
            saksbehandler = saksbehandler,
            kommentar = kommentar,
            begrunnelse = begrunnelse,
            lagretBehandling = lagretBehandling,
            behandlinger = behandlinger,
            hendelser = hendelser
        ) as Revurdering
    }

    fun serialiserbarUtgave() = lagretBehandling.copy()
}
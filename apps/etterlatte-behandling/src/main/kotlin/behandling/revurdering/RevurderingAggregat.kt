package no.nav.etterlatte.behandling.revurdering

import no.nav.etterlatte.behandling.BehandlingDao
import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.behandling.domain.OpprettBehandling
import no.nav.etterlatte.behandling.domain.Revurdering
import no.nav.etterlatte.behandling.domain.toBehandlingOpprettet
import no.nav.etterlatte.behandling.foerstegangsbehandling.FoerstegangsbehandlingAggregat
import no.nav.etterlatte.behandling.hendelse.HendelseDao
import no.nav.etterlatte.behandling.hendelse.HendelseType
import no.nav.etterlatte.behandling.hendelse.registrerVedtakHendelseFelles
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.Prosesstype
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import no.nav.etterlatte.libs.common.behandling.tilVirkningstidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.util.*

class RevurderingAggregat(
    id: UUID,
    behandlinger: BehandlingDao,
    private val hendelser: HendelseDao
) {
    companion object {
        private val logger = LoggerFactory.getLogger(FoerstegangsbehandlingAggregat::class.java)

        fun opprettManuellRevurdering(
            sak: Long,
            forrigeBehandling: Behandling,
            revurderingAarsak: RevurderingAarsak,
            behandlinger: BehandlingDao,
            hendelser: HendelseDao
        ): RevurderingAggregat {
            logger.info("Oppretter en behandling på sak $sak")
            return OpprettBehandling(
                type = BehandlingType.REVURDERING,
                sakId = sak,
                status = BehandlingStatus.OPPRETTET,
                persongalleri = forrigeBehandling.persongalleri,
                revurderingsAarsak = revurderingAarsak,
                kommerBarnetTilgode = forrigeBehandling.kommerBarnetTilgode,
                vilkaarUtfall = forrigeBehandling.vilkaarUtfall,
                virkningstidspunkt = null,
                prosesstype = Prosesstype.MANUELL
            )
                .also {
                    behandlinger.opprettBehandling(it)
                    hendelser.behandlingOpprettet(it.toBehandlingOpprettet())
                    logger.info("Opprettet revurdering ${it.id} i sak ${it.sakId}")
                }
                .let { RevurderingAggregat(it.id, behandlinger, hendelser) }
        }

        fun opprettAutomatiskRevurdering(
            sak: Long,
            fraDato: LocalDate,
            forrigeBehandling: Behandling,
            revurderingAarsak: RevurderingAarsak,
            behandlinger: BehandlingDao,
            hendelser: HendelseDao
        ): RevurderingAggregat {
            logger.info("Oppretter en automatisk revurdering på sak $sak")
            return OpprettBehandling(
                type = BehandlingType.REVURDERING,
                sakId = sak,
                status = BehandlingStatus.OPPRETTET,
                persongalleri = forrigeBehandling.persongalleri,
                revurderingsAarsak = revurderingAarsak,
                kommerBarnetTilgode = forrigeBehandling.kommerBarnetTilgode,
                vilkaarUtfall = forrigeBehandling.vilkaarUtfall,
                virkningstidspunkt = fraDato.tilVirkningstidspunkt("Opprettet automatisk"),
                prosesstype = Prosesstype.AUTOMATISK
            )
                .also {
                    behandlinger.opprettBehandling(it)
                    hendelser.behandlingOpprettet(it.toBehandlingOpprettet())
                    logger.info("Opprettet revurdering ${it.id} i sak ${it.sakId}")
                }
                .let { RevurderingAggregat(it.id, behandlinger, hendelser) }
        }
    }

    var lagretBehandling = requireNotNull(behandlinger.hentBehandling(id) as Revurdering)

    fun registrerVedtakHendelse(
        vedtakId: Long,
        hendelse: HendelseType,
        inntruffet: Tidspunkt,
        saksbehandler: String?,
        kommentar: String?,
        begrunnelse: String?
    ) {
        registrerVedtakHendelseFelles(
            vedtakId = vedtakId,
            hendelse = hendelse,
            inntruffet = inntruffet,
            saksbehandler = saksbehandler,
            kommentar = kommentar,
            begrunnelse = begrunnelse,
            lagretBehandling = lagretBehandling,
            hendelser = hendelser
        )
    }

    fun serialiserbarUtgave() = lagretBehandling.kopier()
}
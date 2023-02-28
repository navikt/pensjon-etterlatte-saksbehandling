package no.nav.etterlatte.behandling.revurdering

import no.nav.etterlatte.behandling.BehandlingDao
import no.nav.etterlatte.behandling.domain.OpprettBehandling
import no.nav.etterlatte.behandling.domain.Revurdering
import no.nav.etterlatte.behandling.domain.toBehandlingOpprettet
import no.nav.etterlatte.behandling.foerstegangsbehandling.FoerstegangsbehandlingAggregat
import no.nav.etterlatte.behandling.hendelse.HendelseDao
import no.nav.etterlatte.behandling.hendelse.HendelseType
import no.nav.etterlatte.behandling.hendelse.registrerVedtakHendelseFelles
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import org.slf4j.LoggerFactory
import java.util.*

class RevurderingAggregat(
    id: UUID,
    behandlinger: BehandlingDao,
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
            return OpprettBehandling(
                type = BehandlingType.REVURDERING,
                sakId = sak,
                status = BehandlingStatus.OPPRETTET,
                persongalleri = persongalleri,
                revurderingsAarsak = revurderingAarsak,
                kommerBarnetTilgode = null,
                vilkaarUtfall = null,
                virkningstidspunkt = null
            )
                .also {
                    behandlinger.opprettBehandling(it)
                    hendelser.behandlingOpprettet(it.toBehandlingOpprettet())
                    logger.info("Opprettet revurdering ${it.id} i sak ${it.sakId}")
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

    fun serialiserbarUtgave() = lagretBehandling.copy()
}
package no.nav.etterlatte.behandling.regulering

import no.nav.etterlatte.behandling.BehandlingDao
import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.behandling.domain.OpprettBehandling
import no.nav.etterlatte.behandling.domain.Regulering
import no.nav.etterlatte.behandling.domain.toBehandlingOpprettet
import no.nav.etterlatte.behandling.foerstegangsbehandling.FoerstegangsbehandlingAggregat
import no.nav.etterlatte.behandling.hendelse.HendelseDao
import no.nav.etterlatte.behandling.hendelse.HendelseType
import no.nav.etterlatte.behandling.hendelse.registrerVedtakHendelseFelles
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import no.nav.etterlatte.libs.common.behandling.tilVirkningstidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.util.*

class ReguleringAggregat(
    id: UUID,
    behandlinger: BehandlingDao,
    private val hendelser: HendelseDao
) {
    companion object {
        private val logger = LoggerFactory.getLogger(FoerstegangsbehandlingAggregat::class.java)

        fun opprettRegulering(
            sak: Long,
            forrigeBehandling: Behandling,
            fradato: LocalDate,
            behandlinger: BehandlingDao,
            hendelser: HendelseDao
        ): ReguleringAggregat {
            logger.info("Oppretter en behandling på $sak")
            return OpprettBehandling(
                type = BehandlingType.OMREGNING,
                sakId = sak,
                status = BehandlingStatus.OPPRETTET,
                persongalleri = forrigeBehandling.persongalleri,
                kommerBarnetTilgode = forrigeBehandling.kommerBarnetTilgode,
                vilkaarUtfall = forrigeBehandling.vilkaarUtfall,
                virkningstidspunkt = fradato.tilVirkningstidspunkt("Regulering"),
                revurderingsAarsak = RevurderingAarsak.GRUNNBELOEPREGULERING
            )
                .also {
                    behandlinger.opprettBehandling(it)
                    hendelser.behandlingOpprettet(it.toBehandlingOpprettet())
                    logger.info("Opprettet regulering ${it.id} i sak ${it.sakId}")
                }
                .let { ReguleringAggregat(it.id, behandlinger, hendelser) }
        }
    }

    var lagretBehandling: Regulering =
        requireNotNull(behandlinger.hentBehandling(id, BehandlingType.OMREGNING) as Regulering)

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
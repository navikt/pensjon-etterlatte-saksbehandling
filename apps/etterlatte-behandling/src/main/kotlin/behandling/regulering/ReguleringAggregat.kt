package no.nav.etterlatte.behandling.regulering

import no.nav.etterlatte.behandling.BehandlingDao
import no.nav.etterlatte.behandling.domain.Regulering
import no.nav.etterlatte.behandling.foerstegangsbehandling.FoerstegangsbehandlingAggregat
import no.nav.etterlatte.behandling.hendelse.HendelseDao
import no.nav.etterlatte.behandling.hendelse.HendelseType
import no.nav.etterlatte.behandling.hendelse.registrerVedtakHendelseFelles
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
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
            persongalleri: Persongalleri,
            behandlinger: BehandlingDao,
            hendelser: HendelseDao
        ): ReguleringAggregat {
            logger.info("Oppretter en behandling på $sak")
            return Regulering(
                id = UUID.randomUUID(),
                sak = sak,
                behandlingOpprettet = LocalDateTime.now(),
                sistEndret = LocalDateTime.now(),
                status = BehandlingStatus.OPPRETTET,
                persongalleri = persongalleri,
                kommerBarnetTilgode = null,
                vilkaarUtfall = null,
                virkningstidspunkt = null
            )
                .also {
                    behandlinger.opprettRegulering(it)
                    hendelser.behandlingOpprettet(it)
                    logger.info("Opprettet regulering ${it.id} i sak ${it.sak}")
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
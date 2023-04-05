package no.nav.etterlatte.behandling.foerstegangsbehandling

import no.nav.etterlatte.behandling.BehandlingDao
import no.nav.etterlatte.behandling.domain.Foerstegangsbehandling
import no.nav.etterlatte.behandling.domain.OpprettBehandling
import no.nav.etterlatte.behandling.domain.toBehandlingOpprettet
import no.nav.etterlatte.behandling.hendelse.HendelseDao
import no.nav.etterlatte.behandling.hendelse.HendelseType
import no.nav.etterlatte.behandling.hendelse.registrerVedtakHendelseFelles
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.KommerBarnetTilgode
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsResultat
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.UUID

class FoerstegangsbehandlingAggregat(
    id: UUID,
    private val behandlinger: BehandlingDao,
    private val hendelser: HendelseDao
) {
    companion object {
        private val logger = LoggerFactory.getLogger(FoerstegangsbehandlingAggregat::class.java)

        fun opprettFoerstegangsbehandling(
            sak: Long,
            mottattDato: String,
            persongalleri: Persongalleri,
            behandlinger: BehandlingDao,
            hendelser: HendelseDao
        ): FoerstegangsbehandlingAggregat {
            logger.info("Oppretter en behandling på $sak")
            return OpprettBehandling(
                type = BehandlingType.FØRSTEGANGSBEHANDLING,
                sakId = sak,
                status = BehandlingStatus.OPPRETTET,
                soeknadMottattDato = LocalDateTime.parse(mottattDato),
                persongalleri = persongalleri
            )
                .also {
                    behandlinger.opprettBehandling(it)
                    hendelser.behandlingOpprettet(it.toBehandlingOpprettet())
                    logger.info("Opprettet behandling ${it.id} i sak ${it.sakId}")
                }
                .let { FoerstegangsbehandlingAggregat(it.id, behandlinger, hendelser) }
        }
    }

    var lagretBehandling = requireNotNull(behandlinger.hentBehandling(id) as Foerstegangsbehandling)

    fun lagreGyldighetproeving(gyldighetsproeving: GyldighetsResultat) {
        lagretBehandling = lagretBehandling.oppdaterGyldighetsproeving(gyldighetsproeving)
            .also {
                behandlinger.lagreGyldighetsproving(it)
                logger.info("behandling ${it.id} i sak ${it.sak} er gyldighetsprøvd")
            }
            .also { behandlinger.lagreStatus(it) }
    }

    fun lagreKommerBarnetTilgode(kommerBarnetTilgode: KommerBarnetTilgode) {
        lagretBehandling = lagretBehandling.oppdaterKommerBarnetTilgode(kommerBarnetTilgode)
            .also { behandlinger.lagreKommerBarnetTilgode(it.id, kommerBarnetTilgode) }
            .also { behandlinger.lagreStatus(it) }
    }

    fun serialiserbarUtgave() = lagretBehandling.copy()

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
}
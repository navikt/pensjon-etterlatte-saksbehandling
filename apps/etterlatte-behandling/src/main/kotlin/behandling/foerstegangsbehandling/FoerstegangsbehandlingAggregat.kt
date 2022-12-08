package no.nav.etterlatte.behandling.foerstegangsbehandling

import no.nav.etterlatte.behandling.BehandlingDao
import no.nav.etterlatte.behandling.Foerstegangsbehandling
import no.nav.etterlatte.behandling.hendelse.HendelseDao
import no.nav.etterlatte.behandling.hendelse.HendelseType
import no.nav.etterlatte.behandling.hendelse.registrerVedtakHendelseFelles
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.KommerBarnetTilgode
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsResultat
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.*

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
            return Foerstegangsbehandling(
                id = UUID.randomUUID(),
                sak = sak,
                behandlingOpprettet = LocalDateTime.now(),
                sistEndret = LocalDateTime.now(),
                status = BehandlingStatus.OPPRETTET,
                soeknadMottattDato = LocalDateTime.parse(mottattDato),
                persongalleri = persongalleri,
                gyldighetsproeving = null,
                virkningstidspunkt = null,
                kommerBarnetTilgode = null
            )
                .also {
                    behandlinger.opprettFoerstegangsbehandling(it)
                    hendelser.behandlingOpprettet(it)
                    logger.info("Opprettet behandling ${it.id} i sak ${it.sak}")
                }
                .let { FoerstegangsbehandlingAggregat(it.id, behandlinger, hendelser) }
        }
    }

    var lagretBehandling: Foerstegangsbehandling =
        requireNotNull(behandlinger.hentBehandling(id, BehandlingType.FØRSTEGANGSBEHANDLING) as Foerstegangsbehandling)

    fun lagreGyldighetprøving(gyldighetsproeving: GyldighetsResultat) {
        lagretBehandling = lagretBehandling.oppdaterGyldighetsproeving(gyldighetsproeving)
            .also {
                behandlinger.lagreGyldighetsproving(it)
                logger.info("behandling ${it.id} i sak ${it.sak} er gyldighetsprøvd")
            }
    }

    fun lagreVirkningstidspunkt(yearMonth: YearMonth, ident: String): Virkningstidspunkt {
        lagretBehandling =
            lagretBehandling.oppdaterVirkningstidspunkt(yearMonth, Grunnlagsopplysning.Saksbehandler.create(ident))
                .also { behandlinger.lagreNyttVirkningstidspunkt(it.id, it.virkningstidspunkt!!) }

        return lagretBehandling.virkningstidspunkt!!
    }

    fun lagreKommerBarnetTilgode(kommerBarnetTilgode: KommerBarnetTilgode) {
        lagretBehandling = lagretBehandling.oppdaterKommerBarnetTilgode(kommerBarnetTilgode)
            .also { behandlinger.lagreKommerBarnetTilgode(it.id, kommerBarnetTilgode) }
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
        ) as Foerstegangsbehandling
    }
}
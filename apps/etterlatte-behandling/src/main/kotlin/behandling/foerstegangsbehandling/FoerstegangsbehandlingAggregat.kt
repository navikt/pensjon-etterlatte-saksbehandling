package no.nav.etterlatte.behandling.foerstegangsbehandling

import no.nav.etterlatte.behandling.Behandling
import no.nav.etterlatte.behandling.BehandlingDao
import no.nav.etterlatte.behandling.Foerstegangsbehandling
import no.nav.etterlatte.behandling.HendelseDao
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.OppgaveStatus
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsResultat
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.vikaar.VurderingsResultat
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*

class AvbruttBehandlingException(message: String) : RuntimeException(message) {}

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
            logger.info("Oppretter en behandling på ${sak}")
            return Foerstegangsbehandling(
                id = UUID.randomUUID(),
                sak = sak,
                behandlingOpprettet = LocalDateTime.now(),
                sistEndret = LocalDateTime.now(),
                status = BehandlingStatus.OPPRETTET,
                type = BehandlingType.FØRSTEGANGSBEHANDLING,
                soeknadMottattDato = LocalDateTime.parse(mottattDato),
                persongalleri = persongalleri,
                gyldighetsproeving = null,
                oppgaveStatus = OppgaveStatus.NY
            )
                .also {
                    behandlinger.opprettFoerstegangsbehandling(it)
                    hendelser.behandlingOpprettet(it)
                    logger.info("Opprettet behandling ${it.id} i sak ${it.sak}")
                }
                .let { FoerstegangsbehandlingAggregat(it.id, behandlinger, hendelser) }
        }
    }

    private object TilgangDao {
        fun sjekkOmBehandlingTillatesEndret(behandling: Behandling): Boolean {
            return behandling.status in listOf(
                BehandlingStatus.OPPRETTET,
                BehandlingStatus.GYLDIG_SOEKNAD,
                BehandlingStatus.IKKE_GYLDIG_SOEKNAD,
                BehandlingStatus.UNDER_BEHANDLING,
                BehandlingStatus.RETURNERT,
            )
        }
    }

    var lagretBehandling: Foerstegangsbehandling =
        requireNotNull(behandlinger.hentBehandling(id, BehandlingType.FØRSTEGANGSBEHANDLING) as Foerstegangsbehandling)

    fun lagreGyldighetprøving(gyldighetsproeving: GyldighetsResultat) {
        if (!TilgangDao.sjekkOmBehandlingTillatesEndret(lagretBehandling)) {
            throw AvbruttBehandlingException(
                "Det tillates ikke å gyldighetsprøve Behandling med id ${lagretBehandling.id} og status: ${lagretBehandling.status}"
            )
        }
        val status =
            if (gyldighetsproeving.resultat == VurderingsResultat.OPPFYLT) BehandlingStatus.GYLDIG_SOEKNAD else BehandlingStatus.IKKE_GYLDIG_SOEKNAD

        lagretBehandling = lagretBehandling.copy(
            gyldighetsproeving = gyldighetsproeving,
            status = status,
            sistEndret = LocalDateTime.now(),
            oppgaveStatus = OppgaveStatus.NY
        )
        behandlinger.lagreGyldighetsproving(lagretBehandling)
        logger.info("behandling ${lagretBehandling.id} i sak ${lagretBehandling.sak} er gyldighetsprøvd")
    }

    fun avbrytBehandling() {
        lagretBehandling = lagretBehandling.copy(
            status = BehandlingStatus.AVBRUTT,
            sistEndret = LocalDateTime.now(),
            oppgaveStatus = OppgaveStatus.LUKKET
        )

        behandlinger.lagreStatus(lagretBehandling)
        behandlinger.lagreOppgaveStatus(lagretBehandling)
    }

    fun serialiserbarUtgave() = lagretBehandling.copy()

    fun registrerVedtakHendelse(
        vedtakId: Long,
        hendelse: String,
        inntruffet: Tidspunkt,
        saksbehandler: String?,
        kommentar: String?,
        begrunnelse: String?
    ) {
        val ikkeSettUnderBehandling = lagretBehandling.status == BehandlingStatus.FATTET_VEDTAK
                || lagretBehandling.status == BehandlingStatus.RETURNERT
                || lagretBehandling.status == BehandlingStatus.ATTESTERT

        if (hendelse in listOf("FATTET", "ATTESTERT", "UNDERKJENT")) {
            requireNotNull(saksbehandler)
        }

        if (hendelse == "UNDERKJENT") {
            requireNotNull(kommentar)
            requireNotNull(begrunnelse)
        }

        lagretBehandling = lagretBehandling.copy(
            status = when (hendelse) {
                "FATTET" -> BehandlingStatus.FATTET_VEDTAK
                "ATTESTERT" -> BehandlingStatus.ATTESTERT
                "UNDERKJENT" -> BehandlingStatus.RETURNERT
                "VILKAARSVURDERT" -> if (ikkeSettUnderBehandling) lagretBehandling.status else BehandlingStatus.UNDER_BEHANDLING
                "BEREGNET" -> if (ikkeSettUnderBehandling) lagretBehandling.status else BehandlingStatus.UNDER_BEHANDLING
                "AVKORTET" -> if (ikkeSettUnderBehandling) lagretBehandling.status else BehandlingStatus.UNDER_BEHANDLING
                else -> throw IllegalStateException("Behandling ${lagretBehandling.id} forstår ikke vedtakhendelse $hendelse")
            },
            oppgaveStatus = when (hendelse) {
                "FATTET" -> OppgaveStatus.TIL_ATTESTERING
                "UNDERKJENT" -> OppgaveStatus.RETURNERT
                "ATTESTERT" -> OppgaveStatus.LUKKET
                else -> lagretBehandling.oppgaveStatus
            },
            sistEndret = LocalDateTime.now()
        )
        behandlinger.lagreStatus(lagretBehandling)
        behandlinger.lagreOppgaveStatus(lagretBehandling)
        hendelser.vedtakHendelse(
            lagretBehandling,
            vedtakId,
            hendelse,
            inntruffet,
            saksbehandler,
            kommentar,
            begrunnelse
        )
    }

}
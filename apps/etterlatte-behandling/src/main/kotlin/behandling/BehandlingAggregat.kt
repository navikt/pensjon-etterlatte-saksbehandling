package no.nav.etterlatte.behandling

import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.OppgaveStatus
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsResultat
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.vikaar.VurderingsResultat
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*

class AvbruttBehandlingException(message: String) : RuntimeException(message) {}

class BehandlingAggregat(
    id: UUID,
    private val behandlinger: BehandlingDao,
    private val hendelser: HendelseDao
) {
    companion object {
        private val logger = LoggerFactory.getLogger(BehandlingAggregat::class.java)

        fun opprett(
            sak: Long,
            behandlinger: BehandlingDao,
            hendelser: HendelseDao
        ): BehandlingAggregat {
            logger.info("Oppretter en behandling på ${sak}")
            return Behandling(
                UUID.randomUUID(),
                sak,
                LocalDateTime.now(),
                LocalDateTime.now(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                BehandlingStatus.OPPRETTET,
                OppgaveStatus.NY,
            )
                .also {
                    behandlinger.opprett(it)
                    hendelser.behandlingOpprettet(it)
                    logger.info("Opprettet behandling ${it.id} i sak ${it.sak}")
                }
                .let { BehandlingAggregat(it.id, behandlinger, hendelser) }
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

    var lagretBehandling = requireNotNull(behandlinger.hentBehandling(id))

    fun leggTilPersongalleriOgDato(persongalleri: Persongalleri, mottattDato: String) {
        if (!TilgangDao.sjekkOmBehandlingTillatesEndret(lagretBehandling)) {
            throw AvbruttBehandlingException(
                "Det tillates ikke å legge til opplysninger på behandling med id ${lagretBehandling.id} og status: ${lagretBehandling.status}"
            )
        }

        lagretBehandling = lagretBehandling.copy(
            sistEndret = LocalDateTime.now(),
            soeknadMottattDato = LocalDateTime.parse(mottattDato),
            innsender = persongalleri.innsender,
            soeker = persongalleri.soeker,
            gjenlevende = persongalleri.gjenlevende,
            avdoed = persongalleri.avdoed,
            soesken = persongalleri.soesken,
        )

        behandlinger.lagrePersongalleriOgMottattdato(lagretBehandling)
        logger.info("Persongalleri er lagret i behandling ${lagretBehandling.id} i sak ${lagretBehandling.sak}")

    }

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
            oppgaveStatus = OppgaveStatus.NY
        )
        behandlinger.lagreGyldighetsproving(lagretBehandling)
        logger.info("behandling ${lagretBehandling.id} i sak ${lagretBehandling.sak} er gyldighetsprøvd")
    }

    fun avbrytBehandling() {
        lagretBehandling = lagretBehandling.copy(
            status = BehandlingStatus.AVBRUTT,
            oppgaveStatus = OppgaveStatus.LUKKET
        )

        behandlinger.lagreStatus(lagretBehandling)
        behandlinger.lagreOppgaveStatus(lagretBehandling)
    }

    fun serialiserbarUtgave() = lagretBehandling.copy()

    fun registrerVedtakHendelse(vedtakId: Long, hendelse: String, inntruffet: Tidspunkt, saksbehandler: String?, kommentar: String?, begrunnelse: String?) {
        val ikkeSettUnderBehandling = lagretBehandling.status == BehandlingStatus.FATTET_VEDTAK
                || lagretBehandling.status == BehandlingStatus.RETURNERT
                || lagretBehandling.status == BehandlingStatus.ATTESTERT

        if(hendelse in listOf("FATTET", "ATTESTERT","UNDERKJENT") ) {
            requireNotNull(saksbehandler)
        }

        if(hendelse == "UNDERKJENT") {
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
            }
        )
        behandlinger.lagreStatus(lagretBehandling)
        behandlinger.lagreOppgaveStatus(lagretBehandling)
        hendelser.vedtakHendelse(lagretBehandling, vedtakId, hendelse, inntruffet, saksbehandler, kommentar, begrunnelse)
    }

}
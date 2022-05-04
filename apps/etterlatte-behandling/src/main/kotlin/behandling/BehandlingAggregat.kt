package no.nav.etterlatte.behandling

import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsResultat
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class AvbruttBehandlingException(message: String) : RuntimeException(message) {}

class BehandlingAggregat(
    id: UUID,
    private val behandlinger: BehandlingDao
) {
    companion object {
        private val logger = LoggerFactory.getLogger(BehandlingAggregat::class.java)

        fun opprett(
            sak: Long,
            behandlinger: BehandlingDao
        ): BehandlingAggregat {
            logger.info("Oppretter en behandling på ${sak}")
            return Behandling(
                UUID.randomUUID(),
                sak,
                LocalDateTime.now(),
                LocalDateTime.now(),
                LocalDate.now(), //må hentes fra søknad
                "", //må hentes fra søknad
                "", //må hentes fra søknad
                emptyList(), //må hentes fra søknad
                emptyList(), //må hentes fra søknad
                null,
                null,
                BehandlingStatus.GYLDIG_SOEKNAD // må hentes fra gyldighetsprøving
            )
                .also {
                    behandlinger.opprett(it)
                    logger.info("Opprettet behandling ${it.id} i sak ${it.sak}")
                }
                .let { BehandlingAggregat(it.id, behandlinger) }
        }
    }

    private object TilgangDao {
        fun sjekkOmBehandlingTillatesEndret(behandling: Behandling): Boolean {
            return behandling.status in listOf(
                BehandlingStatus.GYLDIG_SOEKNAD,
                BehandlingStatus.UNDER_BEHANDLING,
                BehandlingStatus.IKKE_GYLDIG_SOEKNAD,
            )
        }
    }

    var lagretBehandling = requireNotNull(behandlinger.hentBehandling(id))

    fun lagreGyldighetprøving(gyldighetsproeving: GyldighetsResultat) {
        if (!TilgangDao.sjekkOmBehandlingTillatesEndret(lagretBehandling)) {
            throw AvbruttBehandlingException(
                "Det tillates ikke å gyldighetsprøve Behandling med id ${lagretBehandling.id} og status: ${lagretBehandling.status}"
            )
        }
        lagretBehandling = lagretBehandling.copy(
            gyldighetsproeving = gyldighetsproeving
        )
        behandlinger.lagreGyldighetsproving(lagretBehandling)
        logger.info("behandling ${lagretBehandling.id} i sak ${lagretBehandling.sak} er gyldighetsprøvd")
    }

    fun avbrytBehandling(): Behandling {
        behandlinger.avbrytBehandling(lagretBehandling)
            .also { lagretBehandling = lagretBehandling.copy(status = BehandlingStatus.AVBRUTT) }.let {
                return lagretBehandling
            }
    }


    fun serialiserbarUtgave() = lagretBehandling.copy()
}
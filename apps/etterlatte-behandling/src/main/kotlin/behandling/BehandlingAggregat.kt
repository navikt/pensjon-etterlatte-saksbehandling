package no.nav.etterlatte.behandling

import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsResultat
import no.nav.etterlatte.libs.common.vikaar.VurderingsResultat
import org.slf4j.LoggerFactory
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
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                BehandlingStatus.OPPRETTET
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
                BehandlingStatus.OPPRETTET,
                BehandlingStatus.GYLDIG_SOEKNAD,
                BehandlingStatus.IKKE_GYLDIG_SOEKNAD,
                BehandlingStatus.UNDER_BEHANDLING,
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
            status = status
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
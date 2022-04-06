package no.nav.etterlatte.attestering

import no.nav.etterlatte.domain.Attestasjon
import no.nav.etterlatte.domain.AttestasjonsStatus
import no.nav.etterlatte.domain.AttestertVedtak
import org.slf4j.LoggerFactory

class KanIkkeEndreAttestertVedtakException(message: String) : RuntimeException(message)
class AttestertVedtakEksistererIkke(message: String) : RuntimeException(message)
class AttestertVedtakEksistererAllerede(message: String) : RuntimeException(message)

class AttestasjonService(
    val attestasjonDao: AttestasjonDao,
) {

    private object TilgangDao {
        fun sjekkOmAttestasjonTillatesEndret(attestertVedtak: AttestertVedtak): Boolean {
            return attestertVedtak.status in listOf(
                AttestasjonsStatus.TIL_ATTESTERING,
                AttestasjonsStatus.IKKE_ATTESTERT
            )
        }
    }

    fun hentAttestertVedtak(vedtakId: String): AttestertVedtak? =
        attestasjonDao.hentAttestertVedtak(vedtakId)

    fun opprettAttestertVedtak(vedtak: Vedtak, attestasjon: Attestasjon): AttestertVedtak {
        if (hentAttestertVedtak(vedtak.vedtakId) != null) {
            throw AttestertVedtakEksistererAllerede("Attestert vedtak med vedtakId ${vedtak.vedtakId} eksisterer allerede")
        }
        val opprettetAttestertVedtak = attestasjonDao.opprettAttestertVedtak(vedtak, attestasjon)
        logger.info("Vedtak med id ${opprettetAttestertVedtak.vedtakId} attestert av ${opprettetAttestertVedtak.attestantId} og lagret i databasen ${opprettetAttestertVedtak.tidspunkt}")
        return opprettetAttestertVedtak
    }

    fun opprettVedtakUtenAttestering(vedtak: Vedtak): AttestertVedtak {
        if (hentAttestertVedtak(vedtak.vedtakId) != null) {
            throw AttestertVedtakEksistererAllerede("Attestert vedtak med vedtakId ${vedtak.vedtakId} eksisterer allerede")
        }
        return attestasjonDao.opprettMottattVedtak(vedtak)

    }

    fun settAttestertVedtakTilIkkeAttestert(vedtakId: String): AttestertVedtak {
        val attestertVedtak = hentAttestertVedtak(vedtakId)
            ?: throw AttestertVedtakEksistererIkke("Attestert vedtak med vedtakId $vedtakId eksisterer ikke")
        if (!TilgangDao.sjekkOmAttestasjonTillatesEndret(attestertVedtak)) {
            throw KanIkkeEndreAttestertVedtakException("Kan ikke sette vedtak $vedtakId med status ${attestertVedtak.status} til status: IKKE_ATTESTERT")
        }
        return attestasjonDao.settAttestertVedtakTilIkkeAttestert(vedtakId)

    }

    fun attesterVedtak(vedtakId: String, attestasjon: Attestasjon): AttestertVedtak {
        val attestertVedtak = hentAttestertVedtak(vedtakId)
            ?: throw AttestertVedtakEksistererIkke("Attestert vedtak med vedtakId $vedtakId eksisterer ikke")
        if (!TilgangDao.sjekkOmAttestasjonTillatesEndret(attestertVedtak)) {
            throw KanIkkeEndreAttestertVedtakException("Kan ikke attestere vedtak $vedtakId med status ${attestertVedtak.status}")
        }
        return attestasjonDao.attesterVedtak(vedtakId, attestasjon)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AttestasjonService::class.java)
    }
}

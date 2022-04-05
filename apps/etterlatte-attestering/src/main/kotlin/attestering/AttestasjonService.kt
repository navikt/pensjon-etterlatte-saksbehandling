package no.nav.etterlatte.attestering

import no.nav.etterlatte.domain.Attestasjon
import no.nav.etterlatte.domain.AttestasjonsStatus
import no.nav.etterlatte.domain.AttestertVedtak

class KanIkkeEndreAttestertVedtakException(message: String) : RuntimeException(message)
class AttestertVedtakEksistererIkke(message: String) : RuntimeException(message)
class AttestertVedtakEksistererAllerede(message: String) : RuntimeException(message)

class AttestasjonService(
    val attestasjonDao: AttestasjonDao,
) {

    private object TilgangDao {
        fun sjekkOmBehandlingTillatesEndret(attestertVedtak: AttestertVedtak): Boolean {
            return attestertVedtak.attestasjonsstatus in listOf(
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
        return attestasjonDao.opprettAttestertVedtak(vedtak, attestasjon)

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
        if (!TilgangDao.sjekkOmBehandlingTillatesEndret(attestertVedtak)) {
            throw KanIkkeEndreAttestertVedtakException("Kan ikke sette vedtak $vedtakId med attestasjonsstatus ${attestertVedtak.attestasjonsstatus} til attestasjonsstatus: IKKE_ATTESTERT")
        }
        return attestasjonDao.settAttestertVedtakTilIkkeAttestert(vedtakId)

    }

    fun attesterVedtak(vedtakId: String, attestasjon: Attestasjon): AttestertVedtak {
        val attestertVedtak = hentAttestertVedtak(vedtakId)
            ?: throw AttestertVedtakEksistererIkke("Attestert vedtak med vedtakId $vedtakId eksisterer ikke")
        if (!TilgangDao.sjekkOmBehandlingTillatesEndret(attestertVedtak)) {
            throw KanIkkeEndreAttestertVedtakException("Kan ikke attestere vedtak $vedtakId med attestasjonsstatus ${attestertVedtak.attestasjonsstatus}")
        }
        return attestasjonDao.attesterVedtak(vedtakId, attestasjon)

    }
}
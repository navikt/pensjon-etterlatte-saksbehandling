package no.nav.etterlatte.vedtak

import model.Avdoed
import model.Barn
import model.Vedtak
import model.VedtakType
import java.time.LocalDate

class VedtakService {
    fun hentVedtak(vedtakId: Long) = Vedtak(
        vedtakId = vedtakId,
        type = VedtakType.INNVILGELSE,
        status = "Fattet",
        dato = LocalDate.now(),
        gjelderFom = LocalDate.now(),
        sum = 2500.00,
        kontonummer = "1235.123.1234",
        saksnummer = "123456",
        virkningsdato = LocalDate.now(),
        barn = Barn(
            navn = "Ola nordmann",
            fnr = "01010120200"
        ),
        avdoed = Avdoed(
            navn = "Avdød nordmann",
            doedsdato = LocalDate.now()
        ),
        vilkaar = listOf("§12.2", "§6.2")
    )
}

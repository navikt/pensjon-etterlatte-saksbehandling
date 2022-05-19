package no.nav.etterlatte.vedtak

import no.nav.etterlatte.libs.common.brev.model.Avdoed
import no.nav.etterlatte.libs.common.brev.model.Barn
import no.nav.etterlatte.libs.common.brev.model.Vedtak
import no.nav.etterlatte.libs.common.brev.model.VedtakType
import java.time.LocalDate

class VedtakService {
    fun hentVedtak(vedtakId: String) = Vedtak(
        vedtakId = vedtakId,
//        type = if (vedtakId.toInt() % 2 == 0) VedtakType.INNVILGELSE else VedtakType.AVSLAG,
        type = VedtakType.INNVILGELSE,
        status = "Fattet",
        dato = LocalDate.now(),
        gjelderFom = LocalDate.now(),
        sum = 2500.00,
        kontonummer = "1235.123.1234",
        saksnummer = "123456",
        virkningsdato = LocalDate.now(),
        barn = Barn(
            navn = "Talendfull Blyant",
            fnr = "12101376212"
        ),
        avdoed = Avdoed(
            navn = "Avdød nordmann",
            doedsdato = LocalDate.now()
        ),
        vilkaar = listOf("§12.2", "§6.2")
    )
}

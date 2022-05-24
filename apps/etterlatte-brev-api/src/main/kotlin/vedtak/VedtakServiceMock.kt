package vedtak

import no.nav.etterlatte.domene.vedtak.*
import no.nav.etterlatte.libs.common.vikaar.VilkaarResultat
import no.nav.etterlatte.libs.common.vikaar.VurderingsResultat
import no.nav.etterlatte.vedtak.VedtakService
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZonedDateTime
import java.util.*

class VedtakServiceMock : VedtakService {
    override fun hentVedtak(vedtakId: Long) = Vedtak(
        vedtakId = vedtakId,
        virk = Periode(YearMonth.of(2022, 1), null),
        sak = Sak("11057523044", "barnepensjon", 100L),
        behandling = Behandling(BehandlingType.FORSTEGANGSBEHANDLING, id = UUID.randomUUID()),
        type = VedtakType.INNVILGELSE,
        grunnlag = emptyList(),
        vilkaarsvurdering = VilkaarResultat(
            resultat = VurderingsResultat.OPPFYLT,
            vilkaar = null,
            vurdertDato = LocalDateTime.now()
        ),
        beregning = null,
        avkorting = null,
        pensjonTilUtbetaling = listOf(
            Utbetalingsperiode(
                20L,
                Periode(YearMonth.of(2022, 1), null),
                BigDecimal.valueOf(2300.00),
                type = UtbetalingsperiodeType.UTBETALING
            )
        ),
        vedtakFattet = VedtakFattet("z12345", ansvarligEnhet = "Porsgrunn", tidspunkt = ZonedDateTime.now()),
        attestasjon = Attestasjon("z54321", attesterendeEnhet = "Porsgrunn", tidspunkt = ZonedDateTime.now())
    )
}

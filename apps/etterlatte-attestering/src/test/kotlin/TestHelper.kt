import no.nav.etterlatte.libs.common.vedtak.Attestasjon
import no.nav.etterlatte.libs.common.vedtak.Beregningsperiode
import no.nav.etterlatte.libs.common.vedtak.Endringskode
import no.nav.etterlatte.libs.common.vedtak.Enhetstype
import no.nav.etterlatte.libs.common.vedtak.Oppdragsenhet
import no.nav.etterlatte.libs.common.vedtak.Vedtak
import java.io.FileNotFoundException
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

object TestHelper

fun readFile(file: String) = TestHelper::class.java.getResource(file)?.readText()
    ?: throw FileNotFoundException("Fant ikke filen $file")

fun vedtak(vedtakId: String = "1") = Vedtak(
    vedtakId = vedtakId,
    behandlingsId = "11",
    sakId = "111",
    saksbehandlerId = "4321",
    sakIdGjelderFnr = "12345612345",
    aktorFoedselsdato = LocalDate.parse("2010-07-04"),
    behandlingstype = Endringskode.NY,
    beregningsperioder = listOf(
        Beregningsperiode(
            delytelsesId = "delytelsesid",
            ytelseskomponent = "PENBPGP-OPTP",
            datoFOM = LocalDate.parse("2022-02-02"),
            datoTOM = LocalDate.parse("2030-01-04"),
            belop = BigDecimal(10000)
        )
    ),
    oppdragsenheter = listOf(
        Oppdragsenhet(
            enhetsType = Enhetstype.BOSTED,
            enhetsnummer = "9999",
            datoEnhetFOM = LocalDate.parse("1999-09-28")
        )
    ),
    Attestasjon(UUID.randomUUID().toString())
)

fun attestasjon() = Attestasjon(
    attestantId = "Z123456"
)

import no.nav.etterlatte.vedtaksoversetter.Beregningsperiode
import no.nav.etterlatte.vedtaksoversetter.Endringskode
import no.nav.etterlatte.vedtaksoversetter.Enhetstype
import no.nav.etterlatte.vedtaksoversetter.Oppdragsenhet
import no.nav.etterlatte.vedtaksoversetter.Vedtak
import no.nav.etterlatte.vedtaksoversetter.Ytelseskomponent
import java.io.FileNotFoundException
import java.math.BigDecimal
import java.time.LocalDate

object TestHelper

fun readFile(file: String) = TestHelper::class.java.getResource(file)?.readText()
    ?: throw FileNotFoundException("Fant ikke filen $file")

fun dummyVedtak() = Vedtak(
    sakId = "1234",
    saksbehandlerId = "4321",
    beregningsperioder = listOf(
        Beregningsperiode(
            behandlingsId = "1234",
            endringskode = Endringskode.NY,
            delytelsesId = "delytelsesid",
            ytelseskomponent = Ytelseskomponent.BARNEPENSJON,
            datoFOM = LocalDate.parse("2022-02-02"),
            datoTOM = LocalDate.parse("2030-01-04"),
            belop = BigDecimal(10000)
        )
    ),
    sakIdGjelderFnr = "12345612345",
    aktorFoedselsdato = LocalDate.parse("2010-07-04"),
    oppdragsenheter = listOf(
        Oppdragsenhet(
            enhetsType = Enhetstype.BOSTED,
            enhetsnummer = "9999",
            datoEnhetFOM = LocalDate.parse("1999-09-28")
        )
    ),
    vedtakId = "8888"
)

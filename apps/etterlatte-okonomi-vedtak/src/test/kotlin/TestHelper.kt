package no.nav.etterlatte

import no.nav.etterlatte.domain.Beregningsperiode
import no.nav.etterlatte.domain.Endringskode
import no.nav.etterlatte.domain.Enhetstype
import no.nav.etterlatte.domain.Oppdragsenhet
import no.nav.etterlatte.domain.Vedtak
import no.nav.etterlatte.domain.Ytelseskomponent
import java.io.FileNotFoundException
import java.math.BigDecimal
import java.time.LocalDate

object TestHelper

fun readFile(file: String) = TestHelper::class.java.getResource(file)?.readText()
    ?: throw FileNotFoundException("Fant ikke filen $file")

fun mockVedtak() = Vedtak(
    sakId = "1234",
    vedtakId = "8888",
    behandlingsId = "1234",
    saksbehandlerId = "4321",
    sakIdGjelderFnr = "12345612345",
    aktorFoedselsdato = LocalDate.parse("2010-07-04"),
    beregningsperioder = listOf(
        Beregningsperiode(
            endringskode = Endringskode.NY,
            delytelsesId = "delytelsesid",
            ytelseskomponent = Ytelseskomponent.BARNEPENSJON,
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
    )
)

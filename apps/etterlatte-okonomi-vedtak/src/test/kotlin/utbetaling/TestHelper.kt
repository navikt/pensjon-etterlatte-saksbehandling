package no.nav.etterlatte.utbetaling

import no.nav.etterlatte.libs.common.vedtak.Attestasjon
import no.nav.etterlatte.libs.common.vedtak.Beregningsperiode
import no.nav.etterlatte.libs.common.vedtak.Endringskode
import no.nav.etterlatte.libs.common.vedtak.Enhetstype
import no.nav.etterlatte.libs.common.vedtak.Oppdragsenhet
import no.nav.etterlatte.libs.common.vedtak.Vedtak
import no.nav.etterlatte.utbetaling.iverksetting.oppdrag.OppdragMapper
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Utbetaling
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.UtbetalingStatus
import no.nav.etterlatte.utbetaling.common.Tidspunkt
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.BehandlingId
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Foedselsnummer
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.SakId
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.VedtakId
import no.trygdeetaten.skjema.oppdrag.Mmel
import java.io.FileNotFoundException
import java.math.BigDecimal
import java.time.LocalDate

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
    )
)

fun oppdrag(vedtakId: String = "8888") =
    OppdragMapper.oppdragFraVedtak(vedtak(vedtakId), attestasjon(), Tidspunkt.now())

fun oppdragMedGodkjentKvittering(vedtakId: String = "1") = oppdrag(vedtakId).apply {
    mmel = Mmel().apply {
        alvorlighetsgrad = "00"
    }
    oppdrag110 = this.oppdrag110.apply {
        oppdragsId = 1
    }
}

fun oppdragMedFeiletKvittering(vedtakId: String = "1") = oppdrag(vedtakId).apply {
    mmel = Mmel().apply {
        alvorlighetsgrad = "12"
        kodeMelding = "KodeMelding"
        beskrMelding = "Beskrivelse"
    }
    oppdrag110 = this.oppdrag110.apply {
        oppdragsId = 1
    }
}

fun attestasjon() = Attestasjon(
    attestantId = "Z123456"
)

fun utbetaling(
    id: Long = 1,
    status: UtbetalingStatus = UtbetalingStatus.GODKJENT,
    vedtakId: String = "1",
    avstemmingsnoekkel: Tidspunkt = Tidspunkt.now(),
    opprettet: Tidspunkt = Tidspunkt.now()
) =
    Utbetaling(
        id = id,
        vedtakId = VedtakId(vedtakId),
        behandlingId = BehandlingId("1"),
        sakId = SakId("1"),
        status = status,
        vedtak = vedtak(vedtakId),
        opprettet = opprettet,
        endret = Tidspunkt.now(),
        avstemmingsnoekkel = avstemmingsnoekkel,
        foedselsnummer = Foedselsnummer("12345678903"),
        oppdrag = oppdrag(vedtakId),
        kvittering = oppdrag(vedtakId),
        kvitteringBeskrivelse = "En beskrivelse",
        kvitteringFeilkode = "hva skal stå her?",
        kvitteringMeldingKode = "08"
    )
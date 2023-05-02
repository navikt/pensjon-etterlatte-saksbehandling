package no.nav.etterlatte.brev.behandling

import no.nav.etterlatte.brev.model.Spraak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.grunnlag.hentDoedsdato
import no.nav.etterlatte.libs.common.grunnlag.hentFoedselsnummer
import no.nav.etterlatte.libs.common.grunnlag.hentKonstantOpplysning
import no.nav.etterlatte.libs.common.grunnlag.hentNavn
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.InnsenderSoeknad
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Navn
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import java.time.LocalDate
import java.util.*

data class Behandling(
    val sakId: Long,
    val sakType: SakType,
    val behandlingId: UUID,
    val spraak: Spraak,
    val persongalleri: Persongalleri,
    val vedtak: ForenkletVedtak,
    val utbetalingsinfo: Utbetalingsinfo? = null
) {
    init {
        if (vedtak.type == VedtakType.INNVILGELSE) {
            requireNotNull(utbetalingsinfo) { "Utbetalingsinformasjon mangler på behandling (id=$behandlingId" }
        }
    }
}

data class ForenkletVedtak(
    val id: Long,
    val type: VedtakType,
    val saksbehandler: Saksbehandler,
    val attestant: Attestant?
)

data class Saksbehandler(
    val ident: String,
    val enhet: String
)

typealias Attestant = Saksbehandler

data class Utbetalingsinfo(
    val antallBarn: Int?,
    val beloep: Int,
    val virkningsdato: LocalDate,
    val soeskenjustering: Boolean,
    val beregningsperioder: List<Beregningsperiode>
)

data class Beregningsperiode(
    val datoFOM: LocalDate,
    val datoTOM: LocalDate?,
    val grunnbeloep: Int,
    val antallBarn: Int,
    val utbetaltBeloep: Int,
    val trygdetid: Int
)

data class Persongalleri(
    val innsender: Innsender,
    val soeker: Soeker,
    // TODO: Legge til gjenlevende, men det krever litt logikk siden gjenlevende er null på OMS (søker = gjenlevende)
    val avdoed: Avdoed
)

fun Grunnlag.mapSoeker(): Soeker = with(this.soeker) {
    Soeker(
        navn = hentNavn()!!.verdi.fulltNavn(),
        fnr = hentFoedselsnummer()!!.verdi.value
    )
}

fun Grunnlag.mapAvdoed(): Avdoed = with(this.familie) {
    val avdoed = hentAvdoed()

    Avdoed(
        navn = avdoed.hentNavn()!!.verdi.fulltNavn(),
        doedsdato = avdoed.hentDoedsdato()!!.verdi!!
    )
}

fun Grunnlag.mapInnsender(): Innsender = with(this.sak) {
    val opplysning = hentKonstantOpplysning<InnsenderSoeknad>(Opplysningstype.INNSENDER_SOEKNAD_V1)

    val innsender = requireNotNull(opplysning?.verdi) {
        "Sak (id=${metadata.sakId}) mangler opplysningstype INNSENDER_SOEKNAD_V1"
    }

    Innsender(
        navn = innsender.let { "${it.fornavn} ${it.etternavn}" },
        fnr = innsender.foedselsnummer.value
    )
}

fun Grunnlag.mapSpraak(): Spraak = with(this.sak) {
    val opplysning = hentKonstantOpplysning<Spraak>(Opplysningstype.SPRAAK)

    requireNotNull(opplysning?.verdi) {
        "Sak (id=${metadata.sakId}) mangler opplysningstype SPRAAK"
    }
}

fun List<Beregningsperiode>.hentUtbetaltBeloep(): Int {
    // TODO: Håndter grunnbeløpsendringer
    return this.last().utbetaltBeloep
}

private fun Navn.fulltNavn(): String = listOfNotNull(fornavn, mellomnavn, etternavn).joinToString(" ")
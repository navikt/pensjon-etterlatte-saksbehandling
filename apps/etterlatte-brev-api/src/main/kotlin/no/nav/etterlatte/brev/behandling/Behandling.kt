package no.nav.etterlatte.brev.behandling

import no.nav.etterlatte.brev.model.Spraak
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import no.nav.etterlatte.libs.common.behandling.RevurderingInfo
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.grunnlag.hentDoedsdato
import no.nav.etterlatte.libs.common.grunnlag.hentFoedselsnummer
import no.nav.etterlatte.libs.common.grunnlag.hentKonstantOpplysning
import no.nav.etterlatte.libs.common.grunnlag.hentNavn
import no.nav.etterlatte.libs.common.grunnlag.hentVergemaalellerfremtidsfullmakt
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.InnsenderSoeknad
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Navn
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.vedtak.VedtakStatus
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.pensjon.brevbaker.api.model.Foedselsnummer
import no.nav.pensjon.brevbaker.api.model.Kroner
import java.time.LocalDate
import java.time.YearMonth
import java.util.*

data class Behandling(
    val sakId: Long,
    val sakType: SakType,
    val behandlingId: UUID,
    val spraak: Spraak,
    val persongalleri: Persongalleri,
    val vedtak: ForenkletVedtak,
    val utbetalingsinfo: Utbetalingsinfo,
    val avkortingsinfo: Avkortingsinfo? = null,
    val revurderingsaarsak: RevurderingAarsak? = null,
    val revurderingInfo: RevurderingInfo? = null,
    val virkningsdato: YearMonth? = null,
    val innvilgelsesdato: LocalDate? = null,
    val adopsjonsdato: LocalDate? = null,
    val trygdetid: List<Trygdetidsperiode>? = null
) {
    init {
        if (vedtak.type == VedtakType.INNVILGELSE) {
            requireNotNull(utbetalingsinfo) { "Utbetalingsinformasjon mangler på behandling (id=$behandlingId" }
        }
    }
}

data class Trygdetidsperiode(
    val datoFOM: LocalDate,
    val datoTOM: LocalDate?,
    val land: String,
    val opptjeningsperiode: String
)

data class ForenkletVedtak(
    val id: Long,
    val status: VedtakStatus,
    val type: VedtakType,
    val ansvarligEnhet: String,
    val saksbehandlerIdent: String,
    val attestantIdent: String?
)

data class Utbetalingsinfo(
    val antallBarn: Int,
    val beloep: Kroner,
    val virkningsdato: LocalDate,
    val soeskenjustering: Boolean,
    val beregningsperioder: List<Beregningsperiode>
)

data class Avkortingsinfo(
    val grunnbeloep: Kroner,
    val inntekt: Kroner,
    val virkningsdato: LocalDate,
    val beregningsperioder: List<AvkortetBeregningsperiode>
)

data class AvkortetBeregningsperiode(
    val datoFOM: LocalDate,
    val datoTOM: LocalDate?,
    val inntekt: Kroner,
    val ytelseFoerAvkorting: Kroner,
    val trygdetid: Int,
    val utbetaltBeloep: Kroner
)

data class Beregningsperiode(
    val datoFOM: LocalDate,
    val datoTOM: LocalDate?,
    val grunnbeloep: Kroner,
    val antallBarn: Int,
    val utbetaltBeloep: Kroner,
    val trygdetid: Int
)

data class Persongalleri(
    val innsender: Innsender,
    val soeker: Soeker,
    val avdoed: Avdoed,
    val verge: Verge?
)

fun Grunnlag.mapSoeker(): Soeker = with(this.soeker) {
    val navn = hentNavn()!!.verdi

    Soeker(
        fornavn = navn.fornavn.storForbokstav(),
        mellomnavn = navn.mellomnavn?.storForbokstav(),
        etternavn = navn.etternavn.storForbokstav(),
        fnr = Foedselsnummer(hentFoedselsnummer()!!.verdi.value)
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
        navn = innsender.let { "${it.fornavn.storForbokstav()} ${it.etternavn.storForbokstav()}" },
        fnr = Foedselsnummer(innsender.foedselsnummer.value)
    )
}

fun Grunnlag.mapSpraak(): Spraak = with(this.sak) {
    val opplysning = hentKonstantOpplysning<Spraak>(Opplysningstype.SPRAAK)

    requireNotNull(opplysning?.verdi) {
        "Sak (id=${metadata.sakId}) mangler opplysningstype SPRAAK"
    }
}

fun Grunnlag.mapVerge(sakType: SakType): Verge? = with(this) {
    val opplysning = sak.hentVergemaalellerfremtidsfullmakt()

    if (opplysning?.verdi != null) {
        TODO("Støtter ikke annen verge enn forelder – håndtering av annen verge krever ytterligere avklaringer")
    } else if (sakType == SakType.BARNEPENSJON) {
        val gjenlevendeNavn = hentGjenlevende().hentNavn()!!.verdi.fulltNavn()

        Verge(gjenlevendeNavn)
    } else {
        null
    }
}

fun List<Beregningsperiode>.hentUtbetaltBeloep(): Int {
    // TODO: Håndter grunnbeløpsendringer
    return this.last().utbetaltBeloep.value
}

private fun Navn.fulltNavn(): String =
    listOfNotNull(fornavn, mellomnavn, etternavn).joinToString(" ") { it.storForbokstav() }

private fun String.storForbokstav() = this.lowercase().storForbokstavEtter("-").storForbokstavEtter(" ")

private fun String.storForbokstavEtter(delim: String) = this.split(delim).joinToString(delim) {
    it.replaceFirstChar { c -> c.uppercase() }
}
package no.nav.etterlatte.opplysninger.kilde.pdl

import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Navn
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper.AVDOEDESBARN
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper.AVDOED_PDL_V1
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper.BOSTEDSADRESSE
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper.DELTBOSTEDSADRESSE
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper.DOEDSDATO
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper.FAMILIERELASJON
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper.FOEDELAND
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper.FOEDSELSAAR
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper.FOEDSELSDATO
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper.FOEDSELSNUMMER
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper.GJENLEVENDE_FORELDER_PDL_V1
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper.KONTAKTADRESSE
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper.NAVN
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper.OPPHOLDSADRESSE
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper.PERSONROLLE
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper.SIVILSTATUS
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper.SOEKER_PDL_V1
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper.STATSBORGERSKAP
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper.UTLAND
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper.VERGEMAALELLERFREMTIDSFULLMAKT
import no.nav.etterlatte.libs.common.pdl.OpplysningDTO
import no.nav.etterlatte.libs.common.pdl.PersonDTO
import no.nav.etterlatte.libs.common.periode.Periode
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.person.PersonRolle
import java.time.Instant
import java.time.LocalDateTime
import java.time.YearMonth

class Opplysningsbolk(private val fnr: Foedselsnummer, private val innhentetTidspunkt: Instant) {
    private var opplysninger = mutableListOf<Grunnlagsopplysning<out Any>>()
    fun leggTilOpplysninger(
        opplysningstyper: Opplysningstyper,
        grunnlagsopplysning: List<OpplysningDTO<out Any>>?,
        periode: Periode? = null
    ) {
        grunnlagsopplysning?.forEach { leggTilOpplysning(opplysningstyper, it, periode) }
    }

    fun leggTilOpplysning(
        opplysningstyper: Opplysningstyper,
        opplysningDTO: OpplysningDTO<out Any>?,
        periode: Periode? = null
    ) {
        opplysningDTO?.let {
            opplysninger.add(
                lagPersonOpplysning(
                    tidspunktForInnhenting = innhentetTidspunkt,
                    opplysningsType = opplysningstyper,
                    opplysning = opplysningDTO,
                    fnr = fnr,
                    periode = periode
                )
            )
        }
    }

    fun hentOpplysninger() = opplysninger.toList()
}

fun lagOpplysninger(
    person: Person,
    personDTO: PersonDTO,
    opplysningsbehov: Opplysningstyper, // AVDOED_PDL_V1 || SOEKER_PDL_V1 || GJENLEVENDE_PDL_V1
    fnr: Foedselsnummer
): List<Grunnlagsopplysning<*>> {
    val tidspunktForInnhenting = Instant.now()
    val opplysningsbolk = Opplysningsbolk(fnr, tidspunktForInnhenting)
    val personRolle = behovNameTilPersonRolle(opplysningsbehov)

    val opplysningNavn =
        OpplysningDTO(Navn(personDTO.fornavn.verdi, personDTO.etternavn.verdi), personDTO.fornavn.opplysningsid)

    opplysningsbolk.apply {
        leggTilOpplysning(NAVN, opplysningNavn)
        leggTilOpplysninger(DELTBOSTEDSADRESSE, personDTO.deltBostedsadresse)
        leggTilOpplysninger(KONTAKTADRESSE, personDTO.kontaktadresse)
        leggTilOpplysninger(OPPHOLDSADRESSE, personDTO.oppholdsadresse)
        leggTilOpplysninger(VERGEMAALELLERFREMTIDSFULLMAKT, personDTO.vergemaalEllerFremtidsfullmakt)
        leggTilOpplysning(FOEDSELSDATO, personDTO.foedselsdato)
        leggTilOpplysning(FOEDELAND, personDTO.foedeland)
        leggTilOpplysning(DOEDSDATO, personDTO.doedsdato)
        leggTilOpplysning(SIVILSTATUS, personDTO.sivilstatus)
        leggTilOpplysning(STATSBORGERSKAP, personDTO.statsborgerskap)
        leggTilOpplysning(UTLAND, personDTO.utland)
        leggTilOpplysning(FAMILIERELASJON, personDTO.familieRelasjon)
        leggTilOpplysning(FOEDSELSNUMMER, personDTO.foedselsnummer)
        leggTilOpplysning(FOEDSELSAAR, personDTO.foedselsaar)
        leggTilOpplysning(PERSONROLLE, OpplysningDTO(personRolle, null))
        personDTO.avdoedesBarn?.let { leggTilOpplysning(AVDOEDESBARN, OpplysningDTO(it, null)) }
        personDTO.bostedsadresse?.map {
            leggTilOpplysning(
                BOSTEDSADRESSE,
                it,
                it.verdi.gyldigFraOgMed.toYearMonth()?.let { fom ->
                    Periode(
                        fom = fom,
                        tom = it.verdi.gyldigTilOgMed.toYearMonth()
                    )
                }
            )
        }
    }

    val gammalGrunnlagsopplysning = lagOpplysning(opplysningsbehov, person, tidspunktForInnhenting)

    return opplysningsbolk.hentOpplysninger() + gammalGrunnlagsopplysning
}

private fun behovNameTilPersonRolle(opplysningstyper: Opplysningstyper): PersonRolle = when (opplysningstyper) {
    AVDOED_PDL_V1 -> PersonRolle.AVDOED
    GJENLEVENDE_FORELDER_PDL_V1 -> PersonRolle.GJENLEVENDE
    SOEKER_PDL_V1 -> PersonRolle.BARN
    else -> throw Exception("Ugyldig opplysningsbehov")
}

private fun LocalDateTime?.toYearMonth() = this?.let { YearMonth.of(it.year, it.month) }
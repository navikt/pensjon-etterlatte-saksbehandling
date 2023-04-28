package no.nav.etterlatte.opplysninger.kilde.pdl

import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Navn
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.AVDOEDESBARN
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.AVDOED_PDL_V1
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.BOSTEDSADRESSE
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.DELTBOSTEDSADRESSE
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.DOEDSDATO
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.FAMILIERELASJON
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.FOEDELAND
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.FOEDSELSAAR
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.FOEDSELSDATO
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.FOEDSELSNUMMER
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.GJENLEVENDE_FORELDER_PDL_V1
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.KONTAKTADRESSE
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.NAVN
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.OPPHOLDSADRESSE
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.PERSONROLLE
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.SIVILSTAND
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.SIVILSTATUS
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.SOEKER_PDL_V1
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.STATSBORGERSKAP
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.UTLAND
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.VERGEMAALELLERFREMTIDSFULLMAKT
import no.nav.etterlatte.libs.common.pdl.OpplysningDTO
import no.nav.etterlatte.libs.common.pdl.PersonDTO
import no.nav.etterlatte.libs.common.periode.Periode
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt

class Opplysningsbolk(private val fnr: Folkeregisteridentifikator, private val innhentetTidspunkt: Tidspunkt) {
    private val opplysninger = mutableListOf<Grunnlagsopplysning<out Any>>()
    fun leggTilOpplysninger(
        opplysningstype: Opplysningstype,
        grunnlagsopplysning: List<OpplysningDTO<out Any>>?
    ) {
        if (grunnlagsopplysning.isNullOrEmpty()) {
            return
        }
        val opplysningSamlet = OpplysningDTO(
            grunnlagsopplysning.map { it.verdi },
            opplysningsid = grunnlagsopplysning.firstOrNull()?.opplysningsid
        )
        leggTilOpplysning(opplysningstype, opplysningSamlet)
    }

    fun leggTilOpplysning(
        opplysningstype: Opplysningstype,
        opplysningDTO: OpplysningDTO<out Any>?,
        periode: Periode? = null
    ) {
        opplysningDTO?.let {
            opplysninger.add(
                lagPdlPersonopplysning(
                    tidspunktForInnhenting = innhentetTidspunkt,
                    opplysningsType = opplysningstype,
                    opplysning = opplysningDTO,
                    fnr = fnr,
                    periode = periode
                )
            )
        }
    }

    fun hentOpplysninger() = opplysninger.toList()
}

fun lagEnkelopplysningerFraPDL(
    person: Person,
    personDTO: PersonDTO,
    opplysningsbehov: Opplysningstype, // AVDOED_PDL_V1 || SOEKER_PDL_V1 || GJENLEVENDE_PDL_V1
    fnr: Folkeregisteridentifikator
): List<Grunnlagsopplysning<*>> {
    val tidspunktForInnhenting = Tidspunkt.now()
    val opplysningsbolk = Opplysningsbolk(fnr, tidspunktForInnhenting)
    val personRolle = behovNameTilPersonRolle(opplysningsbehov)

    val opplysningNavn =
        OpplysningDTO(
            Navn(personDTO.fornavn.verdi, personDTO.mellomnavn?.verdi, personDTO.etternavn.verdi),
            personDTO.fornavn.opplysningsid
        )

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
        leggTilOpplysninger(SIVILSTAND, personDTO.sivilstand)
        leggTilOpplysning(STATSBORGERSKAP, personDTO.statsborgerskap)
        leggTilOpplysning(UTLAND, personDTO.utland)
        leggTilOpplysning(FAMILIERELASJON, personDTO.familieRelasjon)
        leggTilOpplysning(FOEDSELSNUMMER, personDTO.foedselsnummer)
        leggTilOpplysning(FOEDSELSAAR, personDTO.foedselsaar)
        leggTilOpplysning(PERSONROLLE, OpplysningDTO(personRolle, null))
        personDTO.avdoedesBarn?.let { leggTilOpplysning(AVDOEDESBARN, OpplysningDTO(it, null)) }
        leggTilOpplysninger(BOSTEDSADRESSE, personDTO.bostedsadresse)
    }

    val gammalGrunnlagsopplysning = lagPdlOpplysning(opplysningsbehov, person, tidspunktForInnhenting)

    return opplysningsbolk.hentOpplysninger() + gammalGrunnlagsopplysning
}

private fun behovNameTilPersonRolle(opplysningstype: Opplysningstype): PersonRolle = when (opplysningstype) {
    AVDOED_PDL_V1 -> PersonRolle.AVDOED
    GJENLEVENDE_FORELDER_PDL_V1 -> PersonRolle.GJENLEVENDE
    SOEKER_PDL_V1 -> PersonRolle.BARN
    else -> throw Exception("Ugyldig opplysningsbehov")
}
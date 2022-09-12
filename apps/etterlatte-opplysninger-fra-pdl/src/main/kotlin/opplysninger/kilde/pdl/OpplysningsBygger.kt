package no.nav.etterlatte.opplysninger.kilde.pdl

import no.nav.etterlatte.common.objectMapper
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Navn
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper
import no.nav.etterlatte.libs.common.pdl.OpplysningDTO
import no.nav.etterlatte.libs.common.pdl.PersonDTO
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.person.PersonRolle
import java.time.Instant
import java.util.*

fun lagOpplysninger(
    person: PersonDTO,
    personRolle: PersonRolle,
    fnr: Foedselsnummer
): List<Grunnlagsopplysning<out Any>> {
    val bostedsadresse = person.bostedsadresse?.map {
        lagPersonOpplysning(Opplysningstyper.BOSTEDSADRESSE, it, fnr)
    } ?: emptyList()
    val deltBostedsadresse = person.deltBostedsadresse?.map {
        lagPersonOpplysning(Opplysningstyper.DELTBOSTEDSADRESSE, it, fnr)
    } ?: emptyList()
    val kontaktadresse = person.kontaktadresse?.map {
        lagPersonOpplysning(Opplysningstyper.KONTAKTADRESSE, it, fnr)
    } ?: emptyList()
    val oppholdsadresse = person.oppholdsadresse?.map {
        lagPersonOpplysning(Opplysningstyper.OPPHOLDSADRESSE, it, fnr)
    } ?: emptyList()
    val vergemaal = person.vergemaalEllerFremtidsfullmakt?.map {
        lagPersonOpplysning(Opplysningstyper.VERGEMAALELLERFREMTIDSFULLMAKT, it, fnr)
    } ?: emptyList()

    val periodiserteOpplysninger = bostedsadresse + deltBostedsadresse + kontaktadresse + oppholdsadresse + vergemaal

    val statiskeOpplysninger = listOfNotNull(
        lagPersonOpplysning(
            Opplysningstyper.NAVN,

            OpplysningDTO(
                Navn(fornavn = person.fornavn.verdi, etternavn = person.etternavn.verdi),
                person.fornavn.opplysningsid
            ),
            fnr
        ),
        lagPersonOpplysning(Opplysningstyper.FOEDSELSNUMMER, person.foedselsnummer, fnr),
        lagPersonOpplysning(Opplysningstyper.FOEDSELSAAR, person.foedselsaar, fnr),
        person.foedselsdato?.let { lagPersonOpplysning(Opplysningstyper.FOEDSELSDATO, it, fnr) },
        person.foedeland?.let { lagPersonOpplysning(Opplysningstyper.FOEDELAND, it, fnr) },
        person.doedsdato?.let { lagPersonOpplysning(Opplysningstyper.DOEDSDATO, it, fnr) },
        person.sivilstatus?.let { lagPersonOpplysning(Opplysningstyper.SIVILSTATUS, it, fnr) },
        person.statsborgerskap?.let { lagPersonOpplysning(Opplysningstyper.STATSBORGERSKAP, it, fnr) },
        person.utland?.let { lagPersonOpplysning(Opplysningstyper.UTLAND, it, fnr) },
        person.familieRelasjon?.let { lagPersonOpplysning(Opplysningstyper.FAMILIERELASJON, it, fnr) },
        lagPersonOpplysning(Opplysningstyper.PERSONROLLE, OpplysningDTO(personRolle, null), fnr)
        // person.avdoedesBarn?.let { lagOpplysning(Opplysningstyper.AVDOEDESBARN, personRolle, it) },
    )

    return periodiserteOpplysninger + statiskeOpplysninger
}

fun <T> lagPersonOpplysning(
    opplysningsType: Opplysningstyper,
    opplysning: OpplysningDTO<T>,
    fnr: Foedselsnummer
): Grunnlagsopplysning<T> {
    return Grunnlagsopplysning(
        id = UUID.randomUUID(),
        kilde = Grunnlagsopplysning.Pdl(
            navn = "pdl",
            tidspunktForInnhenting = Instant.now(),
            registersReferanse = null,
            opplysningId = opplysning.opplysningsid.toString()
        ),
        opplysningType = opplysningsType,
        meta = objectMapper.valueToTree(opplysning), // TODO ai: sjekk at denne er riktig
        opplysning = opplysning.verdi,
        fnr = fnr
    )
}

fun <T> lagOpplysning(opplysningsType: Opplysningstyper, opplysning: T): Grunnlagsopplysning<T> {
    return Grunnlagsopplysning(
        UUID.randomUUID(),
        Grunnlagsopplysning.Pdl("pdl", Instant.now(), null, null),
        opplysningsType,
        objectMapper.createObjectNode(),
        opplysning
    )
}
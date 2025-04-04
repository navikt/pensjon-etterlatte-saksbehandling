package no.nav.etterlatte

import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.pdl.OpplysningDTO
import no.nav.etterlatte.libs.common.pdl.PersonDTO
import no.nav.etterlatte.libs.common.person.Adresse
import no.nav.etterlatte.libs.common.person.AdresseType
import no.nav.etterlatte.libs.common.person.FamilieRelasjon
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.GeografiskTilknytning
import no.nav.etterlatte.libs.common.person.PdlIdentifikator
import no.nav.etterlatte.libs.common.person.Utland
import no.nav.etterlatte.libs.common.person.VergemaalEllerFremtidsfullmakt
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeUTC
import no.nav.etterlatte.libs.testdata.grunnlag.AVDOED_FOEDSELSNUMMER
import no.nav.etterlatte.libs.testdata.grunnlag.SOEKER_FOEDSELSNUMMER
import no.nav.etterlatte.pdl.PdlFoedested
import no.nav.etterlatte.pdl.PdlFoedselsdato
import no.nav.etterlatte.pdl.PdlFolkeregisteridentifikator
import no.nav.etterlatte.pdl.PdlHentPerson
import no.nav.etterlatte.pdl.PdlMetadata
import no.nav.etterlatte.pdl.PdlNavn
import no.nav.etterlatte.pdl.PdlSivilstand
import no.nav.etterlatte.pdl.PdlStatsborgerskap
import java.time.LocalDate
import java.util.UUID

object TestHelper

val TRIVIELL_MIDTPUNKT = SOEKER_FOEDSELSNUMMER
val STOR_SNERK = AVDOED_FOEDSELSNUMMER

inline fun <reified T> mockResponse(fil: String): T {
    val json = TestHelper::class.java.getResource(fil)!!.readText()
    return objectMapper.readValue(json, jacksonTypeRef())
}

fun mockPerson(
    utland: Utland? = null,
    familieRelasjon: FamilieRelasjon? = null,
    vergemaal: List<VergemaalEllerFremtidsfullmakt>? = null,
) = PersonDTO(
    fornavn = OpplysningDTO(verdi = "Ola", opplysningsid = null),
    mellomnavn = OpplysningDTO(verdi = "Mellom", opplysningsid = null),
    etternavn = OpplysningDTO(verdi = "Nordmann", opplysningsid = null),
    foedselsnummer = OpplysningDTO(TRIVIELL_MIDTPUNKT, null),
    foedselsdato = OpplysningDTO(LocalDate.now().minusYears(20), UUID.randomUUID().toString()),
    foedselsaar = OpplysningDTO(verdi = 2000, opplysningsid = null),
    foedeland = OpplysningDTO("Norge", UUID.randomUUID().toString()),
    doedsdato = null,
    adressebeskyttelse = null,
    bostedsadresse =
        listOf(
            OpplysningDTO(
                Adresse(
                    type = AdresseType.VEGADRESSE,
                    aktiv = true,
                    coAdresseNavn = "Hos Geir",
                    adresseLinje1 = "Testveien 4",
                    adresseLinje2 = null,
                    adresseLinje3 = null,
                    postnr = "1234",
                    poststed = null,
                    land = "NOR",
                    kilde = "FREG",
                    gyldigFraOgMed = Tidspunkt.now().toLocalDatetimeUTC().minusYears(1),
                    gyldigTilOgMed = null,
                ),
                UUID.randomUUID().toString(),
            ),
        ),
    deltBostedsadresse = emptyList(),
    kontaktadresse = emptyList(),
    oppholdsadresse = emptyList(),
    sivilstatus = null,
    sivilstand = null,
    statsborgerskap = OpplysningDTO("Norsk", UUID.randomUUID().toString()),
    utland = utland?.let { OpplysningDTO(utland, UUID.randomUUID().toString()) },
    familieRelasjon = familieRelasjon?.let { OpplysningDTO(it, UUID.randomUUID().toString()) },
    avdoedesBarn = null,
    vergemaalEllerFremtidsfullmakt = vergemaal?.map { OpplysningDTO(it, UUID.randomUUID().toString()) },
    pdlStatsborgerskap = null,
)

fun mockFolkeregisterident(fnr: String) = PdlIdentifikator.FolkeregisterIdent(Folkeregisteridentifikator.of(fnr))

fun mockGeografiskTilknytning() = GeografiskTilknytning(kommune = "0301", ukjent = false)

fun pdlHentPerson(
    folkeregisteridentifikator: List<PdlFolkeregisteridentifikator> =
        listOf(
            pdlFolkeregisteridentifikator(
                SOEKER_FOEDSELSNUMMER.value,
            ),
        ),
    navn: List<PdlNavn> = listOf(pdlNavn()),
    foedsel: List<PdlFoedselsdato> =
        listOf(
            PdlFoedselsdato(
                foedselsdato = LocalDate.of(1990, 1, 1),
                foedselsaar = 1990,
                metadata = pdlMetadata(),
            ),
        ),
    foedested: List<PdlFoedested> = emptyList(),
    statsborgerskap: List<PdlStatsborgerskap>? = null,
    sivilstand: List<PdlSivilstand>? = null,
): PdlHentPerson =
    PdlHentPerson(
        folkeregisteridentifikator = folkeregisteridentifikator,
        adressebeskyttelse = emptyList(),
        navn = navn,
        foedselsdato = foedsel,
        foedested = foedested,
        sivilstand = sivilstand,
        doedsfall = emptyList(),
        bostedsadresse = null,
        deltBostedsadresse = null,
        kontaktadresse = null,
        oppholdsadresse = null,
        innflyttingTilNorge = null,
        statsborgerskap = statsborgerskap,
        utflyttingFraNorge = null,
        foreldreansvar = null,
        forelderBarnRelasjon = null,
        vergemaalEllerFremtidsfullmakt = null,
    )

fun pdlFolkeregisteridentifikator(
    ident: String,
    historisk: Boolean = false,
) = PdlFolkeregisteridentifikator(
    identifikasjonsnummer = ident,
    status = "status",
    type = "type",
    folkeregistermetadata = null,
    metadata = pdlMetadata(historisk),
)

fun pdlNavn(
    fornavn: String = "fornavn",
    etternavn: String = "etternavn",
) = PdlNavn(fornavn, null, etternavn, metadata = pdlMetadata())

fun pdlMetadata(historisk: Boolean = false): PdlMetadata =
    PdlMetadata(
        endringer = emptyList(),
        historisk = historisk,
        master = "FREG",
        opplysningsId = UUID.randomUUID().toString(),
    )

package no.nav.etterlatte

import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import io.ktor.server.application.Application
import io.ktor.server.routing.Route
import no.nav.etterlatte.ktortokenexchange.SecurityContextMediator
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.Adresse
import no.nav.etterlatte.libs.common.person.AdresseType
import no.nav.etterlatte.libs.common.person.FamilieRelasjon
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.person.FolkeregisterIdent
import no.nav.etterlatte.libs.common.person.Utland
import no.nav.etterlatte.libs.common.person.VergemaalEllerFremtidsfullmakt
import no.nav.etterlatte.pdl.OpplysningDTO
import no.nav.etterlatte.pdl.PersonDTO
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

object TestHelper

val TRIVIELL_MIDTPUNKT = Foedselsnummer.of("19040550081")
val STOR_SNERK = Foedselsnummer.of("11057523044")

inline fun <reified T> mockResponse(fil: String): T {
    val json = TestHelper::class.java.getResource(fil)!!.readText()
    return objectMapper.readValue(json, jacksonTypeRef())
}

fun mockPerson(
    utland: Utland? = null,
    familieRelasjon: FamilieRelasjon? = null,
    vergemaal: List<VergemaalEllerFremtidsfullmakt>? = null
) = PersonDTO(
    fornavn = OpplysningDTO(verdi = "Ola", opplysningsid = null),
    etternavn = OpplysningDTO(verdi = "Nordmann", opplysningsid = null),
    foedselsnummer = OpplysningDTO(TRIVIELL_MIDTPUNKT, null),
    foedselsdato = OpplysningDTO(LocalDate.now().minusYears(20), UUID.randomUUID().toString()),
    foedselsaar = OpplysningDTO(verdi = 2000, opplysningsid = null),
    foedeland = OpplysningDTO("Norge", UUID.randomUUID().toString()),
    doedsdato = null,
    adressebeskyttelse = null,
    bostedsadresse = listOf(
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
                gyldigFraOgMed = LocalDateTime.now().minusYears(1),
                gyldigTilOgMed = null
            ),
            UUID.randomUUID().toString()
        )
    ),
    deltBostedsadresse = listOf(),
    kontaktadresse = listOf(),
    oppholdsadresse = listOf(),
    sivilstatus = null,
    statsborgerskap = OpplysningDTO("Norsk", UUID.randomUUID().toString()),
    utland = utland?.let { OpplysningDTO(utland, UUID.randomUUID().toString()) },
    familieRelasjon = familieRelasjon?.let { OpplysningDTO(it, UUID.randomUUID().toString()) },
    avdoedesBarn = null,
    vergemaalEllerFremtidsfullmakt = vergemaal?.map { OpplysningDTO(it, UUID.randomUUID().toString()) }
)

fun mockFolkeregisterident(fnr: String) = FolkeregisterIdent(Foedselsnummer.of(fnr))

object SecurityContextMediatorStub : SecurityContextMediator {
    override fun outgoingToken(audience: String): suspend () -> String = { "token" }
    override fun installSecurity(ktor: Application) = Unit
    override fun secureRoute(ctx: Route, block: Route.() -> Unit) = block(ctx)
}
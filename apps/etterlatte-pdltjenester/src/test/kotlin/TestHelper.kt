package no.nav.etterlatte

import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import io.ktor.application.*
import io.ktor.routing.*
import no.nav.etterlatte.ktortokenexchange.SecurityContextMediator
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.*
import java.time.LocalDate
import java.time.LocalDateTime

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
    vergemaal: List<VergemaalEllerFremtidsfullmakt>? = null) =

    Person(
        fornavn = "Ola",
        etternavn = "Nordmann",
        foedselsnummer = TRIVIELL_MIDTPUNKT,
        foedselsaar = 2000,
        foedselsdato = LocalDate.now().minusYears(20),
        doedsdato = null,
        adressebeskyttelse = Adressebeskyttelse.UGRADERT,
        bostedsadresse = listOf(
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
            )
        ),
        deltBostedsadresse = emptyList(),
        oppholdsadresse = emptyList(),
        kontaktadresse = emptyList(),
        statsborgerskap = "Norsk",
        foedeland = "Norge",
        sivilstatus = null,
        utland = utland,
        familieRelasjon = familieRelasjon,
        avdoedesBarn = null,
        vergemaalEllerFremtidsfullmakt = vergemaal,
    )

object SecurityContextMediatorStub : SecurityContextMediator {
    override fun outgoingToken(audience: String): suspend () -> String = { "token" }
    override fun installSecurity(ktor: Application) = Unit
    override fun secureRoute(ctx: Route, block: Route.() -> Unit) = block(ctx)
}
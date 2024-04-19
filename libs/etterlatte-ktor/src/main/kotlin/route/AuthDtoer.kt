package no.nav.etterlatte.libs.ktor.route

import no.nav.etterlatte.libs.common.person.AdressebeskyttelseGradering

interface IFoedselsnummerDTO {
    val foedselsnummer: String
}

data class FoedselsnummerDTO(
    override val foedselsnummer: String,
) : IFoedselsnummerDTO

data class FoedselsNummerMedGraderingDTO(
    override val foedselsnummer: String,
    @Deprecated("Ikke bruk, fjernes når fordeleren sin graderingssjekk er borte")
    val gradering: AdressebeskyttelseGradering? = null,
) : IFoedselsnummerDTO

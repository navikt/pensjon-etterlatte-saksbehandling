package no.nav.etterlatte.libs.ktor.route

interface IFoedselsnummerDTO {
    val foedselsnummer: String
}

data class FoedselsnummerDTO(
    override val foedselsnummer: String,
) : IFoedselsnummerDTO

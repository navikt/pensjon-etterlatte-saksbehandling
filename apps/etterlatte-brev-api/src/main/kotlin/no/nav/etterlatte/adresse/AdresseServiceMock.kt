package no.nav.etterlatte.adresse

import no.nav.etterlatte.brev.model.RegoppslagResponseDTO
import no.nav.etterlatte.libs.common.person.Foedselsnummer

class AdresseServiceMock : AdresseService {
    override suspend fun hentMottakerAdresse(id: Foedselsnummer): RegoppslagResponseDTO {
        return RegoppslagResponseDTO(
            navn = "Fornavnet",
            adresse = RegoppslagResponseDTO.Adresse(
                type = RegoppslagResponseDTO.AdresseType.NORSKPOSTADRESSE,
                adresselinje1 = "Adresselinje 1",
                adresselinje2 = "Adresselinje 2",
                adresselinje3 = "Adresselinje 3",
                postnummer = "0000",
                poststed = "Sted",
                land = "Norge",
                landkode = "NO"
            )
        )
    }
}

package no.nav.etterlatte.adresse

import no.nav.etterlatte.brev.model.Mottaker

class AdresseServiceMock : AdresseService {
    override suspend fun hentMottakerAdresse(id: String, accessToken: String): Mottaker {
        return Mottaker("Fornavnet", "Organisasjon", "Veien 22", id, "Oslo")
    }
}

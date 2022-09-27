package no.nav.etterlatte.adresse

import no.nav.etterlatte.brev.model.Mottaker
import no.nav.etterlatte.libs.common.person.Foedselsnummer

class AdresseServiceMock : AdresseService {
    override suspend fun hentMottakerAdresse(id: Foedselsnummer): Mottaker {
        return Mottaker("Fornavnet", "Organisasjon", "Veien 22", id.value, "Oslo")
    }
}

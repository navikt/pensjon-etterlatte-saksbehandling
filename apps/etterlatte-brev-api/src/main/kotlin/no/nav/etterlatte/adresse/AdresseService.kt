package no.nav.etterlatte.adresse

import no.nav.etterlatte.brev.model.RegoppslagResponseDTO
import no.nav.etterlatte.libs.common.person.Foedselsnummer

interface AdresseService {
    suspend fun hentMottakerAdresse(id: Foedselsnummer): RegoppslagResponseDTO
}

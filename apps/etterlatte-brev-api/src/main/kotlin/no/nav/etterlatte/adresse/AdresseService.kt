package no.nav.etterlatte.adresse

import no.nav.etterlatte.brev.model.RegoppslagResponseDTO

interface AdresseService {
    suspend fun hentMottakerAdresse(id: String): RegoppslagResponseDTO
}

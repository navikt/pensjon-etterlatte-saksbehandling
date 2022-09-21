package no.nav.etterlatte.adresse

import no.nav.etterlatte.brev.model.Mottaker

interface AdresseService {
    suspend fun hentMottakerAdresse(id: String, accessToken: String): Mottaker
}

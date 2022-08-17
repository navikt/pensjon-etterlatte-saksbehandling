package dolly

import kotlinx.coroutines.runBlocking

class DollyService(
    private val dollyClient: DollyClient
) {
    // val scopes = listOf("api://${config.getString("dolly.client.id")}/.default")
    // val accessToken = azureAdClient.getAccessTokenForResource(scopes).accessToken

    /**
     * Returnerer ID-en på testgruppen dersom den eksisterer. Hvis ikke må gruppen opprettes manuelt.
     */
    fun hentTestGruppe(username: String, accessToken: String): Long? = runBlocking {
        val bruker = dollyClient.hentDollyBrukere(accessToken)
            .find { it.epost == username }
            ?: throw Exception("Bruker med epost = $username finnes ikke i Dolly.")

        dollyClient.hentBrukersGrupper(bruker.brukerId, accessToken)
            .find { it.navn == testdataGruppe.navn }?.id
    }

    /**
     * Oppretter en ny bestilling i gruppen som er spesifisert.
     */
    fun opprettBestilling(bestilling: String, gruppeId: Long, accessToken: String): BestillingStatus = runBlocking {
        dollyClient.opprettBestilling(bestilling, gruppeId, accessToken)
    }

    companion object {
        private val testdataGruppe = OpprettGruppeRequest(
            navn = "etterlatte-testdata",
            hensikt = "Test av etterlatte-saksbehandling"
        )
    }
}

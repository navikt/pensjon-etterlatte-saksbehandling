package dolly

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.logger
import no.nav.etterlatte.objectMapper

class DollyService(
    private val dollyClient: DollyClient
) {
    /**
     * Returnerer ID-en på testgruppen dersom den eksisterer. Hvis ikke må gruppen opprettes manuelt.
     */
    fun hentTestGruppe(username: String, accessToken: String): Long? = runBlocking {
        val brukere = dollyClient.hentDollyBrukere(accessToken)

        val bruker = brukere
            .filter { bruker -> bruker.brukerId != null }
            .find { it.epost?.uppercase() == username.uppercase() }
            ?: throw Exception("Bruker med epost = $username finnes ikke i Dolly.")

        dollyClient.hentBrukersGrupper(bruker.brukerId!!, accessToken)
            .find { it.navn == testdataGruppe.navn }?.id
    }

    /**
     * Oppretter en ny bestilling i gruppen som er spesifisert.
     */
    fun opprettBestilling(bestilling: String, gruppeId: Long, accessToken: String): BestillingStatus = runBlocking {
        dollyClient.opprettBestilling(bestilling, gruppeId, accessToken)
    }

    /**
     * Hent testfamilier som kan benyttes for å sende inn søknad.
     */
    fun hentFamilier(gruppeId: Long, accessToken: String): List<ForenkletFamilieModell> = runBlocking {
        dollyClient.hentTestGruppeBestillinger(gruppeId, accessToken, 0, 10).let { bestillinger ->
            logger.info("Funnet bestillinger: ${objectMapper.writeValueAsString(bestillinger)}")
            dollyClient.hentPersonInfo(bestillinger.identer.map { it.ident }, accessToken).map { personResponse ->
                ForenkletFamilieModell(
                    avdoed = personResponse.ident,
                    gjenlevende = personResponse.person.sivilstand.first().relatertVedSivilstand,
                    barn = personResponse.person.forelderBarnRelasjon
                        .filter { it.barn }
                        .map { it.relatertPersonsIdent }
                )
            }
        }
    }

    companion object {
        private val testdataGruppe = OpprettGruppeRequest(
            navn = "etterlatte-testdata",
            hensikt = "Test av etterlatte-saksbehandling"
        )
    }
}
package no.nav.etterlatte.testdata.dolly

import kotlinx.coroutines.runBlocking
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class DollyService(
    private val dollyClient: DollyClient
) {
    private val logger: Logger = LoggerFactory.getLogger(DollyService::class.java)

    /**
     * Returnerer ID-en på testgruppen dersom den eksisterer. Hvis ikke må gruppen opprettes manuelt.
     */
    fun hentTestGruppe(username: String, accessToken: String): Long? = runBlocking {
        logger.info("henter brukere")
        val brukere = dollyClient.hentDollyBrukere(accessToken)
        logger.info("brukere: ${brukere.size}")
        val bruker = brukere
            .filter { bruker -> bruker.brukerId != null }
            .find { it.epost?.uppercase() == username.uppercase() }
            ?: throw Exception("Bruker med epost = $username finnes ikke i Dolly.")
        logger.info("bruker navident: ${bruker.navIdent}")

        dollyClient.hentBrukersGrupper(bruker.brukerId!!, accessToken).contents
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
            dollyClient.hentPersonInfo(bestillinger.identer.map { it.ident }, accessToken)
                .mapNotNull { personResponse ->
                    val gjenlevendeEktefelle =
                        personResponse.person.sivilstand.firstOrNull { it.type == "GIFT" }?.relatertVedSivilstand
                    when (gjenlevendeEktefelle) {
                        null -> null
                        else -> ForenkletFamilieModell(
                            avdoed = personResponse.ident,
                            gjenlevende = gjenlevendeEktefelle,
                            barn = personResponse.person.forelderBarnRelasjon
                                .filter { it.barn }
                                .map { it.relatertPersonsIdent }
                        )
                    }
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
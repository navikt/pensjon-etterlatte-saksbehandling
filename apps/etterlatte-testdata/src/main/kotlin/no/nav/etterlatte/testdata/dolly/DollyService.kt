package no.nav.etterlatte.testdata.dolly

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.getDollyAccessToken
import no.nav.etterlatte.producer
import no.nav.etterlatte.rapidsandrivers.Behandlingssteg
import no.nav.etterlatte.testdata.features.dolly.NySoeknadRequest
import no.nav.etterlatte.testdata.features.soeknad.SoeknadMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.UUID

interface DollyInterface {
    fun hentTestGruppeId(
        brukerId: String,
        accessToken: String,
    ): Long?

    fun hentFamilier(
        gruppeId: Long,
        accessToken: String,
    ): List<ForenkletFamilieModell>

    fun opprettBestilling(
        bestilling: String,
        gruppeId: Long,
        accessToken: String,
    ): BestillingStatus

    fun sendSoeknad(
        request: NySoeknadRequest,
        navIdent: String?,
        behandlingssteg: Behandlingssteg,
    ): String

    fun sendSoeknadFraDolly(
        request: NySoeknadRequest,
        navIdent: String?,
        behandlingssteg: Behandlingssteg,
    )
}

class DollyService(
    private val dollyClient: DollyClient,
    private val testnavClient: TestnavClient,
) : DollyInterface {
    private val logger: Logger = LoggerFactory.getLogger(DollyService::class.java)

    /**
     * Returnerer ID-en på testgruppen dersom den eksisterer. Hvis ikke må gruppen opprettes manuelt.
     */
    override fun hentTestGruppeId(
        brukerId: String,
        accessToken: String,
    ): Long? =
        runBlocking {
            val bruker =
                dollyClient.hentDollyBruker(brukerId, accessToken)
                    ?: throw IllegalStateException("Bruker finnes ikke i Dolly.")

            dollyClient
                .hentBrukersGrupper(bruker.brukerId!!, accessToken)
                .contents
                .find { it.navn == testdataGruppe.navn }
                ?.id
        }

    /**
     * Oppretter en ny bestilling i gruppen som er spesifisert.
     */
    override fun opprettBestilling(
        bestilling: String,
        gruppeId: Long,
        accessToken: String,
    ): BestillingStatus =
        runBlocking {
            dollyClient.opprettBestilling(bestilling, gruppeId, accessToken)
        }

    fun statusBestilling(
        bestilling: Long,
        accessToken: String,
    ) = runBlocking {
        dollyClient.hentStatus(bestilling, accessToken)
    }

    /**
     * Hent testfamilier som kan benyttes for å sende inn søknad.
     */
    override fun hentFamilier(
        gruppeId: Long,
        accessToken: String,
    ): List<ForenkletFamilieModell> =
        runBlocking {
            dollyClient.hentTestGruppeBestillinger(gruppeId, accessToken, 0, 10).let { bestillinger ->
                testnavClient
                    .hentPersonInfo(bestillinger.identer.map { it.ident })
                    .mapNotNull { personResponse ->
                        val avdoed = personResponse.ident
                        val ibruk = bestillinger.identer.any { avdoed == it.ident && it.ibruk }

                        val gjenlevendeEktefelle =
                            personResponse.person.sivilstand
                                .firstOrNull { it.type == "GIFT" }
                                ?.relatertVedSivilstand

                        when (gjenlevendeEktefelle) {
                            null -> null
                            else ->
                                ForenkletFamilieModell(
                                    ibruk = ibruk,
                                    avdoed = avdoed,
                                    gjenlevende = gjenlevendeEktefelle,
                                    barn =
                                        personResponse.person.forelderBarnRelasjon
                                            .filter { it.relatertPersonsRolle == "BARN" }
                                            .map { it.relatertPersonsIdent },
                                )
                        }
                    }
            }
        }

    override fun sendSoeknad(
        request: NySoeknadRequest,
        navIdent: String?,
        behandlingssteg: Behandlingssteg,
    ): String {
        val noekkel = UUID.randomUUID().toString()
        val (partisjon, offset) =
            producer.publiser(
                noekkel,
                SoeknadMapper
                    .opprettJsonMessage(
                        type = request.type,
                        gjenlevendeFnr = request.gjenlevende,
                        avdoedFnr = request.avdoed,
                        barn = request.barn,
                        soeker = request.soeker,
                        behandlingssteg = behandlingssteg,
                    ).toJson(),
                mapOf("NavIdent" to (navIdent!!.toByteArray())),
            )
        logger.info("Publiserer melding med partisjon: $partisjon offset: $offset")

        markerSomIBruk(request.avdoed, getDollyAccessToken())
        return noekkel
    }

    override fun sendSoeknadFraDolly(
        request: NySoeknadRequest,
        navIdent: String?,
        behandlingssteg: Behandlingssteg,
    ) {
        val noekkel = UUID.randomUUID().toString()
        val (partisjon, offset) =
            producer.publiser(
                noekkel,
                SoeknadMapper
                    .opprettJsonMessage(
                        type = request.type,
                        gjenlevendeFnr = request.gjenlevende,
                        avdoedFnr = request.avdoed,
                        barn = request.barn,
                        soeker = request.soeker,
                        behandlingssteg = behandlingssteg,
                    ).toJson(),
                mapOf("NavIdent" to (navIdent!!.toByteArray())),
            )
        logger.info("Publiserer melding med partisjon: $partisjon offset: $offset")
    }

    private fun markerSomIBruk(
        ident: String,
        accessToken: String,
    ) = runBlocking {
        try {
            logger.info("Forsøker å markere testident $ident som 'i bruk'")

            dollyClient
                .markerIdentIBruk(ident, accessToken)
                .also { logger.info("Test ident $ident markert med 'iBruk=${it.ibruk}'") }
        } catch (e: Exception) {
            logger.warn("Klart ikke markere testident $ident som 'i bruk'", e)
        }
    }

    companion object {
        private val testdataGruppe =
            OpprettGruppeRequest(
                navn = "etterlatte-testdata",
                hensikt = "Test av etterlatte-saksbehandling",
            )
    }
}

class DollyMock : DollyInterface {
    override fun hentTestGruppeId(
        brukerId: String,
        accessToken: String,
    ): Long = 0

    override fun hentFamilier(
        gruppeId: Long,
        accessToken: String,
    ): List<ForenkletFamilieModell> =
        listOf(
            ForenkletFamilieModell(
                ibruk = true,
                avdoed = "123",
                gjenlevende = "321",
                barn = listOf("044", "555"),
            ),
            ForenkletFamilieModell(
                ibruk = true,
                avdoed = "678",
                gjenlevende = "777",
                barn = listOf("888", "999"),
            ),
        )

    override fun opprettBestilling(
        bestilling: String,
        gruppeId: Long,
        accessToken: String,
    ): BestillingStatus =
        BestillingStatus(
            123L,
            false,
        )

    override fun sendSoeknad(
        request: NySoeknadRequest,
        navIdent: String?,
        behandlingssteg: Behandlingssteg,
    ): String = "123"

    override fun sendSoeknadFraDolly(
        request: NySoeknadRequest,
        navIdent: String?,
        behandlingssteg: Behandlingssteg,
    ) {
    }
}

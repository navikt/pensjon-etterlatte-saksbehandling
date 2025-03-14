package no.nav.etterlatte.behandling.jobs.sjekkadressebeskyttelse

import no.nav.etterlatte.Context
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.common.klienter.PdlTjenesterKlient
import no.nav.etterlatte.funksjonsbrytere.FeatureToggle
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.grunnlag.GrunnlagService
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.tilgangsstyring.OppdaterTilgangService
import org.slf4j.LoggerFactory

enum class SjekkAdressebeskyttelseToggles(
    private val key: String,
) : FeatureToggle {
    SJEKK_ADRESSEBESKYTTELSE_JOBB("sjekk-adressebeskyttelse-jobb"),
    ;

    override fun key(): String = key
}

class SjekkAdressebeskyttelseJobService(
    private val sjekkAdressebeskyttelseJobDao: SjekkAdressebeskyttelseJobDao,
    private val pdlTjenesterKlient: PdlTjenesterKlient,
    private val tilgangService: OppdaterTilgangService,
    private val grunnlagService: GrunnlagService,
    private val featureToggleService: FeatureToggleService,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun setupKontekstAndRun(context: Context) {
        Kontekst.set(context)
        run()
    }

    private fun run() {
        if (featureToggleService.isEnabled(SjekkAdressebeskyttelseToggles.SJEKK_ADRESSEBESKYTTELSE_JOBB, false)) {
            val aktuellSak = SakId(17518)
            val persongalleri = inTransaction { grunnlagService.hentPersongalleri(aktuellSak) }
            if (persongalleri != null) {
                logger.info("Oppdaterer gradering på sak ${aktuellSak.sakId}")
                inTransaction { tilgangService.haandtergraderingOgEgenAnsatt(aktuellSak, persongalleri) }
            }

            /* Fjerner dette inntil videre da vi må se på alle personer i persongalleriet.
            val diff = mutableListOf<SakId>()

            logger.info("Henter saker for å sjekke om adressebeskyttelse er lik på tvers av Gjenny og PDL")
            val saker =
                inTransaction { sjekkAdressebeskyttelseJobDao.hentSakerMedAdressebeskyttelse() }

            logger.info("Fant ${saker.size} saker som skal sjekkes")
            saker.forEachIndexed { index, sak ->
                try {
                    val adressebeskyttelseRequest = HentAdressebeskyttelseRequest(PersonIdent(sak.ident), sak.sakType)
                    val adressebeskyttelseResponse =
                        runBlocking { pdlTjenesterKlient.hentAdressebeskyttelseForPerson(adressebeskyttelseRequest) }

                    if (adressebeskyttelseResponse != sak.adressebeskyttelse) {
                        diff.add(sak.sakId)
                        logger.info("${sak.sakId} har forskjell i adressebeskyttelse mellom Gjenny og PDL")
                    }

                    logger.info("Har sjekket for riktig adressebeskyttelse i ${index + 1} saker")
                } catch (e: Exception) {
                    logger.error("Feilet ved sjekk av adressebeskyttelse i sak ${sak.sakId}")
                }
            }

            logger.info(
                "Fant ${diff.size} saker som har forskjell i adressebeskyttelse mellom Gjenny og PDL: ${diff.map { it.sakId }.joinToString(
                    ", ",
                )}",
            )

             */
        }
    }
}

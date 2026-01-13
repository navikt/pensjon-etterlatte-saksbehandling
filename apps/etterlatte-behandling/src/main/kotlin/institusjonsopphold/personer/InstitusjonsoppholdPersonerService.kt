package no.nav.etterlatte.institusjonsopphold.personer

import institusjonsopphold.personer.InstitusjonsoppholdInternKlient
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.Context
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.logger
import no.nav.etterlatte.sak.SakLesDao

class InstitusjonsoppholdPersonerService(
    private val sakLesDao: SakLesDao,
    private val institusjonsoppholdPersonerDao: InstitusjonsoppholdPersonerDao,
    private val featureToggleService: FeatureToggleService,
    private val institusjonsoppholdInternKlient: InstitusjonsoppholdInternKlient,
) {
    fun setupKontekstAndRun(context: Context) {
        Kontekst.set(context)
        run()
    }

    private fun run() {
        val skalSetteOpp = skalSetteOpp()
        if (skalSetteOpp) {
            settOppKjoeringTabell()
        }

        if (skalKjoere()) {
            var erFerdig = false
            var counter = 0
            while (counter < 300 && !erFerdig) {
                val antallBehandlet = kjoerBolk(50)
                if (antallBehandlet == 0) {
                    erFerdig = true
                }
                counter++
            }
            if (erFerdig) {
                logger.info("Ferdig med å hente institusjonsopphold for alle saker!")
            }
        }
    }

    /**
     * @return Antall personer funnet og behandlet
     */
    @Suppress("SameParameterValue")
    private fun kjoerBolk(antallPersoner: Int): Int =
        inTransaction {
            val personIdenter =
                institusjonsoppholdPersonerDao.hentUbehandledePersoner(antallPersoner)
            if (personIdenter.isNotEmpty()) {
                runBlocking {
                    institusjonsoppholdInternKlient
                        .hentInstitusjonsopphold(personIdenter)
                        .data
                        .forEach { (personIdent, oppholdListe) ->
                            oppholdListe.forEach { institusjonsopphold ->
                                institusjonsoppholdPersonerDao.lagreInstitusjonsopphold(
                                    personIdent,
                                    institusjonsopphold,
                                )
                            }
                        }
                }
                institusjonsoppholdPersonerDao.markerSomFerdig(personIdenter)
            }
            personIdenter.size
        }

    private fun settOppKjoeringTabell() {
        inTransaction {
            val fnrListe = sakLesDao.hentAllePersonerMedBehandlingMedStatusIkkeAvbruttEllerAvslag()
            fnrListe
                .forEach {
                    institusjonsoppholdPersonerDao.lagreKjoering(it)
                }.also {
                    logger.info("Satte opp for kjøring med ${fnrListe.size} saker")
                }
        }
    }

    private fun skalKjoere(): Boolean = featureToggleService.isEnabled(InstitusjonsoppholdPersonerToggles.KjoerHentingFraInst2, false)

    private fun skalSetteOpp(): Boolean = featureToggleService.isEnabled(InstitusjonsoppholdPersonerToggles.SettOppKjoering, false)
}

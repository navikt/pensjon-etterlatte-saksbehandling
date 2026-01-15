package no.nav.etterlatte.institusjonsopphold.oppgaver

import institusjonsopphold.personer.InstitusjonsoppholdInternKlient
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.Context
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.grunnlagsendring.GrunnlagsendringshendelseService
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.institusjonsopphold.InstitusjonsoppholdToggles
import no.nav.etterlatte.institusjonsopphold.model.Institusjonsopphold
import no.nav.etterlatte.institusjonsopphold.model.InstitusjonsoppholdHendelseBeriket
import no.nav.etterlatte.institusjonsopphold.model.InstitusjonsoppholdKilde
import no.nav.etterlatte.institusjonsopphold.model.InstitusjonsoppholdsType
import no.nav.etterlatte.institusjonsopphold.personer.InstitusjonsoppholdPersonerDao
import no.nav.etterlatte.libs.common.feilhaandtering.krevIkkeNull
import no.nav.etterlatte.logger

class InstitusjonsoppholdOppgaverService(
    private val institusjonsoppholdOppgaverDao: InstitusjonsoppholdOppgaverDao,
    private val institusjonsoppholdPersonerDao: InstitusjonsoppholdPersonerDao,
    private val featureToggleService: FeatureToggleService,
    private val grunnlagsendringshendelseService: GrunnlagsendringshendelseService,
    private val institusjonsoppholdInternKlient: InstitusjonsoppholdInternKlient,
) {
    fun setupKontekstAndRun(context: Context) {
        Kontekst.set(context)
        run()
    }

    private fun run() {
        if (skalSetteOpp()) {
            settOppKjoeringTabell()
        }

        if (skalKjoere()) {
            kjoer()
            logger.info("Ferdig med å opprette oppgaver for institusjonsopphold")
        }
    }

    private fun kjoer() {
        val oppholdListe = institusjonsoppholdOppgaverDao.hentUbehandledeOpphold()

        oppholdListe.forEach { oppholdId ->
            val (personIdent, _) = institusjonsoppholdPersonerDao.hentInstitusjonsopphold(oppholdId)
            inTransaction {
                val oppholdFraInst2: Institusjonsopphold =
                    runBlocking {
                        krevIkkeNull(
                            institusjonsoppholdInternKlient.hentInstitusjonsopphold(listOf(personIdent)).data[personIdent],
                        ) { "Fant ikke oppholdet!" }
                            .single { it.oppholdId == oppholdId }
                    }
                grunnlagsendringshendelseService.opprettInstitusjonsOppholdhendelse(
                    oppholdsHendelse =
                        InstitusjonsoppholdHendelseBeriket(
                            hendelseId = 0L,
                            oppholdId = oppholdFraInst2.oppholdId,
                            norskident = personIdent,
                            institusjonsoppholdsType = InstitusjonsoppholdsType.OPPDATERING,
                            institusjonsoppholdKilde = InstitusjonsoppholdKilde.INST,
                            institusjonsType = oppholdFraInst2.institusjonstype,
                            startdato = oppholdFraInst2.startdato,
                            faktiskSluttdato = oppholdFraInst2.faktiskSluttdato,
                            forventetSluttdato = oppholdFraInst2.forventetSluttdato,
                            institusjonsnavn = oppholdFraInst2.institusjonsnavn,
                            organisasjonsnummer = oppholdFraInst2.organisasjonsnummer,
                        ),
                    kommentar =
                        "OBS! Dette er et institusjonsopphold innhentet i ettertid fra INST2, siden vi hadde " +
                            "en periode hvor vi ignorerte hendelsene fra INST2",
                )
                institusjonsoppholdOppgaverDao.markerSomFerdig(oppholdId)
            }
        }
    }

    private fun settOppKjoeringTabell() {
        inTransaction {
            val oppholdListe = institusjonsoppholdOppgaverDao.finnOppholdSomTrengerOppgave()
            oppholdListe
                .forEach {
                    institusjonsoppholdOppgaverDao.lagreKjoering(it)
                }.also {
                    logger.info("Satte opp for kjøring med ${oppholdListe.size} opphold")
                }
        }
    }

    private fun skalKjoere(): Boolean = featureToggleService.isEnabled(InstitusjonsoppholdToggles.LagOppgaverForOpphold, false)

    private fun skalSetteOpp(): Boolean = featureToggleService.isEnabled(InstitusjonsoppholdToggles.SettOppOppgaverForOpphold, false)
}

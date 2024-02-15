package grunnlagsendring.doedshendelse

import io.kotest.matchers.shouldBe
import no.nav.etterlatte.DatabaseExtension
import no.nav.etterlatte.grunnlagsendring.doedshendelse.Doedshendelse
import no.nav.etterlatte.grunnlagsendring.doedshendelse.DoedshendelseDao
import no.nav.etterlatte.grunnlagsendring.doedshendelse.DoedshendelseStatus
import no.nav.etterlatte.grunnlagsendring.doedshendelse.Relasjon
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(DatabaseExtension::class)
class DoedshendelseDaoTest(val dataSource: DataSource) {
    private lateinit var doedshendelseDao: DoedshendelseDao

    @BeforeAll
    fun setup() {
        doedshendelseDao = DoedshendelseDao { dataSource.connection }
    }

    @Test
    fun `Skal opprette ny doedshendelse og hente ut basert paa status`() {
        val doedshendelse =
            Doedshendelse.nyHendelse(
                avdoedFnr = "12345678901",
                avdoedDoedsdato = LocalDate.now(),
                beroertFnr = "12345678901",
                relasjon = Relasjon.BARN,
            )

        doedshendelseDao.opprettDoedshendelse(doedshendelse) shouldBe 1
        doedshendelseDao.hentDoedshendelserMedStatus(DoedshendelseStatus.NY) shouldBe listOf(doedshendelse)
        doedshendelseDao.hentDoedshendelserMedStatus(DoedshendelseStatus.OPPDATERT) shouldBe emptyList()
    }

    @Test
    fun `Skal oppdatere hendelse ferdig med brevid`() {
        // TODO: må først ha egen versjon grunnlagsendringshendelseService.opprettHendelseAvTypeForPerson som setter sak_id
        // doedshendelseDao.oppdaterBrevDistribuertDoedshendelse()
    }
}

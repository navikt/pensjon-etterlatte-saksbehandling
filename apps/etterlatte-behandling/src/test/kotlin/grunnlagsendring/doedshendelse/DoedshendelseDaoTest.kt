package grunnlagsendring.doedshendelse

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import no.nav.etterlatte.DatabaseExtension
import no.nav.etterlatte.common.ConnectionAutoclosing
import no.nav.etterlatte.grunnlagsendring.doedshendelse.Doedshendelse
import no.nav.etterlatte.grunnlagsendring.doedshendelse.DoedshendelseDao
import no.nav.etterlatte.grunnlagsendring.doedshendelse.Relasjon
import no.nav.etterlatte.grunnlagsendring.doedshendelse.Status
import no.nav.etterlatte.grunnlagsendring.doedshendelse.Utfall
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
        doedshendelseDao = DoedshendelseDao(ConnectionAutoclosing(dataSource))
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

        doedshendelseDao.opprettDoedshendelse(doedshendelse)
        doedshendelseDao.hentDoedshendelserMedStatus(Status.NY) shouldBe listOf(doedshendelse)
        doedshendelseDao.hentDoedshendelserMedStatus(Status.OPPDATERT) shouldBe emptyList()
    }

    @Test
    fun `Skal oppdatere avbrutt doedshendelse`() {
        val doedshendelse =
            Doedshendelse.nyHendelse(
                avdoedFnr = "12345678901",
                avdoedDoedsdato = LocalDate.now(),
                beroertFnr = "12345678901",
                relasjon = Relasjon.BARN,
            )
        doedshendelseDao.opprettDoedshendelse(doedshendelse)
        val avbruttHendelse = doedshendelse.tilAvbrutt(5L)

        doedshendelseDao.oppdaterDoedshendelse(avbruttHendelse)

        val ferdigeHendelser = doedshendelseDao.hentDoedshendelserMedStatus(Status.FERDIG)
        ferdigeHendelser shouldContainExactly listOf(avbruttHendelse)
        with(ferdigeHendelser.first()) {
            sakId shouldBe 5L
            status shouldBe Status.FERDIG
            utfall shouldBe Utfall.AVBRUTT
            endret shouldBeGreaterThan opprettet
        }
    }

    @Test
    fun `Skal oppdatere hendelse ferdig med brevid`() {
        // TODO: må først ha egen versjon grunnlagsendringshendelseService.opprettHendelseAvTypeForPerson som setter sak_id
        // doedshendelseDao.oppdaterBrevDistribuertDoedshendelse()
    }
}

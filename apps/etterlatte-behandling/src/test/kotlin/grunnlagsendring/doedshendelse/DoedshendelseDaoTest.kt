package grunnlagsendring.doedshendelse

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import no.nav.etterlatte.ConnectionAutoclosingTest
import no.nav.etterlatte.DatabaseExtension
import no.nav.etterlatte.grunnlagsendring.doedshendelse.DoedshendelseDao
import no.nav.etterlatte.grunnlagsendring.doedshendelse.DoedshendelseInternal
import no.nav.etterlatte.grunnlagsendring.doedshendelse.Relasjon
import no.nav.etterlatte.grunnlagsendring.doedshendelse.Status
import no.nav.etterlatte.grunnlagsendring.doedshendelse.Utfall
import no.nav.etterlatte.libs.common.pdlhendelse.Endringstype
import org.junit.jupiter.api.AfterEach
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
        doedshendelseDao = DoedshendelseDao(ConnectionAutoclosingTest(dataSource))
    }

    @AfterEach
    fun afterEach() {
        dataSource.connection.use {
            it.prepareStatement("TRUNCATE doedshendelse CASCADE;").execute()
        }
    }

    @Test
    fun `Kan hente doedshendelser for avdoed basert på fnr`() {
        val avdoedFnr = "12345678902"
        val doedshendelseInternal =
            DoedshendelseInternal.nyHendelse(
                avdoedFnr = avdoedFnr,
                avdoedDoedsdato = LocalDate.now(),
                beroertFnr = "12345678901",
                relasjon = Relasjon.BARN,
                endringstype = Endringstype.OPPRETTET,
            )

        doedshendelseDao.opprettDoedshendelse(doedshendelseInternal)
        doedshendelseDao.hentDoedshendelserMedStatus(Status.NY) shouldBe listOf(doedshendelseInternal)
        doedshendelseDao.hentDoedshendelserMedStatus(Status.OPPDATERT) shouldBe emptyList()
        doedshendelseDao.hentDoedshendelserForPerson(avdoedFnr) shouldBe listOf(doedshendelseInternal)
    }

    @Test
    fun `Skal opprette ny doedshendelse og hente ut basert paa status`() {
        val doedshendelseInternal =
            DoedshendelseInternal.nyHendelse(
                avdoedFnr = "12345678901",
                avdoedDoedsdato = LocalDate.now(),
                beroertFnr = "12345678901",
                relasjon = Relasjon.BARN,
                endringstype = Endringstype.OPPRETTET,
            )

        doedshendelseDao.opprettDoedshendelse(doedshendelseInternal)
        doedshendelseDao.hentDoedshendelserMedStatus(Status.NY) shouldBe listOf(doedshendelseInternal)
        doedshendelseDao.hentDoedshendelserMedStatus(Status.OPPDATERT) shouldBe emptyList()
    }

    @Test
    fun `Skal oppdatere avbrutt doedshendelse`() {
        val doedshendelseInternal =
            DoedshendelseInternal.nyHendelse(
                avdoedFnr = "12345678901",
                avdoedDoedsdato = LocalDate.now(),
                beroertFnr = "12345678901",
                relasjon = Relasjon.BARN,
                endringstype = Endringstype.OPPRETTET,
            )
        doedshendelseDao.opprettDoedshendelse(doedshendelseInternal)
        val avbruttHendelse = doedshendelseInternal.tilAvbrutt(5L)

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

package no.nav.etterlatte.grunnlagsendring.doedshendelse

import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.shouldBe
import no.nav.etterlatte.ConnectionAutoclosingTest
import no.nav.etterlatte.DatabaseExtension
import no.nav.etterlatte.libs.common.behandling.PersonUtenIdent
import no.nav.etterlatte.libs.common.behandling.RelatertPerson
import no.nav.etterlatte.libs.common.behandling.RelativPersonrolle
import no.nav.etterlatte.libs.common.person.Sivilstand
import no.nav.etterlatte.libs.common.person.Sivilstatus
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(DatabaseExtension::class)
class UkjentBeroertDaoTest(
    val dataSource: DataSource,
) {
    private lateinit var ukjentBeroertDao: UkjentBeroertDao

    @BeforeAll
    fun setup() {
        ukjentBeroertDao = UkjentBeroertDao(ConnectionAutoclosingTest(dataSource))
    }

    @AfterEach
    fun afterEach() {
        dataSource.connection.use {
            it.prepareStatement("TRUNCATE doedshendelse_ukjente_beroerte CASCADE;").execute()
        }
    }

    @Test
    fun `Skal lagre ukjent beroerte uten faktisk ukjente beroerte`() {
        ukjentBeroertDao.hentUkjentBeroert("123") shouldBe null
        val beroerte = UkjentBeroert("123", emptyList(), emptyList())
        ukjentBeroertDao.lagreUkjentBeroert(beroerte)
        ukjentBeroertDao.hentUkjentBeroert("123")!! shouldBeEqual beroerte
    }

    @Test
    fun `Skal lagre ukjent beroerte med faktisk ukjente beroerte`() {
        val avdoedFnr = "08088812345"
        val beroerte =
            UkjentBeroert(
                avdoedFnr = avdoedFnr,
                barnUtenIdent =
                    listOf(
                        PersonUtenIdent(
                            RelativPersonrolle.BARN,
                            RelatertPerson(
                                foedselsdato = java.time.LocalDate.of(2025, 4, 6),
                                kjoenn = "M",
                                navn =
                                    no.nav.etterlatte.libs.common.behandling
                                        .Navn("A", "B", "C"),
                                statsborgerskap = "NOR",
                            ),
                        ),
                    ),
                ektefellerUtenIdent =
                    listOf(
                        Sivilstand(
                            sivilstatus = Sivilstatus.GIFT,
                            relatertVedSiviltilstand = null,
                            gyldigFraOgMed = java.time.LocalDate.now(),
                            bekreftelsesdato = null,
                            kilde = "FREG",
                        ),
                    ),
            )
        ukjentBeroertDao.lagreUkjentBeroert(beroerte)
        ukjentBeroertDao
            .hentUkjentBeroert(avdoedFnr)!!
            .shouldBeEqual(beroerte)
    }
}

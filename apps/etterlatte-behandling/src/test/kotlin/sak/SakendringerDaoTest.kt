package sak

import io.kotest.matchers.equals.shouldBeEqual
import no.nav.etterlatte.ConnectionAutoclosingTest
import no.nav.etterlatte.DatabaseExtension
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.person.AdressebeskyttelseGradering
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.sak.SakendringerDao
import no.nav.etterlatte.sak.Saksendring
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.util.UUID
import javax.sql.DataSource

internal class SakendringerDaoTest(
    val dataSource: DataSource,
) {
    companion object {
        @RegisterExtension
        private val dbExtension = DatabaseExtension()
    }

    private lateinit var sakendringerDao: SakendringerDao

    @BeforeEach
    fun beforeEach() {
        sakendringerDao = SakendringerDao(ConnectionAutoclosingTest(dataSource))
    }

    @AfterEach
    fun afterEach() {
        dbExtension.resetDb()
    }

    @Test
    fun `skal hente det samme som lagres i DB`() {
        val saksendringId = UUID.randomUUID()
        val endringstype = Saksendring.Endringstype.ENDRE_IDENT
        val sakFoer = komplettSak("fnr1")
        val sakEtter = komplettSak("fnr2")
        val tidspunkt = Tidspunkt.now()
        val ident = "JABO"
        val identtype = Saksendring.Identtype.SAKSBEHANDLER

        sakendringerDao.lagreSaksendring(
            Saksendring(
                id = saksendringId,
                endringstype = endringstype,
                foer = sakFoer,
                etter = sakEtter,
                tidspunkt = tidspunkt,
                ident = ident,
                identtype = identtype,
            ),
        )

        val endringForSak = sakendringerDao.hentEndringerForSak(sakEtter.id).single()
        endringForSak.id shouldBeEqual saksendringId
        endringForSak.endringstype shouldBeEqual endringstype
        endringForSak.foer!! shouldBeEqual sakFoer
        endringForSak.etter shouldBeEqual sakEtter
        endringForSak.tidspunkt shouldBeEqual tidspunkt
        endringForSak.ident shouldBeEqual ident
        endringForSak.identtype shouldBeEqual identtype
    }

    private fun komplettSak(fnr: String) =
        KomplettSak(
            SakId(2),
            fnr,
            SakType.OMSTILLINGSSTOENAD,
            AdressebeskyttelseGradering.UGRADERT,
            false,
            Enheter.AALESUND.enhetNr,
            null,
            Tidspunkt.now(),
        )
}

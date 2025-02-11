package no.nav.etterlatte.sak

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.etterlatte.ConnectionAutoclosingTest
import no.nav.etterlatte.DatabaseExtension
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.grunnlagsendring.SakMedEnhet
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.person.AdressebeskyttelseGradering
import no.nav.etterlatte.libs.testdata.grunnlag.SOEKER2_FOEDSELSNUMMER
import no.nav.etterlatte.libs.testdata.grunnlag.SOEKER_FOEDSELSNUMMER
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.RegisterExtension
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SakendringerDaoTest(
    val dataSource: DataSource,
) {
    companion object {
        @RegisterExtension
        private val dbExtension = DatabaseExtension()
    }

    private lateinit var sakendringerDao: SakendringerDao
    private lateinit var sakSkrivDao: SakSkrivDao

    @BeforeEach
    fun beforeEach() {
        sakendringerDao = SakendringerDao(ConnectionAutoclosingTest(dataSource))
        sakSkrivDao = SakSkrivDao(sakendringerDao)
    }

    @AfterEach
    fun afterEach() {
        dbExtension.resetDb()
    }

    @Test
    fun `skal lagre endring ved opprettelse av sak`() {
        val soeker = SOEKER_FOEDSELSNUMMER
        val enhet = Enheter.PORSGRUNN.enhetNr

        val sak = sakSkrivDao.opprettSak(soeker.value, SakType.BARNEPENSJON, enhet)
        val endringForSak = sakendringerDao.hentEndringerForSak(sak.id).first { it.endringstype == Endringstype.OPPRETT_SAK }

        endringForSak.id shouldNotBe null
        endringForSak.endringstype shouldBe Endringstype.OPPRETT_SAK
        endringForSak.foer shouldBe null
        endringForSak.etter.id shouldBe sak.id
        endringForSak.etter.sakType shouldBe SakType.BARNEPENSJON
        endringForSak.etter.ident shouldBe soeker.value
        endringForSak.etter.enhet shouldBe enhet
        endringForSak.etter.erSkjermet shouldBe null
        endringForSak.etter.adressebeskyttelse shouldBe null
        endringForSak.etter.opprettet shouldNotBe null
        endringForSak.tidspunkt shouldNotBe null
        endringForSak.ident shouldNotBe null
        endringForSak.identtype shouldNotBe null
    }

    @Test
    fun `skal lagre endring av enhet`() {
        val soeker = SOEKER_FOEDSELSNUMMER
        val enhet = Enheter.PORSGRUNN.enhetNr
        val enhetNy = Enheter.AALESUND.enhetNr

        val sak = sakSkrivDao.opprettSak(soeker.value, SakType.BARNEPENSJON, enhet)
        sakSkrivDao.oppdaterEnhet(SakMedEnhet(sak.id, Enheter.AALESUND.enhetNr), "en kommentar")
        val endringForSakEnhet =
            sakendringerDao.hentEndringerForSak(sak.id).first { it.endringstype == Endringstype.ENDRE_ENHET }

        endringForSakEnhet.foer?.enhet shouldBe enhet
        endringForSakEnhet.etter.enhet shouldBe enhetNy
    }

    @Test
    fun `skal lagre endring av skjerming`() {
        val soeker = SOEKER_FOEDSELSNUMMER
        val enhet = Enheter.PORSGRUNN.enhetNr

        val sak = sakSkrivDao.opprettSak(soeker.value, SakType.BARNEPENSJON, enhet)
        sakSkrivDao.oppdaterSkjerming(sak.id, true)
        val endringForSakSkjerming = sakendringerDao.hentEndringerForSak(sak.id).first { it.endringstype == Endringstype.ENDRE_SKJERMING }

        endringForSakSkjerming.foer?.erSkjermet shouldBe null
        endringForSakSkjerming.etter.erSkjermet shouldBe true
    }

    @Test
    fun `skal lagre endring av adressebeskyttelse`() {
        val soeker = SOEKER_FOEDSELSNUMMER
        val enhet = Enheter.PORSGRUNN.enhetNr

        val sak = sakSkrivDao.opprettSak(soeker.value, SakType.BARNEPENSJON, enhet)
        sakSkrivDao.oppdaterAdresseBeskyttelse(sak.id, AdressebeskyttelseGradering.STRENGT_FORTROLIG)
        val endringForSakAdressebeskyttelse =
            sakendringerDao.hentEndringerForSak(sak.id).first {
                it.endringstype ==
                    Endringstype.ENDRE_ADRESSEBESKYTTELSE
            }

        endringForSakAdressebeskyttelse.foer?.adressebeskyttelse shouldBe null
        endringForSakAdressebeskyttelse.etter.adressebeskyttelse shouldBe AdressebeskyttelseGradering.STRENGT_FORTROLIG
    }

    @Test
    fun `skal lagre endring av ident`() {
        val soeker = SOEKER_FOEDSELSNUMMER
        val soeker2 = SOEKER2_FOEDSELSNUMMER
        val enhet = Enheter.PORSGRUNN.enhetNr

        val sak = sakSkrivDao.opprettSak(soeker.value, SakType.BARNEPENSJON, enhet)
        sakSkrivDao.oppdaterIdent(sak.id, soeker2)
        val endringForSakNyIdent = sakendringerDao.hentEndringerForSak(sak.id).first { it.endringstype == Endringstype.ENDRE_IDENT }

        endringForSakNyIdent.foer?.ident shouldBe soeker.value
        endringForSakNyIdent.etter.ident shouldBe soeker2.value
    }
}

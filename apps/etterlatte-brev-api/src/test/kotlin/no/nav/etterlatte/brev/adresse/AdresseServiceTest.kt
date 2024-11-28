package no.nav.etterlatte.brev.adresse

import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.randomSakId
import no.nav.etterlatte.brev.AvsenderRequest
import no.nav.etterlatte.brev.adresse.saksbehandler.SaksbehandlerKlient
import no.nav.etterlatte.brev.behandling.Avdoed
import no.nav.etterlatte.brev.behandling.Innsender
import no.nav.etterlatte.brev.behandling.PersonerISak
import no.nav.etterlatte.brev.behandling.Soeker
import no.nav.etterlatte.brev.model.MottakerType
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.person.Vergemaal
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.testdata.grunnlag.AVDOED_FOEDSELSNUMMER
import no.nav.etterlatte.libs.testdata.grunnlag.INNSENDER_FOEDSELSNUMMER
import no.nav.etterlatte.libs.testdata.grunnlag.SOEKER_FOEDSELSNUMMER
import no.nav.etterlatte.libs.testdata.grunnlag.VERGE_FOEDSELSNUMMER
import no.nav.pensjon.brevbaker.api.model.Foedselsnummer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.LocalDate

internal class AdresseServiceTest {
    private val norg2Mock = mockk<Norg2Klient>()
    private val saksbehandlerKlient = mockk<SaksbehandlerKlient>()
    private val regoppslagMock = mockk<RegoppslagKlient>()
    private val adresseService = AdresseService(norg2Mock, saksbehandlerKlient, regoppslagMock)

    companion object {
        private val ANSVARLIG_ENHET = Enheter.defaultEnhet.enhetNr
        private const val SAKSBEHANDLER = "Z123456"
        private const val ATTESTANT = "Z00002"
    }

    @BeforeEach
    fun before() {
        clearAllMocks()
    }

    @AfterEach
    fun after() {
        confirmVerified(norg2Mock, saksbehandlerKlient, regoppslagMock)
    }

    @Test
    fun `Hent avsender og attestant fungerer`() {
        coEvery { norg2Mock.hentEnhet(any()) } returns opprettEnhet()
        coEvery { saksbehandlerKlient.hentSaksbehandlerNavn(SAKSBEHANDLER, any()) } returns "saks behandler"
        coEvery { saksbehandlerKlient.hentSaksbehandlerNavn(ATTESTANT, any()) } returns "att estant"

        val avsenderRequest =
            AvsenderRequest(
                saksbehandlerIdent = SAKSBEHANDLER,
                sakenhet = ANSVARLIG_ENHET,
                attestantIdent = ATTESTANT,
            )
        val faktiskAvsender =
            runBlocking {
                adresseService.hentAvsender(avsenderRequest, mockk())
            }

        faktiskAvsender.saksbehandler shouldBe "saks behandler"
        faktiskAvsender.attestant shouldBe "att estant"

        coVerify(exactly = 1) {
            norg2Mock.hentEnhet(avsenderRequest.sakenhet)
            saksbehandlerKlient.hentSaksbehandlerNavn(avsenderRequest.saksbehandlerIdent, any())
            saksbehandlerKlient.hentSaksbehandlerNavn(avsenderRequest.attestantIdent!!, any())
        }
    }

    @Test
    fun `Hent avsender med innlogget bruker og sak`() {
        val zIdent = "Z123456"

        coEvery { norg2Mock.hentEnhet(any()) } returns opprettEnhet()
        coEvery { saksbehandlerKlient.hentSaksbehandlerNavn(any(), any()) } returns "saks behandler"

        val sakId = randomSakId()
        val sak = Sak("ident", SakType.BARNEPENSJON, sakId, Enheter.defaultEnhet.enhetNr)

        val faktiskAvsender =
            runBlocking {
                adresseService.hentAvsender(AvsenderRequest(saksbehandlerIdent = zIdent, sakenhet = sak.enhet), mockk())
            }

        faktiskAvsender.saksbehandler shouldBe "saks behandler"

        coVerify(exactly = 1) {
            norg2Mock.hentEnhet(sak.enhet)
            saksbehandlerKlient.hentSaksbehandlerNavn(zIdent, any())
        }
    }

    @ParameterizedTest
    @EnumSource(SakType::class)
    fun `Skal IKKE sende kopi hvis innsender og søker er samme person, uten verge`(sakType: SakType) {
        val personerISak =
            PersonerISak(
                innsender = Innsender(Foedselsnummer(SOEKER_FOEDSELSNUMMER.value)),
                soeker = Soeker("RETRO", null, "TELEFONKIOSK", Foedselsnummer(SOEKER_FOEDSELSNUMMER.value)),
                avdoede = listOf(Avdoed(Foedselsnummer(AVDOED_FOEDSELSNUMMER.value), "RIKTIG BOK", LocalDate.now())),
                verge = null,
            )

        coEvery { regoppslagMock.hentMottakerAdresse(any(), any()) } returns opprettRegoppslagResponse()

        val mottakere =
            runBlocking {
                adresseService.hentMottakere(sakType, personerISak)
            }

        mottakere.size shouldBe 1

        val mottaker = mottakere.single()
        mottaker.type shouldBe MottakerType.HOVED
        mottaker.foedselsnummer!!.value shouldBe personerISak.soeker.fnr.value

        coVerify(exactly = 1) {
            regoppslagMock.hentMottakerAdresse(sakType, personerISak.soeker.fnr.value)
        }
    }

    @ParameterizedTest
    @EnumSource(SakType::class)
    fun `Skal sende kopi til innsender hvis verge mangler`(sakType: SakType) {
        val personerISak =
            PersonerISak(
                innsender = Innsender(Foedselsnummer(INNSENDER_FOEDSELSNUMMER.value)),
                soeker = Soeker("LITEN", null, "FLASKE", Foedselsnummer(SOEKER_FOEDSELSNUMMER.value)),
                avdoede = listOf(Avdoed(Foedselsnummer(AVDOED_FOEDSELSNUMMER.value), "RIKTIG BOK", LocalDate.now())),
                verge = null,
            )

        coEvery { regoppslagMock.hentMottakerAdresse(any(), any()) } returns opprettRegoppslagResponse()

        val mottakere =
            runBlocking {
                adresseService.hentMottakere(sakType, personerISak)
            }

        mottakere.size shouldBe 2

        val innsender = mottakere.single { it.foedselsnummer!!.value == personerISak.innsender!!.fnr.value }
        innsender.type shouldBe MottakerType.KOPI

        val soeker = mottakere.single { it.foedselsnummer!!.value == personerISak.soeker.fnr.value }
        soeker.type shouldBe MottakerType.HOVED

        coVerify(exactly = 1) {
            regoppslagMock.hentMottakerAdresse(sakType, personerISak.soeker.fnr.value)
            regoppslagMock.hentMottakerAdresse(sakType, personerISak.innsender!!.fnr.value)
        }
    }

    @ParameterizedTest
    @EnumSource(SakType::class)
    fun `Skal sende kopi til verge hvis verge finnes`(sakType: SakType) {
        val personerISak =
            PersonerISak(
                innsender = Innsender(Foedselsnummer(INNSENDER_FOEDSELSNUMMER.value)),
                soeker = Soeker("LITEN", null, "FLASKE", Foedselsnummer(SOEKER_FOEDSELSNUMMER.value)),
                avdoede = listOf(Avdoed(Foedselsnummer(AVDOED_FOEDSELSNUMMER.value), "RIKTIG BOK", LocalDate.now())),
                verge = Vergemaal("Verg Vergesen", VERGE_FOEDSELSNUMMER),
            )

        coEvery { regoppslagMock.hentMottakerAdresse(any(), any()) } returns opprettRegoppslagResponse()

        val mottakere =
            runBlocking {
                adresseService.hentMottakere(sakType, personerISak)
            }

        mottakere.size shouldBe 2

        val verge =
            mottakere.single { it.foedselsnummer!!.value == (personerISak.verge as Vergemaal).foedselsnummer.value }
        verge.type shouldBe MottakerType.KOPI

        val soeker = mottakere.single { it.foedselsnummer!!.value == personerISak.soeker.fnr.value }
        soeker.type shouldBe MottakerType.HOVED

        coVerify(exactly = 1) {
            regoppslagMock.hentMottakerAdresse(sakType, personerISak.soeker.fnr.value)
            // Skal ikke hente mottaker adresse for verge
        }
    }

    private fun opprettRegoppslagResponse() =
        RegoppslagResponseDTO(
            "Test Testesen",
            adresse =
                RegoppslagResponseDTO.Adresse(
                    type = RegoppslagResponseDTO.AdresseType.NORSKPOSTADRESSE,
                    adresseKilde = RegoppslagResponseDTO.AdresseKilde.BOSTEDSADRESSE,
                    adresselinje1 = "Testveien 13A",
                    adresselinje2 = null,
                    adresselinje3 = null,
                    postnummer = "0123",
                    poststed = "Teststed",
                    landkode = "NO",
                    land = "NORGE",
                ),
        )

    private fun opprettEnhet() =
        Norg2Enhet(
            navn = "NAV Porsgrunn",
            enhetNr = ANSVARLIG_ENHET,
            kontaktinfo =
                Norg2Kontaktinfo(
                    telefonnummer = "00 11 22 33",
                    epost =
                        Norg2Epost(
                            adresse = "test@nav.no",
                        ),
                    postadresse =
                        Postadresse(
                            type = "postboksadresse",
                            postboksnummer = "012345",
                            postboksanlegg = "Testanlegget",
                        ),
                ),
        )
}

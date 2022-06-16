package behandling

import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.*
import no.nav.etterlatte.libs.common.behandling.BehandlingListe
import no.nav.etterlatte.libs.common.behandling.BehandlingSammendrag
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.person.*
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.SoeknadType
import no.nav.etterlatte.libs.common.vikaar.VilkaarResultat
import no.nav.etterlatte.libs.common.vikaar.VurderingsResultat
import no.nav.etterlatte.typer.Saker
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal class BehandlingServiceTest {


    @MockK
    lateinit var behandlingKlient: BehandlingKlient
    @MockK
    lateinit var pdlKlient: PdltjenesterKlient
    @MockK
    lateinit var vedtakKlient: EtterlatteVedtak
    @InjectMockKs
    lateinit var service: BehandlingService
    @BeforeEach
    fun setUp() = MockKAnnotations.init(this)
    private val accessToken = UUID.randomUUID().toString()
    private val fnr = "11057523044"


    @Test
    fun hentPerson() {
        val person = mockPerson()
        val sakliste = Saker(emptyList())
        coEvery { pdlKlient.hentPerson(fnr, accessToken) } returns person
        coEvery { behandlingKlient.hentSakerForPerson(fnr, accessToken) } returns sakliste

        val respons = runBlocking { service.hentPerson(fnr, accessToken) }

        assertSame(person, respons.person)
        assertSame(sakliste, respons.saker)
    }

    @Test
    fun opprettSak() {
        val sak = no.nav.etterlatte.typer.Sak(fnr, SoeknadType.BARNEPENSJON.name, 43)
        coEvery { behandlingKlient.opprettSakForPerson(fnr, SoeknadType.BARNEPENSJON, accessToken) } returns sak

        val respons = runBlocking { service.opprettSak(fnr, SoeknadType.BARNEPENSJON, accessToken) }

        assertEquals(sak.id, respons.id)
        assertEquals(sak.sakType, respons.sakType)
        assertEquals(sak.ident, respons.ident)
    }

    @Test
    fun hentSaker() {
        val saker = Saker(listOf(no.nav.etterlatte.typer.Sak(fnr, SoeknadType.BARNEPENSJON.name, 43)))
        coEvery { behandlingKlient.hentSaker(accessToken) } returns saker

        val respons = runBlocking { service.hentSaker(accessToken) }

        assertEquals(1, respons.saker.size)
        assertEquals(saker.saker.first(), respons.saker.first())
    }

    @Test
    fun hentBehandlingerForSak() {
        val behandling = BehandlingSammendrag(UUID.randomUUID(), 4, null, null, null)
        coEvery { behandlingKlient.hentBehandlingerForSak(4, accessToken) } returns BehandlingListe(listOf(behandling))

        val respons = runBlocking { service.hentBehandlingerForSak(4, accessToken) }

        assertEquals(1, respons.behandlinger.size)
        assertEquals(behandling.id, respons.behandlinger.first().id)
    }

    @Test
    fun hentBehandling() {
        val behandlingid = UUID.randomUUID()
        val detaljertBehandling = DetaljertBehandling(
            behandlingid,
            4,
            LocalDateTime.now(),
            LocalDateTime.now(),
            LocalDateTime.now(),
            null,
            null,
            null,
            null,
            null,
            null,
            null
        )
        val vedtak = Vedtak(
            "4",
            behandlingid,
            null,
            null,
            null,
            VilkaarResultat(VurderingsResultat.OPPFYLT, null, LocalDateTime.now()),
            null,
            null,
            null
        )
        coEvery { behandlingKlient.hentBehandling(behandlingid.toString(), accessToken) } returns detaljertBehandling
        coEvery { vedtakKlient.hentVedtak(4, behandlingid.toString(), accessToken) } returns vedtak


        val respons = runBlocking { service.hentBehandling(behandlingid.toString(), accessToken) }

        assertEquals(behandlingid, respons.id)
        assertEquals(4, respons.sak)
        assertEquals(VurderingsResultat.OPPFYLT, respons.vilkårsprøving?.resultat)
    }

    @Test
    fun opprettBehandling() {
        val behandlingbehov = BehandlingsBehov(4, null)
        val behandlingId = UUID.randomUUID()
        coEvery { behandlingKlient.opprettBehandling(behandlingbehov, accessToken) } returns BehandlingSammendrag(
            behandlingId,
            4,
            null,
            null,
            null
        )

        val respons = runBlocking { service.opprettBehandling(BehandlingsBehov(4, null), accessToken) }

        assertEquals(4, respons.sak)
        assertEquals(behandlingId, respons.id)
    }

    private fun mockPerson(
        utland: Utland? = null,
        familieRelasjon: FamilieRelasjon? = null
    ) =

        Person(
            fornavn = "Ola",
            etternavn = "Nordmann",
            foedselsnummer = Foedselsnummer.of(fnr),
            foedselsaar = 2000,
            foedselsdato = LocalDate.now().minusYears(20),
            doedsdato = null,
            adressebeskyttelse = Adressebeskyttelse.UGRADERT,
            bostedsadresse = listOf(
                Adresse(
                    type = AdresseType.VEGADRESSE,
                    aktiv = true,
                    coAdresseNavn = "Hos Geir",
                    adresseLinje1 = "Testveien 4",
                    adresseLinje2 = null,
                    adresseLinje3 = null,
                    postnr = "1234",
                    poststed = null,
                    land = "NOR",
                    kilde = "FREG",
                    gyldigFraOgMed = LocalDateTime.now().minusYears(1),
                    gyldigTilOgMed = null
                )
            ),
            deltBostedsadresse = emptyList(),
            oppholdsadresse = emptyList(),
            kontaktadresse = emptyList(),
            statsborgerskap = "Norsk",
            foedeland = "Norge",
            sivilstatus = null,
            utland = utland,
            familieRelasjon = familieRelasjon,
            vergemaalEllerFremtidsfullmakt = null
        )
}
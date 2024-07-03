package no.nav.etterlatte.behandling

import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.SaksbehandlerMedEnheterOgRoller
import no.nav.etterlatte.behandling.domain.Foerstegangsbehandling
import no.nav.etterlatte.behandling.hendelse.HendelseDao
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.JaNei
import no.nav.etterlatte.libs.common.behandling.JaNeiMedBegrunnelse
import no.nav.etterlatte.libs.common.behandling.Prosesstype
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsResultat
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsTyper
import no.nav.etterlatte.libs.common.gyldigSoeknad.ManuellVurdering
import no.nav.etterlatte.libs.common.gyldigSoeknad.VurderingsResultat
import no.nav.etterlatte.libs.common.gyldigSoeknad.VurdertGyldighet
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.fixedNorskTid
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeNorskTid
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeUTC
import no.nav.etterlatte.nyKontekstMedBruker
import no.nav.etterlatte.sak.SakDao
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID

internal class GyldighetsproevingServiceImplTest {
    private val user = mockk<SaksbehandlerMedEnheterOgRoller> { every { name() } returns "ident" }
    private val sakDaoMock = mockk<SakDao>()
    private val behandlingDaoMock = mockk<BehandlingDao>()
    private val hendelseDaoMock = mockk<HendelseDao>()
    private val behandlingHendelserKafkaProducerMock = mockk<BehandlingHendelserKafkaProducer>()
    private val naaTid = Tidspunkt.now()
    private val behandlingsService =
        GyldighetsproevingServiceImpl(
            behandlingDaoMock,
            naaTid.fixedNorskTid(),
        )

    @BeforeEach
    fun before() {
        every { user.name() } returns "User"
        nyKontekstMedBruker(user)
    }

    @AfterEach
    fun after() {
        confirmVerified(sakDaoMock, behandlingDaoMock, hendelseDaoMock, behandlingHendelserKafkaProducerMock)
        clearAllMocks()
    }

    @Test
    fun hentFoerstegangsbehandling() {
        val id = UUID.randomUUID()

        every {
            behandlingDaoMock.hentBehandling(id)
        } returns
            Foerstegangsbehandling(
                id = id,
                sak =
                    Sak(
                        ident = "Ola Olsen",
                        sakType = SakType.BARNEPENSJON,
                        id = 1,
                        enhet = Enheter.defaultEnhet.enhetNr,
                    ),
                behandlingOpprettet = Tidspunkt.now().toLocalDatetimeUTC(),
                sistEndret = Tidspunkt.now().toLocalDatetimeUTC(),
                status = BehandlingStatus.OPPRETTET,
                soeknadMottattDato = Tidspunkt.now().toLocalDatetimeUTC(),
                gyldighetsproeving = null,
                virkningstidspunkt =
                    Virkningstidspunkt(
                        YearMonth.of(2022, 1),
                        Grunnlagsopplysning.Saksbehandler.create("ident"),
                        "begrunnelse",
                    ),
                utlandstilknytning = null,
                boddEllerArbeidetUtlandet = null,
                kommerBarnetTilgode = null,
                kilde = Vedtaksloesning.GJENNY,
                sendeBrev = true,
            )

        every {
            user.enheter()
        } returns listOf(Enheter.defaultEnhet.enhetNr)

        behandlingsService.hentFoerstegangsbehandling(id)

        verify(exactly = 1) { behandlingDaoMock.hentBehandling(id) }
    }

    @Test
    fun `lagring av gyldighetsproeving skal lagre og returnere gyldighetsresultat for manuell vurdering`() {
        val id = UUID.randomUUID()
        val now = LocalDateTime.now()

        val behandling =
            Foerstegangsbehandling(
                id = id,
                sak = Sak("", SakType.BARNEPENSJON, 1, Enheter.PORSGRUNN.enhetNr),
                behandlingOpprettet = now,
                sistEndret = now,
                status = BehandlingStatus.OPPRETTET,
                kommerBarnetTilgode = null,
                virkningstidspunkt = null,
                utlandstilknytning = null,
                boddEllerArbeidetUtlandet = null,
                soeknadMottattDato = now,
                gyldighetsproeving = null,
                prosesstype = Prosesstype.MANUELL,
                kilde = Vedtaksloesning.GJENNY,
                sendeBrev = true,
            )
        every { behandlingDaoMock.hentBehandling(any()) } returns behandling

        every { behandlingDaoMock.lagreGyldighetsproeving(any(), any()) } just Runs

        every {
            user.enheter()
        } returns listOf(Enheter.defaultEnhet.enhetNr)

        val forventetResultat =
            GyldighetsResultat(
                resultat = VurderingsResultat.OPPFYLT,
                vurderinger =
                    listOf(
                        VurdertGyldighet(
                            navn = GyldighetsTyper.MANUELL_VURDERING,
                            resultat = VurderingsResultat.OPPFYLT,
                            basertPaaOpplysninger =
                                ManuellVurdering(
                                    begrunnelse = "begrunnelse",
                                    kilde = Grunnlagsopplysning.Saksbehandler("saksbehandler", naaTid),
                                ),
                        ),
                    ),
                vurdertDato = naaTid.toLocalDatetimeNorskTid(),
            )

        val resultat =
            behandlingsService.lagreGyldighetsproeving(
                id,
                JaNeiMedBegrunnelse(JaNei.JA, "begrunnelse"),
                Grunnlagsopplysning.Saksbehandler(
                    "saksbehandler",
                    naaTid,
                ),
            )

        assertEquals(forventetResultat, resultat)

        verify(exactly = 1) {
            behandlingDaoMock.hentBehandling(id)
            behandlingDaoMock.lagreGyldighetsproeving(any(), any())
        }
    }

    @Test
    fun hentFoerstegangsbehandlingMedEnhetOgSaksbehandlerHarEnhet() {
        every {
            user.enheter()
        } returns listOf(Enheter.PORSGRUNN.enhetNr)

        val id = UUID.randomUUID()

        every {
            behandlingDaoMock.hentBehandling(id)
        } returns
            Foerstegangsbehandling(
                id = id,
                sak =
                    Sak(
                        ident = "Ola Olsen",
                        sakType = SakType.BARNEPENSJON,
                        id = 1,
                        enhet = Enheter.PORSGRUNN.enhetNr,
                    ),
                behandlingOpprettet = Tidspunkt.now().toLocalDatetimeUTC(),
                sistEndret = Tidspunkt.now().toLocalDatetimeUTC(),
                status = BehandlingStatus.OPPRETTET,
                soeknadMottattDato = Tidspunkt.now().toLocalDatetimeUTC(),
                gyldighetsproeving = null,
                virkningstidspunkt =
                    Virkningstidspunkt(
                        YearMonth.of(2022, 1),
                        Grunnlagsopplysning.Saksbehandler.create("ident"),
                        "begrunnelse",
                    ),
                utlandstilknytning = null,
                boddEllerArbeidetUtlandet = null,
                kommerBarnetTilgode = null,
                kilde = Vedtaksloesning.GJENNY,
                sendeBrev = true,
            )

        behandlingsService.hentFoerstegangsbehandling(id)

        verify(exactly = 1) { behandlingDaoMock.hentBehandling(id) }
    }
}

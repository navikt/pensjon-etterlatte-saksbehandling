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
import no.nav.etterlatte.behandling.domain.Revurdering
import no.nav.etterlatte.behandling.hendelse.HendelseDao
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.JaNei
import no.nav.etterlatte.libs.common.behandling.JaNeiMedBegrunnelse
import no.nav.etterlatte.libs.common.behandling.Prosesstype
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.SakType
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
import no.nav.etterlatte.sak.SakSkrivDao
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

internal class GyldighetsproevingServiceImplTest {
    private val user = mockk<SaksbehandlerMedEnheterOgRoller> { every { name() } returns "ident" }
    private val sakSkrivDaoMock = mockk<SakSkrivDao>()
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
        confirmVerified(sakSkrivDaoMock, behandlingDaoMock, hendelseDaoMock, behandlingHendelserKafkaProducerMock)
        clearAllMocks()
    }

    @Test
    fun `lagring av gyldighetsproeving skal lagre og returnere gyldighetsresultat for manuell vurdering`() {
        val id = UUID.randomUUID()
        val now = LocalDateTime.now()

        val behandling =
            Foerstegangsbehandling(
                id = id,
                sak = Sak("", SakType.BARNEPENSJON, sakId1, Enheter.PORSGRUNN.enhetNr, null, false),
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
                vedtaksloesning = Vedtaksloesning.GJENNY,
                sendeBrev = true,
            )
        every { behandlingDaoMock.hentBehandling(any()) } returns behandling

        every { behandlingDaoMock.lagreBehandling(any()) } just Runs

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
            behandlingDaoMock.lagreBehandling(any())
        }
    }

    @Test
    fun `lagreGyldighetsproeving returnerer null for revurdering med annen aarsak enn ny soeknad`() {
        val id = UUID.randomUUID()
        val revurdering = mockk<Revurdering>()

        every { behandlingDaoMock.hentBehandling(id) } returns revurdering
        every { revurdering.type } returns BehandlingType.REVURDERING
        every { revurdering.revurderingsaarsak } returns Revurderingaarsak.ANNEN

        val resultat =
            behandlingsService.lagreGyldighetsproeving(
                id,
                JaNeiMedBegrunnelse(JaNei.JA, "begrunnelse"),
                Grunnlagsopplysning.Saksbehandler(
                    "saksbehandler",
                    naaTid,
                ),
            )

        assertEquals(null, resultat)

        verify(exactly = 1) {
            behandlingDaoMock.hentBehandling(id)
        }
        verify(exactly = 0) {
            behandlingDaoMock.lagreBehandling(any())
        }
    }

    @Test
    fun `lagreGyldighetsproeving lagrer og returnerer gyldighetsresultat for revurdering med ny soeknad aarsak`() {
        val id = UUID.randomUUID()
        val revurdering = mockk<Revurdering>(relaxed = true)

        every { behandlingDaoMock.hentBehandling(id) } returns revurdering
        every { revurdering.type } returns BehandlingType.REVURDERING
        every { revurdering.revurderingsaarsak } returns Revurderingaarsak.NY_SOEKNAD
        every { revurdering.oppdaterGyldighetsproeving(any()) } returns revurdering
        every { behandlingDaoMock.lagreBehandling(any()) } just Runs

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
            behandlingDaoMock.lagreBehandling(any())
        }
    }
}

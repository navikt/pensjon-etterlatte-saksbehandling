package no.nav.etterlatte.behandling

import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.Context
import no.nav.etterlatte.DatabaseKontekst
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.SaksbehandlerMedEnheterOgRoller
import no.nav.etterlatte.behandling.domain.Foerstegangsbehandling
import no.nav.etterlatte.behandling.hendelse.HendelseDao
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.JaNei
import no.nav.etterlatte.libs.common.behandling.JaNeiMedBegrunnelse
import no.nav.etterlatte.libs.common.behandling.Persongalleri
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
import no.nav.etterlatte.persongalleri
import no.nav.etterlatte.sak.SakDao
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.sql.Connection
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.*

internal class GyldighetsproevingServiceImplTest {

    private val user = mockk<SaksbehandlerMedEnheterOgRoller>()
    private val sakDaoMock = mockk<SakDao>()
    private val behandlingDaoMock = mockk<BehandlingDao>()
    private val hendelseDaoMock = mockk<HendelseDao>()
    private val behandlingHendelserKafkaProducerMock = mockk<BehandlingHendelserKafkaProducer>()
    private val featureToggleService = mockk<FeatureToggleService>()
    private val naaTid = Tidspunkt.now()
    private val behandlingsService = GyldighetsproevingServiceImpl(
        behandlingDaoMock,
        featureToggleService,
        naaTid.fixedNorskTid()
    )

    @BeforeEach
    fun before() {
        Kontekst.set(
            Context(
                user,
                object : DatabaseKontekst {
                    override fun activeTx(): Connection {
                        throw IllegalArgumentException()
                    }

                    override fun <T> inTransaction(gjenbruk: Boolean, block: () -> T): T {
                        return block()
                    }
                }
            )
        )
    }

    @AfterEach
    fun after() {
        confirmVerified(sakDaoMock, behandlingDaoMock, hendelseDaoMock, behandlingHendelserKafkaProducerMock)
        clearAllMocks()
    }

    @Test
    fun hentFoerstegangsbehandling() {
        val id = UUID.randomUUID()

        every { featureToggleService.isEnabled(BehandlingServiceFeatureToggle.FiltrerMedEnhetId, false) } returns false

        every {
            behandlingDaoMock.hentBehandling(id)
        } returns Foerstegangsbehandling(
            id = id,
            sak = Sak(
                ident = "Ola Olsen",
                sakType = SakType.BARNEPENSJON,
                id = 1,
                enhet = Enheter.defaultEnhet.enhetNr
            ),
            behandlingOpprettet = Tidspunkt.now().toLocalDatetimeUTC(),
            sistEndret = Tidspunkt.now().toLocalDatetimeUTC(),
            status = BehandlingStatus.OPPRETTET,
            soeknadMottattDato = Tidspunkt.now().toLocalDatetimeUTC(),
            persongalleri = Persongalleri(
                soeker = "Ola Olsen",
                "Soeker",
                listOf("Gjenlevende"),
                listOf("Avdoed"),
                emptyList()
            ),
            gyldighetsproeving = null,
            virkningstidspunkt = Virkningstidspunkt(
                YearMonth.of(2022, 1),
                Grunnlagsopplysning.Saksbehandler.create("ident"),
                "begrunnelse"
            ),
            utenlandstilsnitt = null,
            boddEllerArbeidetUtlandet = null,
            kommerBarnetTilgode = null,
            kilde = Vedtaksloesning.GJENNY
        )

        assertEquals("Soeker", behandlingsService.hentFoerstegangsbehandling(id)!!.persongalleri.innsender)

        verify(exactly = 1) { behandlingDaoMock.hentBehandling(id) }
    }

    @Test
    fun `lagring av gyldighetsproeving skal lagre og returnere gyldighetsresultat for innsender er gjenlevende`() {
        val id = UUID.randomUUID()
        val now = LocalDateTime.now()

        val behandling = Foerstegangsbehandling(
            id = id,
            sak = Sak("", SakType.BARNEPENSJON, 1, Enheter.PORSGRUNN.enhetNr),
            behandlingOpprettet = now,
            sistEndret = now,
            status = BehandlingStatus.OPPRETTET,
            persongalleri = persongalleri(),
            kommerBarnetTilgode = null,
            virkningstidspunkt = null,
            boddEllerArbeidetUtlandet = null,
            utenlandstilsnitt = null,
            soeknadMottattDato = now,
            gyldighetsproeving = null,
            prosesstype = Prosesstype.MANUELL,
            kilde = Vedtaksloesning.GJENNY
        )
        every { behandlingDaoMock.hentBehandling(any()) } returns behandling

        every { behandlingDaoMock.lagreGyldighetsproving(any()) } just Runs

        every { featureToggleService.isEnabled(BehandlingServiceFeatureToggle.FiltrerMedEnhetId, false) } returns false

        val forventetResultat = GyldighetsResultat(
            resultat = VurderingsResultat.OPPFYLT,
            vurderinger = listOf(
                VurdertGyldighet(
                    navn = GyldighetsTyper.INNSENDER_ER_GJENLEVENDE,
                    resultat = VurderingsResultat.OPPFYLT,
                    basertPaaOpplysninger = ManuellVurdering(
                        begrunnelse = "begrunnelse",
                        kilde = Grunnlagsopplysning.Saksbehandler("saksbehandler", naaTid)
                    )
                )
            ),
            vurdertDato = naaTid.toLocalDatetimeNorskTid()
        )

        val resultat = behandlingsService.lagreGyldighetsproeving(
            id,
            "saksbehandler",
            JaNeiMedBegrunnelse(JaNei.JA, "begrunnelse")
        )

        assertEquals(forventetResultat, resultat)

        verify(exactly = 1) {
            behandlingDaoMock.hentBehandling(id)
            behandlingDaoMock.lagreGyldighetsproving(any())
        }
    }

    @Test
    fun hentFoerstegangsbehandlingMedEnhetMensFeatureErSkruddAv() {
        val id = UUID.randomUUID()

        every { featureToggleService.isEnabled(BehandlingServiceFeatureToggle.FiltrerMedEnhetId, false) } returns false

        every {
            behandlingDaoMock.hentBehandling(id)
        } returns Foerstegangsbehandling(
            id = id,
            sak = Sak(
                ident = "Ola Olsen",
                sakType = SakType.BARNEPENSJON,
                id = 1,
                enhet = Enheter.PORSGRUNN.enhetNr
            ),
            behandlingOpprettet = Tidspunkt.now().toLocalDatetimeUTC(),
            sistEndret = Tidspunkt.now().toLocalDatetimeUTC(),
            status = BehandlingStatus.OPPRETTET,
            soeknadMottattDato = Tidspunkt.now().toLocalDatetimeUTC(),
            persongalleri = Persongalleri(
                soeker = "Ola Olsen",
                "Soeker",
                listOf("Gjenlevende"),
                listOf("Avdoed"),
                emptyList()
            ),
            gyldighetsproeving = null,
            virkningstidspunkt = Virkningstidspunkt(
                YearMonth.of(2022, 1),
                Grunnlagsopplysning.Saksbehandler.create("ident"),
                "begrunnelse"
            ),
            utenlandstilsnitt = null,
            boddEllerArbeidetUtlandet = null,
            kommerBarnetTilgode = null,
            kilde = Vedtaksloesning.GJENNY
        )

        assertEquals("Soeker", behandlingsService.hentFoerstegangsbehandling(id)!!.persongalleri.innsender)

        verify(exactly = 1) { behandlingDaoMock.hentBehandling(id) }
    }

    @Test
    fun hentFoerstegangsbehandlingMedEnhetOgSaksbehandlerHarEnhet() {
        every {
            user.enheter()
        } returns listOf(Enheter.PORSGRUNN.enhetNr)

        val id = UUID.randomUUID()

        every { featureToggleService.isEnabled(BehandlingServiceFeatureToggle.FiltrerMedEnhetId, false) } returns true

        every {
            behandlingDaoMock.hentBehandling(id)
        } returns Foerstegangsbehandling(
            id = id,
            sak = Sak(
                ident = "Ola Olsen",
                sakType = SakType.BARNEPENSJON,
                id = 1,
                enhet = Enheter.PORSGRUNN.enhetNr
            ),
            behandlingOpprettet = Tidspunkt.now().toLocalDatetimeUTC(),
            sistEndret = Tidspunkt.now().toLocalDatetimeUTC(),
            status = BehandlingStatus.OPPRETTET,
            soeknadMottattDato = Tidspunkt.now().toLocalDatetimeUTC(),
            persongalleri = Persongalleri(
                soeker = "Ola Olsen",
                "Soeker",
                listOf("Gjenlevende"),
                listOf("Avdoed"),
                emptyList()
            ),
            gyldighetsproeving = null,
            virkningstidspunkt = Virkningstidspunkt(
                YearMonth.of(2022, 1),
                Grunnlagsopplysning.Saksbehandler.create("ident"),
                "begrunnelse"
            ),
            utenlandstilsnitt = null,
            boddEllerArbeidetUtlandet = null,
            kommerBarnetTilgode = null,
            kilde = Vedtaksloesning.GJENNY
        )

        assertEquals("Soeker", behandlingsService.hentFoerstegangsbehandling(id)!!.persongalleri.innsender)

        verify(exactly = 1) { behandlingDaoMock.hentBehandling(id) }
    }

    @Test
    fun hentFoerstegangsbehandlingMedEnhetOgSaksbehandlerHarIkkeEnhet() {
        every {
            user.enheter()
        } returns listOf(Enheter.EGNE_ANSATTE.enhetNr)

        val id = UUID.randomUUID()

        every { featureToggleService.isEnabled(BehandlingServiceFeatureToggle.FiltrerMedEnhetId, false) } returns true

        every {
            behandlingDaoMock.hentBehandling(id)
        } returns Foerstegangsbehandling(
            id = id,
            sak = Sak(
                ident = "Ola Olsen",
                sakType = SakType.BARNEPENSJON,
                id = 1,
                enhet = Enheter.PORSGRUNN.enhetNr
            ),
            behandlingOpprettet = Tidspunkt.now().toLocalDatetimeUTC(),
            sistEndret = Tidspunkt.now().toLocalDatetimeUTC(),
            status = BehandlingStatus.OPPRETTET,
            soeknadMottattDato = Tidspunkt.now().toLocalDatetimeUTC(),
            persongalleri = Persongalleri(
                soeker = "Ola Olsen",
                "Soeker",
                listOf("Gjenlevende"),
                listOf("Avdoed"),
                emptyList()
            ),
            gyldighetsproeving = null,
            virkningstidspunkt = Virkningstidspunkt(
                YearMonth.of(2022, 1),
                Grunnlagsopplysning.Saksbehandler.create("ident"),
                "begrunnelse"
            ),
            utenlandstilsnitt = null,
            boddEllerArbeidetUtlandet = null,
            kommerBarnetTilgode = null,
            kilde = Vedtaksloesning.GJENNY
        )

        assertNull(behandlingsService.hentFoerstegangsbehandling(id))

        verify(exactly = 1) { behandlingDaoMock.hentBehandling(id) }
    }
}
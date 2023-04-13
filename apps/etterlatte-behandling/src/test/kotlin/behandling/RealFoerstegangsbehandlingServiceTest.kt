package no.nav.etterlatte.behandling

import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.etterlatte.Context
import no.nav.etterlatte.DatabaseKontekst
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.behandling.domain.Foerstegangsbehandling
import no.nav.etterlatte.behandling.domain.OpprettBehandling
import no.nav.etterlatte.behandling.foerstegangsbehandling.RealFoerstegangsbehandlingService
import no.nav.etterlatte.behandling.hendelse.HendelseDao
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.JaNei
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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.sql.Connection
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.*

internal class RealFoerstegangsbehandlingServiceTest {

    private val sakDaoMock = mockk<SakDao>()
    private val behandlingDaoMock = mockk<BehandlingDao>()
    private val hendelseDaoMock = mockk<HendelseDao>()
    private val hendelserKanalMock = mockk<BehandlingHendelserKanal>()

    private val naaTid = Tidspunkt.now()
    private val sut = RealFoerstegangsbehandlingService(
        sakDaoMock,
        behandlingDaoMock,
        hendelseDaoMock,
        hendelserKanalMock,
        naaTid.fixedNorskTid()
    )

    @BeforeEach
    fun before() {
        Kontekst.set(
            Context(
                mockk(),
                object : DatabaseKontekst {
                    override fun activeTx(): Connection {
                        throw IllegalArgumentException()
                    }

                    override fun <T> inTransaction(block: () -> T): T {
                        return block()
                    }
                }
            )
        )
    }

    @AfterEach
    fun after() {
        confirmVerified(sakDaoMock, behandlingDaoMock, hendelseDaoMock, hendelserKanalMock)
        clearAllMocks()
    }

    @Test
    fun hentFoerstegangsbehandling() {
        val id = UUID.randomUUID()

        every {
            behandlingDaoMock.hentBehandling(id)
        } returns Foerstegangsbehandling(
            id = id,
            sak = Sak(
                ident = "Ola Olsen",
                sakType = SakType.BARNEPENSJON,
                id = 1
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
            kommerBarnetTilgode = null
        )

        assertEquals("Soeker", sut.hentFoerstegangsbehandling(id).persongalleri.innsender)

        verify(exactly = 1) { behandlingDaoMock.hentBehandling(id) }
    }

    @Test
    fun startBehandling() {
        val behandlingOpprettes = slot<OpprettBehandling>()
        val behandlingHentes = slot<UUID>()
        val hendelse = slot<Pair<UUID, BehandlingHendelseType>>()
        val datoNaa = Tidspunkt.now().toLocalDatetimeUTC()

        val opprettetBehandling = Foerstegangsbehandling(
            id = UUID.randomUUID(),
            sak = Sak(
                ident = "Soeker",
                sakType = SakType.BARNEPENSJON,
                id = 1
            ),
            behandlingOpprettet = datoNaa,
            sistEndret = datoNaa,
            status = BehandlingStatus.OPPRETTET,
            soeknadMottattDato = Tidspunkt.now().toLocalDatetimeUTC(),
            persongalleri = Persongalleri(
                "Innsender",
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
            kommerBarnetTilgode = null
        )

        val persongalleri = Persongalleri(
            "Soeker",
            "Innsender",
            emptyList(),
            listOf("Avdoed"),
            listOf("Gjenlevende")
        )

        every { sakDaoMock.hentSak(any()) } returns opprettetBehandling.sak
        every { behandlingDaoMock.opprettBehandling(capture(behandlingOpprettes)) } returns Unit
        every { behandlingDaoMock.hentBehandling(capture(behandlingHentes)) } returns opprettetBehandling
        every { hendelseDaoMock.behandlingOpprettet(any()) } returns Unit
        every { behandlingDaoMock.lagreGyldighetsproving(any()) } returns Unit
        coEvery { hendelserKanalMock.send(capture(hendelse)) } returns Unit

        val resultat = sut.startFoerstegangsbehandling(
            1,
            persongalleri,
            datoNaa.toString()
        )

        assertEquals(opprettetBehandling, resultat)
        assertEquals(opprettetBehandling.persongalleri.avdoed, resultat.persongalleri.avdoed)
        assertEquals(opprettetBehandling.sak, resultat.sak)
        assertEquals(opprettetBehandling.id, resultat.id)
        assertEquals(opprettetBehandling.persongalleri.soeker, resultat.persongalleri.soeker)
        assertEquals(opprettetBehandling.behandlingOpprettet, resultat.behandlingOpprettet)
        assertEquals(1, behandlingOpprettes.captured.sakId)
        assertEquals(behandlingHentes.captured, behandlingOpprettes.captured.id)
        assertEquals(BehandlingHendelseType.OPPRETTET, hendelse.captured.second)

        verify(exactly = 1) {
            sakDaoMock.hentSak(any())
            behandlingDaoMock.hentBehandling(any())
            behandlingDaoMock.opprettBehandling(any())
            hendelseDaoMock.behandlingOpprettet(any())
        }
        coVerify(exactly = 1) { hendelserKanalMock.send(any()) }
    }

    @Test
    fun `lagring av gyldighetsproeving skal lagre og returnere gyldighetsresultat for innsender er gjenlevende`() {
        val id = UUID.randomUUID()
        val now = LocalDateTime.now()

        val behandling = Foerstegangsbehandling(
            id = id,
            sak = Sak("", SakType.BARNEPENSJON, 1, Enheter.DEFAULT.enhetNr),
            behandlingOpprettet = now,
            sistEndret = now,
            status = BehandlingStatus.OPPRETTET,
            persongalleri = persongalleri(),
            kommerBarnetTilgode = null,
            virkningstidspunkt = null,
            soeknadMottattDato = now,
            gyldighetsproeving = null,
            prosesstype = Prosesstype.MANUELL
        )
        every { behandlingDaoMock.hentBehandling(any()) } returns behandling

        every { behandlingDaoMock.lagreGyldighetsproving(any()) } just Runs

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

        val resultat = sut.lagreGyldighetsproeving(id, "saksbehandler", JaNei.JA, "begrunnelse")

        assertEquals(forventetResultat, resultat)

        verify(exactly = 1) {
            behandlingDaoMock.hentBehandling(id)
            behandlingDaoMock.lagreGyldighetsproving(any())
        }
    }
}
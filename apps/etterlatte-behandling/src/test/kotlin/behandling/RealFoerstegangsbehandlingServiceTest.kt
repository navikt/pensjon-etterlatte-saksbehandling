package no.nav.etterlatte.behandling

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.channels.SendChannel
import no.nav.etterlatte.Context
import no.nav.etterlatte.DatabaseKontekst
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.behandling.domain.Foerstegangsbehandling
import no.nav.etterlatte.behandling.domain.OpprettBehandling
import no.nav.etterlatte.behandling.foerstegangsbehandling.FoerstegangsbehandlingAggregat
import no.nav.etterlatte.behandling.foerstegangsbehandling.FoerstegangsbehandlingFactory
import no.nav.etterlatte.behandling.foerstegangsbehandling.RealFoerstegangsbehandlingService
import no.nav.etterlatte.behandling.hendelse.HendelseDao
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsResultat
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsTyper
import no.nav.etterlatte.libs.common.gyldigSoeknad.ManuellVurdering
import no.nav.etterlatte.libs.common.gyldigSoeknad.VurderingsResultat
import no.nav.etterlatte.libs.common.gyldigSoeknad.VurdertGyldighet
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.JaNeiVetIkke
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.fixedNorskTid
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDateTimeNorskTid
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeUTC
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.sql.Connection
import java.time.Instant
import java.time.YearMonth
import java.util.*

internal class RealFoerstegangsbehandlingServiceTest {

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

    @Test
    fun hentFoerstegangsbehandling() {
        val behandlingerMock = mockk<BehandlingDao>()
        val hendelserMock = mockk<HendelseDao>()
        val sut = RealFoerstegangsbehandlingService(
            behandlingerMock,
            FoerstegangsbehandlingFactory(behandlingerMock, hendelserMock),
            mockk()
        )
        val id = UUID.randomUUID()

        every {
            behandlingerMock.hentBehandling(id)
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
            kommerBarnetTilgode = null,
            vilkaarUtfall = null
        )

        assertEquals("Soeker", sut.hentFoerstegangsbehandling(id).persongalleri.innsender)
    }

    @Test
    fun startBehandling() {
        val behandlingerMock = mockk<BehandlingDao>()
        val hendelserMock = mockk<HendelseDao>()
        val behandlingOpprettes = slot<OpprettBehandling>()
        val behandlingHentes = slot<UUID>()
        val hendleseskanal = mockk<SendChannel<Pair<UUID, BehandlingHendelseType>>>()
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
            kommerBarnetTilgode = null,
            vilkaarUtfall = null
        )

        val persongalleri = Persongalleri(
            "Soeker",
            "Innsender",
            emptyList(),
            listOf("Avdoed"),
            listOf("Gjenlevende")
        )

        val sut = RealFoerstegangsbehandlingService(
            behandlingerMock,
            FoerstegangsbehandlingFactory(behandlingerMock, hendelserMock),
            hendleseskanal
        )

        every { behandlingerMock.opprettBehandling(capture(behandlingOpprettes)) } returns Unit
        every {
            behandlingerMock.hentBehandling(capture(behandlingHentes))
        } returns opprettetBehandling
        every { hendelserMock.behandlingOpprettet(any()) } returns Unit
        every { behandlingerMock.lagreGyldighetsproving(any()) } returns Unit
        coEvery { hendleseskanal.send(capture(hendelse)) } returns Unit

        val resultat = sut.startFoerstegangsbehandling(
            1,
            persongalleri,
            datoNaa.toString()
        )

        assertEquals(opprettetBehandling.persongalleri.avdoed, resultat.persongalleri.avdoed)
        assertEquals(opprettetBehandling.sak, resultat.sak)
        assertEquals(opprettetBehandling.id, resultat.id)
        assertEquals(opprettetBehandling.persongalleri.soeker, resultat.persongalleri.soeker)
        assertEquals(opprettetBehandling.behandlingOpprettet, resultat.behandlingOpprettet)
        assertEquals(1, behandlingOpprettes.captured.sakId)
        assertEquals(behandlingHentes.captured, behandlingOpprettes.captured.id)
        assertEquals(resultat.id, hendelse.captured.first)
        assertEquals(BehandlingHendelseType.OPPRETTET, hendelse.captured.second)
    }

    @Test
    fun `lagring av gyldighetsproeving skal lagre og returnere gyldighetsresultat for innsender er gjenlevende`() {
        val id = UUID.randomUUID()
        val naaTid = Instant.now()
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
            vurdertDato = naaTid.toLocalDateTimeNorskTid()!!
        )

        val foerstegangsbehandlingFactory = mockk<FoerstegangsbehandlingFactory>()
        val foerstegangsbehandlingAggregat = mockk<FoerstegangsbehandlingAggregat>()
        every { foerstegangsbehandlingFactory.hentFoerstegangsbehandling(id) } returns foerstegangsbehandlingAggregat
        every { foerstegangsbehandlingAggregat.lagreGyldighetproeving(any()) } returns Unit

        val service =
            RealFoerstegangsbehandlingService(
                mockk<BehandlingDao>(),
                foerstegangsbehandlingFactory,
                mockk(),
                naaTid.fixedNorskTid()
            )
        val resultat = service.lagreGyldighetsproeving(id, "saksbehandler", JaNeiVetIkke.JA, "begrunnelse")

        assertEquals(forventetResultat, resultat)
    }
}
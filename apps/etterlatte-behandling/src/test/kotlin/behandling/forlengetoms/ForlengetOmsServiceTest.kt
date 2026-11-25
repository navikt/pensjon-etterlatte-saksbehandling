package no.nav.etterlatte.behandling.forlengetoms

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.DatabaseExtension
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.behandling.BehandlingDao
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.domain.Revurdering
import no.nav.etterlatte.behandling.domain.TilstandException
import no.nav.etterlatte.behandling.kommerbarnettilgode.KommerBarnetTilGodeDao
import no.nav.etterlatte.behandling.revurdering.RevurderingDao
import no.nav.etterlatte.behandling.revurdering.RevurderingInfoMedBegrunnelse
import no.nav.etterlatte.behandling.revurdering.RevurderingService
import no.nav.etterlatte.behandling.revurdering.RevurderingToggle
import no.nav.etterlatte.common.ConnectionAutoclosingImpl
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.JaNei
import no.nav.etterlatte.libs.common.behandling.RevurderingInfo
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Delvilkaar
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Lovreferanse
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Utfall
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Vilkaar
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarType
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarVurderingData
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Vilkaarsvurdering
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingMedBehandlingGrunnlagsversjon
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingResultat
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingUtfall
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.mockSaksbehandler
import no.nav.etterlatte.nyKontekstMedBrukerOgDatabase
import no.nav.etterlatte.opprettBehandling
import no.nav.etterlatte.revurdering
import no.nav.etterlatte.sak.SakSkrivDao
import no.nav.etterlatte.sak.SakendringerDao
import no.nav.etterlatte.vilkaarsvurdering.service.VilkaarsvurderingService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID
import javax.sql.DataSource

@ExtendWith(DatabaseExtension::class)
class ForlengetOmsServiceTest(
    private val ds: DataSource,
) {
    private val connection = ConnectionAutoclosingImpl(ds)
    private val dao = ForlengetOmsDao(connection)
    private val revurderingDao = RevurderingDao(connection)
    private val behandlingDao = BehandlingDao(KommerBarnetTilGodeDao(connection), revurderingDao, connection)
    private val behandlingService = mockk<BehandlingService>()
    private val vilkaarsvurderingService = mockk<VilkaarsvurderingService>()
    private val toggles = mockk<FeatureToggleService>()
    private val bruker = mockk<BrukerTokenInfo>()
    private val revurderingService =
        RevurderingService(
            oppgaveService = mockk(),
            grunnlagService = mockk(),
            behandlingHendelser = mockk(),
            behandlingDao = behandlingDao,
            hendelseDao = mockk(),
            kommerBarnetTilGodeService = mockk(),
            revurderingDao = revurderingDao,
            aktivitetspliktDao = mockk(),
            aktivitetspliktKopierService = mockk(),
            featureToggleService = toggles,
        )
    private val service = ForlengetOmsService(dao, behandlingService, revurderingService, vilkaarsvurderingService, toggles)
    private val periode = RevurderingInfo.ForlengetOms(YearMonth.of(2027, 1), YearMonth.of(2027, 6))
    private val request =
        LagreForlengetOmsVurdering(
            ForlengetOmsAktivitet.Utdanning("Skole", "Studium", JaNei.JA),
            "Begrunnelse",
            ForlengetOmsUtfall.INNVILGELSE,
            periode,
        )
    private lateinit var behandlingId: UUID
    private lateinit var vilkaarsvurdering: Vilkaarsvurdering

    @BeforeEach
    fun setup() {
        nyKontekstMedBrukerOgDatabase(mockSaksbehandler("Z123456"), ds)
        every { bruker.ident() } returns "Z123456"
        every { toggles.isEnabled(RevurderingToggle.FORLENGET_OMS, false) } returns true
        inTransaction {
            val sak =
                SakSkrivDao(SakendringerDao(connection)).opprettSak(
                    "12345678901",
                    SakType.OMSTILLINGSSTOENAD,
                    Enheter.defaultEnhet.enhetNr,
                )
            val behandling =
                opprettBehandling(
                    BehandlingType.REVURDERING,
                    sak.id,
                    revurderingAarsak = Revurderingaarsak.FORLENGET_OMS,
                    virkningstidspunkt =
                        Virkningstidspunkt(
                            periode.fom,
                            Grunnlagsopplysning.Saksbehandler.create("Z123456"),
                            "Forlengelse",
                        ),
                )
            behandlingDao.opprettBehandling(behandling)
            behandlingId = behandling.id
        }
        vilkaarsvurdering =
            Vilkaarsvurdering(
                behandlingId = behandlingId,
                grunnlagVersjon = 1,
                virkningstidspunkt = periode.fom,
                vilkaar =
                    listOf(
                        Vilkaar(
                            hovedvilkaar =
                                Delvilkaar(
                                    type = VilkaarType.OMS_RETT_UTEN_TIDSBEGRENSNING,
                                    lovreferanse = Lovreferanse("17-4"),
                                    resultat = Utfall.IKKE_OPPFYLT,
                                ),
                            vurdering = VilkaarVurderingData("Begrunnelse", LocalDateTime.now(), "Z123456"),
                        ),
                    ),
            )
        every { behandlingService.hentBehandling(behandlingId) } answers { behandlingDao.hentBehandling(behandlingId) }
        every { vilkaarsvurderingService.hentVilkaarsvurdering(behandlingId) } answers { vilkaarsvurdering }
        every { vilkaarsvurderingService.oppdaterTotalVurdering(behandlingId, bruker, any()) } answers {
            vilkaarsvurdering = vilkaarsvurdering.copy(resultat = thirdArg<VilkaarsvurderingResultat>())
            behandlingDao.lagreStatus(behandlingDao.hentBehandling(behandlingId)!!.tilVilkaarsvurdert())
            VilkaarsvurderingMedBehandlingGrunnlagsversjon(vilkaarsvurdering, 1)
        }
    }

    @AfterEach
    fun cleanup() {
        Kontekst.remove()
    }

    @Test
    fun `lagrer aktivitet periode og eksplisitt utfall samlet`() {
        val lagret = inTransaction { service.lagreVurdering(behandlingId, request, bruker) }

        lagret.vurdering.aktivitet shouldBe request.aktivitet
        lagret.vurdering.begrunnelse shouldBe request.begrunnelse
        lagret.vilkaarsvurdering.vilkaar
            .single()
            .hovedvilkaar.resultat shouldBe Utfall.IKKE_OPPFYLT
        lagret.periode shouldBe periode
        lagret.vilkaarsvurdering.resultat?.utfall shouldBe VilkaarsvurderingUtfall.OPPFYLT
        inTransaction {
            service.hentVurdering(behandlingId) shouldBe lagret.vurdering
            behandlingDao.hentBehandling(behandlingId)?.opphoerFraOgMed shouldBe periode.tom.plusMonths(1)
            behandlingDao.hentBehandling(behandlingId)?.status shouldBe BehandlingStatus.VILKAARSVURDERT
        }
    }

    @Test
    fun `avslag krever ingen periode og fjerner en tidligere foreslaatt forlengelse`() {
        inTransaction {
            revurderingService.lagreRevurderingInfo(behandlingId, RevurderingInfoMedBegrunnelse(periode, null), bruker.ident())
        }
        val avslag =
            request.copy(
                aktivitet = ForlengetOmsAktivitet.IngenAktivitet,
                utfall = ForlengetOmsUtfall.AVSLAG,
                periode = null,
            )

        val lagret = inTransaction { service.lagreVurdering(behandlingId, avslag, bruker) }

        lagret.periode shouldBe null
        lagret.vurdering.utfall shouldBe ForlengetOmsUtfall.AVSLAG
        lagret.vilkaarsvurdering.resultat?.utfall shouldBe VilkaarsvurderingUtfall.IKKE_OPPFYLT
        inTransaction {
            behandlingDao.hentBehandling(behandlingId)?.opphoerFraOgMed shouldBe null
            (behandlingDao.hentBehandling(behandlingId) as Revurdering).revurderingInfo?.revurderingInfo shouldBe null
        }
    }

    @Test
    fun `vilkaar uten vurdering hindrer lagring av aktivitet`() {
        vilkaarsvurdering =
            vilkaarsvurdering.copy(vilkaar = vilkaarsvurdering.vilkaar.map { it.copy(vurdering = null) })

        assertThrows<UgyldigForlengetOmsVurdering> {
            inTransaction { service.lagreVurdering(behandlingId, request, bruker) }
        }
        inTransaction { dao.hentVurdering(behandlingId) shouldBe null }
    }

    @Test
    fun `feil ved lagring av utfall ruller tilbake aktivitet og periode`() {
        every { vilkaarsvurderingService.oppdaterTotalVurdering(behandlingId, bruker, any()) } throws
            IllegalStateException("Feil ved lagring")

        assertThrows<IllegalStateException> {
            inTransaction { service.lagreVurdering(behandlingId, request, bruker) }
        }

        inTransaction {
            dao.hentVurdering(behandlingId) shouldBe null
            behandlingDao.hentBehandling(behandlingId)?.opphoerFraOgMed shouldBe null
            behandlingDao.hentBehandling(behandlingId)?.status shouldBe BehandlingStatus.OPPRETTET
            (behandlingDao.hentBehandling(behandlingId) as Revurdering).revurderingInfo?.revurderingInfo shouldBe null
        }
    }

    @Test
    fun `lagret utfall og deaktivert toggle hindrer endring`() {
        inTransaction { service.lagreVurdering(behandlingId, request, bruker) }
        assertThrows<UgyldigForlengetOmsVurdering> {
            inTransaction { service.lagreVurdering(behandlingId, request, bruker) }
        }
        every { toggles.isEnabled(RevurderingToggle.FORLENGET_OMS, false) } returns false
        assertThrows<UgyldigForlengetOmsVurdering> {
            inTransaction { service.lagreVurdering(behandlingId, request, bruker) }
        }
    }

    @Test
    fun `ufullstendige svar og ugyldig innvilgelse avvises`() {
        val ugyldige =
            listOf(
                request.copy(begrunnelse = " "),
                request.copy(aktivitet = ForlengetOmsAktivitet.Utdanning(" ", "Studium", JaNei.JA)),
                request.copy(aktivitet = ForlengetOmsAktivitet.Utdanning("Skole", " ", JaNei.JA)),
                request.copy(aktivitet = ForlengetOmsAktivitet.Utdanning("Skole", "Studium", JaNei.NEI)),
                request.copy(aktivitet = ForlengetOmsAktivitet.IngenAktivitet),
                request.copy(aktivitet = ForlengetOmsAktivitet.ArbeidsrettetTiltak(" ", Tiltaksomfang.HELTID, JaNei.JA)),
                request.copy(periode = null),
                request.copy(periode = periode.copy(tom = periode.fom.minusMonths(1))),
                request.copy(periode = periode.copy(fom = periode.fom.plusMonths(1))),
            )
        ugyldige.forEach {
            assertThrows<UgyldigForlengetOmsVurdering> {
                inTransaction { service.lagreVurdering(behandlingId, it, bruker) }
            }
        }
        inTransaction { dao.hentVurdering(behandlingId) shouldBe null }
    }

    @Test
    fun `deltid og manglende aktivitetsplan hindrer ikke innvilgelse`() {
        val tiltak =
            request.copy(
                aktivitet = ForlengetOmsAktivitet.ArbeidsrettetTiltak("Tiltak", Tiltaksomfang.DELTID, JaNei.NEI),
            )

        inTransaction { service.lagreVurdering(behandlingId, tiltak, bruker) }.vurdering.aktivitet shouldBe tiltak.aktivitet
    }

    @Test
    fun `attestert og iverksatt behandling kan ikke endres`() {
        val sakId = inTransaction { behandlingDao.hentBehandling(behandlingId)!!.sak.id }
        listOf(BehandlingStatus.ATTESTERT, BehandlingStatus.IVERKSATT).forEach { status ->
            every { behandlingService.hentBehandling(behandlingId) } returns
                revurdering(
                    id = behandlingId,
                    sakId = sakId,
                    sakType = SakType.OMSTILLINGSSTOENAD,
                    status = status,
                    revurderingAarsak = Revurderingaarsak.FORLENGET_OMS,
                )
            assertThrows<TilstandException.KanIkkeRedigere> {
                inTransaction { service.lagreVurdering(behandlingId, request, bruker) }
            }
        }
        inTransaction { dao.hentVurdering(behandlingId) shouldBe null }
    }

    @Test
    fun `vanlig revurdering kan ikke lagre vurdering for forlenget OMS`() {
        val sakId = inTransaction { behandlingDao.hentBehandling(behandlingId)!!.sak.id }
        every { behandlingService.hentBehandling(behandlingId) } returns
            revurdering(
                id = behandlingId,
                sakId = sakId,
                sakType = SakType.OMSTILLINGSSTOENAD,
                revurderingAarsak = Revurderingaarsak.ANNEN,
            )
        assertThrows<UgyldigForlengetOmsVurdering> {
            inTransaction { service.lagreVurdering(behandlingId, request, bruker) }
        }
        inTransaction { dao.hentVurdering(behandlingId) shouldBe null }
    }
}

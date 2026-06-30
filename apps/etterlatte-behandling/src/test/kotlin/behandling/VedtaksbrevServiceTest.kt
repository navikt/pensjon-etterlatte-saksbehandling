package no.nav.etterlatte.behandling

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.klage.KlageService
import no.nav.etterlatte.behandling.klienter.BeregningKlient
import no.nav.etterlatte.behandling.klienter.TrygdetidKlient
import no.nav.etterlatte.behandling.klienter.VedtakInternalService
import no.nav.etterlatte.behandling.vedtaksvurdering.vedtak
import no.nav.etterlatte.brev.BrevKlient
import no.nav.etterlatte.brev.BrevRequest
import no.nav.etterlatte.brev.model.FeilutbetalingType
import no.nav.etterlatte.brev.model.oms.OmstillingsstoenadBeregningRedigerbartVedleggData
import no.nav.etterlatte.brev.model.oms.OmstillingsstoenadInnvilgelseVedtakBrevData
import no.nav.etterlatte.brev.model.oms.OmstillingsstoenadRevurderingVedtakBrevData
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.grunnlag.GrunnlagService
import no.nav.etterlatte.ktor.token.simpleSaksbehandler
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.BehandlingOpprinnelse
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.Klage
import no.nav.etterlatte.libs.common.behandling.Prosesstype
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.beregning.BeregningOgAvkortingDto
import no.nav.etterlatte.libs.common.beregning.BeregningsMetode
import no.nav.etterlatte.libs.common.periode.Periode
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Delvilkaar
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Utfall
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Vilkaar
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarType
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Vilkaarsvurdering
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingResultat
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingUtfall
import no.nav.etterlatte.libs.testdata.behandling.VirkningstidspunktTestData
import no.nav.etterlatte.libs.testdata.grunnlag.GrunnlagTestData
import no.nav.etterlatte.sak.SakService
import no.nav.etterlatte.vilkaarsvurdering.service.VilkaarsvurderingService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.security.SecureRandom
import java.time.LocalDateTime
import java.time.Month
import java.time.YearMonth
import java.util.UUID

internal class VedtaksbrevServiceTest {
    private val stubbedYtelseEtterAvkorting: Int = 4325

    private val klageService = mockk<KlageService>()
    private val brukerTokenInfo = simpleSaksbehandler("Z123456")

    private val behandlingServiceMock = mockk<BehandlingService>()
    private val vedtakInternalService = mockk<VedtakInternalService>()
    private val trygdetidKlient = mockk<TrygdetidKlient>()
    private val vilkaarsvurderingService = mockk<VilkaarsvurderingService>()
    private val sakService = mockk<SakService>()
    private val beregningKlient = mockk<BeregningKlient>()

    private val grunnlagService = mockk<GrunnlagService>()
    private val brevKlient = mockk<BrevKlient>()

    private val service =
        VedtaksbrevService(
            grunnlagService = grunnlagService,
            vedtakInternalService = vedtakInternalService,
            brevKlient = brevKlient,
            behandlingService = behandlingServiceMock,
            beregningKlient = beregningKlient,
            behandlingInfoService = mockk(relaxed = true),
            trygdetidKlient = trygdetidKlient,
            vilkaarsvurderingService = vilkaarsvurderingService,
            sakService = sakService,
            klageService = klageService,
            kodeverkService = mockk(relaxed = true),
            oppgaveService = mockk(relaxed = true),
        )

    @BeforeEach
    fun setUp() {
        coEvery {
            grunnlagService.hentOpplysningsgrunnlag(any())
        } returns GrunnlagTestData().hentOpplysningsgrunnlag()
        coEvery {
            brevKlient.opprettStrukturertBrev(any(), any(), any())
        } returns mockk()
    }

    @AfterEach
    fun tearDown() {
        confirmVerified(brevKlient)
        clearAllMocks()
    }

    @Test
    fun `hentKlageForBehandling returnerer null naar relatertBehandlingId er null`() {
        val behandling = lagDetaljertBehandling(BehandlingType.FØRSTEGANGSBEHANDLING, relatertBehandlingId = null)

        val resultat = service.hentKlageForBehandling(behandling)

        resultat shouldBe null
    }

    @Test
    fun `hentKlageForBehandling returnerer null naar behandlingen verken er foerstegangsbehandling eller omgjoering etter klage`() {
        val behandling =
            lagDetaljertBehandling(
                BehandlingType.REVURDERING,
                aarsak = Revurderingaarsak.SOESKENJUSTERING,
                relatertBehandlingId = UUID.randomUUID(),
            )

        val resultat = service.hentKlageForBehandling(behandling)

        resultat shouldBe null
    }

    @Test
    fun `hentKlageForBehandling returnerer klage for foerstegangsbehandling`() {
        val relatertBehandlingId = UUID.randomUUID()
        val behandling = lagDetaljertBehandling(BehandlingType.FØRSTEGANGSBEHANDLING, relatertBehandlingId = relatertBehandlingId)
        val klage = lagKlage()
        every { klageService.hentKlage(relatertBehandlingId) } returns klage

        val resultat = service.hentKlageForBehandling(behandling)

        resultat shouldBe klage
    }

    @Test
    fun `hentKlageForBehandling returnerer klage for omgjoering etter klage`() {
        val relatertBehandlingId = UUID.randomUUID()
        val behandling =
            lagDetaljertBehandling(
                BehandlingType.REVURDERING,
                aarsak = Revurderingaarsak.OMGJOERING_ETTER_KLAGE,
                relatertBehandlingId = relatertBehandlingId,
            )
        val klage = lagKlage()
        every { klageService.hentKlage(relatertBehandlingId) } returns klage

        val resultat = service.hentKlageForBehandling(behandling)

        resultat shouldBe klage
    }

    @Test
    fun `oppretter vedtaksbrev for innvilgelse`() {
        val sakType = SakType.OMSTILLINGSSTOENAD
        val sakId = SakId(1336)
        val sak = Sak("fnr", sakType, sakId, Enheter.defaultEnhet.enhetNr, null, null)
        val virkningstidspunkt = VirkningstidspunktTestData.virkningstidsunkt(dato = YearMonth.of(2024, Month.MARCH))
        val revurdering =
            lagDetaljertBehandling(
                type = BehandlingType.FØRSTEGANGSBEHANDLING,
                sakId = sak.id,
                virkningstidspunkt = virkningstidspunkt,
            )
        val vedtak =
            vedtak(
                id = SecureRandom().nextLong(),
                virkningstidspunkt = virkningstidspunkt.dato,
                sakId = sakId,
                sakType = sakType,
                behandlingId = revurdering.id,
                type = VedtakType.INNVILGELSE,
            )
        every { sakService.finnSak(sakId) } returns sak

        every { behandlingServiceMock.hentDetaljertBehandling(revurdering.id, brukerTokenInfo) } returns revurdering
        coEvery { vedtakInternalService.hentVedtak(revurdering.id, brukerTokenInfo) } returns vedtak.toDto()
        coEvery {
            vilkaarsvurderingService.hentVilkaarsvurdering(revurdering.id)
        } returns
            vilkaarsvurdering(
                behandlingId = revurdering.id,
                virkningstidspunkt = virkningstidspunkt.dato,
                utfall = VilkaarsvurderingUtfall.OPPFYLT,
                omsRettUtenTidsbegrensning = Utfall.OPPFYLT,
            )
        coEvery {
            beregningKlient.hentBeregningOgAvkorting(revurdering.id, brukerTokenInfo)
        } returns
            beregningOgAvkortingDto(
                behandling = revurdering,
                metode = BeregningsMetode.NASJONAL,
            )
        coEvery {
            trygdetidKlient.hentTrygdetid(revurdering.id, brukerTokenInfo)
        } returns listOf(mockk(relaxed = true))

        runBlocking {
            service.opprettVedtaksbrev(revurdering.id, bruker = brukerTokenInfo)
        }

        val brevSlot = slot<BrevRequest>()
        coVerify {
            brevKlient.opprettStrukturertBrev(
                revurdering.id,
                capture(brevSlot),
                eq(brukerTokenInfo),
            )
        }

        with(brevSlot.captured) {
            this.sak.id shouldBe sakId
            (this.brevFastInnholdData as OmstillingsstoenadInnvilgelseVedtakBrevData.Vedtak).let {
                it.omsRettUtenTidsbegrensning shouldBe true
                it.bosattUtland shouldBe false
                it.beregning.trygdetid.beregningsMetodeAnvendt shouldBe BeregningsMetode.NASJONAL
                it.beregning.beregningsperioder shouldHaveSize 1
            }
            (this.brevRedigerbarInnholdData as OmstillingsstoenadInnvilgelseVedtakBrevData.VedtakInnhold).let {
                it.beregning.beregningsperioder shouldHaveSize 1
                it.utbetalingsbeloep.value shouldBe stubbedYtelseEtterAvkorting
            }
            (this.brevVedleggData.single().data as OmstillingsstoenadBeregningRedigerbartVedleggData).let {
                it.omstillingsstoenadBeregning.beregningsperioder shouldHaveSize 1
                it.omstillingsstoenadBeregning.trygdetid.beregningsMetodeAnvendt shouldBe BeregningsMetode.NASJONAL
                it.omstillingsstoenadBeregning.virkningsdato shouldBe virkningstidspunkt.dato.atDay(1)
            }
        }
    }

    @Test
    fun `oppretter vedtaksbrev for endring`() {
        val sakType = SakType.OMSTILLINGSSTOENAD
        val sakId = SakId(1336)
        val sak = Sak("fnr", sakType, sakId, Enheter.defaultEnhet.enhetNr, null, null)
        val virkningstidspunkt = VirkningstidspunktTestData.virkningstidsunkt(dato = YearMonth.of(2024, Month.MARCH))
        val revurdering =
            lagDetaljertBehandling(
                type = BehandlingType.REVURDERING,
                sakId = sak.id,
                virkningstidspunkt = virkningstidspunkt,
            )
        val vedtak =
            vedtak(
                id = SecureRandom().nextLong(),
                virkningstidspunkt = virkningstidspunkt.dato,
                sakId = sakId,
                sakType = sakType,
                behandlingId = revurdering.id,
                type = VedtakType.ENDRING,
            )
        every { sakService.finnSak(sakId) } returns sak

        every { behandlingServiceMock.hentDetaljertBehandling(revurdering.id, brukerTokenInfo) } returns revurdering
        coEvery { vedtakInternalService.hentVedtak(revurdering.id, brukerTokenInfo) } returns vedtak.toDto()
        coEvery {
            vilkaarsvurderingService.hentVilkaarsvurdering(revurdering.id)
        } returns
            vilkaarsvurdering(
                behandlingId = revurdering.id,
                virkningstidspunkt = virkningstidspunkt.dato,
                utfall = VilkaarsvurderingUtfall.OPPFYLT,
                omsRettUtenTidsbegrensning = Utfall.OPPFYLT,
            )
        coEvery {
            beregningKlient.hentBeregningOgAvkorting(revurdering.id, brukerTokenInfo)
        } returns
            beregningOgAvkortingDto(
                behandling = revurdering,
                metode = BeregningsMetode.NASJONAL,
            )
        coEvery {
            trygdetidKlient.hentTrygdetid(revurdering.id, brukerTokenInfo)
        } returns listOf(mockk(relaxed = true))

        runBlocking {
            service.opprettVedtaksbrev(revurdering.id, bruker = brukerTokenInfo)
        }

        val brevSlot = slot<BrevRequest>()
        coVerify {
            brevKlient.opprettStrukturertBrev(
                revurdering.id,
                capture(brevSlot),
                eq(brukerTokenInfo),
            )
        }

        with(brevSlot.captured) {
            this.sak.id shouldBe sakId
            (this.brevFastInnholdData as OmstillingsstoenadRevurderingVedtakBrevData.Vedtak).let {
                it.omsRettUtenTidsbegrensning shouldBe true
                it.feilutbetaling shouldBe FeilutbetalingType.INGEN_FEILUTBETALING
                it.bosattUtland shouldBe false
                it.innholdForhaandsvarsel shouldBe emptyList()
                it.beregning.trygdetid.beregningsMetodeAnvendt shouldBe BeregningsMetode.NASJONAL
                it.beregning.beregningsperioder shouldHaveSize 1
            }
            (this.brevRedigerbarInnholdData as OmstillingsstoenadRevurderingVedtakBrevData.VedtakInnhold).let {
                it.feilutbetaling shouldBe FeilutbetalingType.INGEN_FEILUTBETALING
            }
            (this.brevVedleggData.single().data as OmstillingsstoenadBeregningRedigerbartVedleggData).let {
                it.omstillingsstoenadBeregning.beregningsperioder shouldHaveSize 1
                it.omstillingsstoenadBeregning.trygdetid.beregningsMetodeAnvendt shouldBe BeregningsMetode.NASJONAL
                it.omstillingsstoenadBeregning.virkningsdato shouldBe virkningstidspunkt.dato.atDay(1)
            }
        }
    }

    @Test
    fun `oppretter vedtaksbrev for opphoer`() {
        val sakType = SakType.OMSTILLINGSSTOENAD
        val sakId = SakId(1336)
        val sak = Sak("fnr", sakType, sakId, Enheter.defaultEnhet.enhetNr, null, null)
        val virkningstidspunkt = VirkningstidspunktTestData.virkningstidsunkt(dato = YearMonth.of(2024, Month.MARCH))
        val revurdering =
            lagDetaljertBehandling(
                type = BehandlingType.REVURDERING,
                sakId = sak.id,
                virkningstidspunkt = virkningstidspunkt,
            )
        val vedtak =
            vedtak(
                id = SecureRandom().nextLong(),
                virkningstidspunkt = virkningstidspunkt.dato,
                sakId = sakId,
                sakType = sakType,
                behandlingId = revurdering.id,
                type = VedtakType.OPPHOER,
            )
        every { sakService.finnSak(sakId) } returns sak

        every { behandlingServiceMock.hentDetaljertBehandling(revurdering.id, brukerTokenInfo) } returns revurdering
        coEvery { vedtakInternalService.hentVedtak(revurdering.id, brukerTokenInfo) } returns vedtak.toDto()
        coEvery {
            vilkaarsvurderingService.hentVilkaarsvurdering(revurdering.id)
        } returns vilkaarsvurdering(revurdering.id, virkningstidspunkt.dato, VilkaarsvurderingUtfall.IKKE_OPPFYLT)

        runBlocking {
            service.opprettVedtaksbrev(revurdering.id, bruker = brukerTokenInfo)
        }

        val brevSlot = slot<BrevRequest>()
        coVerify {
            brevKlient.opprettStrukturertBrev(
                revurdering.id,
                capture(brevSlot),
                eq(brukerTokenInfo),
            )
        }

        with(brevSlot.captured) {
            this.sak.id shouldBe sakId
            (this.brevFastInnholdData as OmstillingsstoenadRevurderingVedtakBrevData.VedtakOpphoer).let {
                it.virkningsdato shouldBe virkningstidspunkt.dato.atDay(1)
                it.feilutbetaling shouldBe FeilutbetalingType.INGEN_FEILUTBETALING
                it.bosattUtland shouldBe false
                it.innholdForhaandsvarsel shouldBe emptyList()
            }
            (this.brevRedigerbarInnholdData as OmstillingsstoenadRevurderingVedtakBrevData.VedtakInnholdOpphoer).let {
                it.feilutbetaling shouldBe FeilutbetalingType.INGEN_FEILUTBETALING
            }
            this.brevVedleggData shouldBe emptyList()
        }
    }

    private fun lagDetaljertBehandling(
        type: BehandlingType,
        aarsak: Revurderingaarsak? = null,
        relatertBehandlingId: UUID? = null,
        sakId: SakId = SAK_ID,
        virkningstidspunkt: Virkningstidspunkt? = null,
    ) = DetaljertBehandling(
        id = UUID.randomUUID(),
        sak = sakId,
        sakType = SakType.OMSTILLINGSSTOENAD,
        soeker = "123",
        status = BehandlingStatus.OPPRETTET,
        behandlingType = type,
        virkningstidspunkt = virkningstidspunkt,
        boddEllerArbeidetUtlandet = null,
        utlandstilknytning = null,
        revurderingsaarsak = aarsak,
        prosesstype = Prosesstype.MANUELL,
        revurderingInfo = null,
        vedtaksloesning = Vedtaksloesning.GJENNY,
        sendeBrev = true,
        opphoerFraOgMed = null,
        relatertBehandlingId = relatertBehandlingId,
        tidligereFamiliepleier = null,
        opprinnelse = BehandlingOpprinnelse.UKJENT,
    )

    private fun lagKlage() =
        Klage.ny(
            Sak("ident", SakType.OMSTILLINGSSTOENAD, SAK_ID, Enheter.defaultEnhet.enhetNr, null, null),
            null,
        )

    private companion object {
        val SAK_ID = SakId(1L)
    }

    private fun vilkaarsvurdering(
        behandlingId: UUID,
        virkningstidspunkt: YearMonth,
        utfall: VilkaarsvurderingUtfall,
        omsRettUtenTidsbegrensning: Utfall = Utfall.IKKE_OPPFYLT,
    ): Vilkaarsvurdering =
        Vilkaarsvurdering(
            behandlingId = behandlingId,
            grunnlagVersjon = 1L,
            virkningstidspunkt = virkningstidspunkt,
            vilkaar =
                listOf(
                    Vilkaar(
                        hovedvilkaar =
                            Delvilkaar(
                                type = VilkaarType.OMS_RETT_UTEN_TIDSBEGRENSNING,
                                lovreferanse = mockk(relaxed = true),
                                resultat = omsRettUtenTidsbegrensning,
                            ),
                    ),
                ),
            resultat =
                VilkaarsvurderingResultat(
                    utfall = utfall,
                    kommentar = null,
                    tidspunkt = LocalDateTime.now(),
                    saksbehandler = "JABO",
                ),
        )

    private fun beregningOgAvkortingDto(
        behandling: DetaljertBehandling,
        metode: BeregningsMetode,
    ): BeregningOgAvkortingDto =
        BeregningOgAvkortingDto(
            perioder =
                listOf(
                    mockk(relaxed = true) {
                        every { beregningsMetode } returns metode
                        every { periode } returns Periode(fom = behandling.virkningstidspunkt!!.dato, tom = null)
                        every { ytelseEtterAvkorting } returns stubbedYtelseEtterAvkorting
                    },
                ),
            erInnvilgelsesaar = true,
            endringIUtbetalingVedVirk = true,
        )
}

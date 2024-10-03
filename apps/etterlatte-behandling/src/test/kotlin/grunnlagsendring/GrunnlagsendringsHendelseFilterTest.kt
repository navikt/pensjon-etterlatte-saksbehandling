package no.nav.etterlatte.grunnlagsendring

import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import no.nav.etterlatte.Context
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.SystemUser
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.domain.GrunnlagsendringsType
import no.nav.etterlatte.behandling.klienter.VedtakKlient
import no.nav.etterlatte.behandling.sakId1
import no.nav.etterlatte.common.DatabaseContext
import no.nav.etterlatte.foerstegangsbehandling
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.vedtak.LoependeYtelseDTO
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.mockedSakTilgangDao
import no.nav.security.token.support.core.context.TokenValidationContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.LocalDate

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GrunnlagsendringsHendelseFilterTest {
    private val vedtakklient = mockk<VedtakKlient>()
    private val behandlingService = mockk<BehandlingService>()
    private lateinit var service: GrunnlagsendringsHendelseFilter

    @BeforeEach
    fun setup() {
        coEvery { vedtakklient.sakHarLopendeVedtakPaaDato(any(), any(), any()) } returns LoependeYtelseDTO(true, false, LocalDate.now())
        every { behandlingService.hentBehandlingerForSak(any()) } returns emptyList()
        service = GrunnlagsendringsHendelseFilter(vedtakklient, behandlingService)
        val tokenValidationContext = mockk<TokenValidationContext>()
        val brukerTokenInfo = mockk<BrukerTokenInfo>()
        val systembruker =
            spyk(SystemUser(tokenValidationContext, brukerTokenInfo)).also {
                every { it.name() } returns
                    this::class.java.simpleName
            }
        val databasekontekst = mockk<DatabaseContext>()
        Kontekst.set(Context(systembruker, databasekontekst, mockedSakTilgangDao(), null))
    }

    @AfterEach
    fun afterEach() {
        clearAllMocks()
    }

    @ParameterizedTest
    @EnumSource(
        GrunnlagsendringsType::class,
        names = ["DOEDSFALL", "INSTITUSJONSOPPHOLD", "GRUNNBELOEP"],
        mode = EnumSource.Mode.EXCLUDE,
    )
    fun `Skal slippe gjennom relevante hendelser for sak med åpen behandling`(grunnlagendringType: GrunnlagsendringsType) {
        val sakId = 1L
        val foerstegangsbehandlinger =
            listOf(
                foerstegangsbehandling(sakId = sakId1, status = BehandlingStatus.VILKAARSVURDERT),
            )
        every { behandlingService.hentBehandlingerForSak(any()) } returns foerstegangsbehandlinger
        assertTrue(service.hendelseErRelevantForSak(sakId1, grunnlagendringType))
        verify(exactly = 1) { behandlingService.hentBehandlingerForSak(sakId1) }
        coVerify(exactly = 0) { vedtakklient.sakHarLopendeVedtakPaaDato(sakId1, any(), any()) }
    }

    @ParameterizedTest
    @EnumSource(
        GrunnlagsendringsType::class,
        names = ["DOEDSFALL", "INSTITUSJONSOPPHOLD", "GRUNNBELOEP"],
        mode = EnumSource.Mode.EXCLUDE,
    )
    fun `Skal slippe gjennom relevante hendelser for sak med løpende vedtak ytelse`(grunnlagendringType: GrunnlagsendringsType) {
        assertTrue(service.hendelseErRelevantForSak(sakId1, grunnlagendringType))
        verify(exactly = 1) { behandlingService.hentBehandlingerForSak(sakId1) }
        coVerify(exactly = 1) { vedtakklient.sakHarLopendeVedtakPaaDato(sakId1, any(), any()) }
    }

    @ParameterizedTest
    @EnumSource(
        GrunnlagsendringsType::class,
        names = ["DOEDSFALL", "INSTITUSJONSOPPHOLD", "GRUNNBELOEP"],
        mode = EnumSource.Mode.EXCLUDE,
    )
    fun `Skal ikke slippe gjennom gyldige hendelser hvis sak ikke har en åpen behandling eller løpende vedtak`(
        grunnlagendringType: GrunnlagsendringsType,
    ) {
        coEvery { vedtakklient.sakHarLopendeVedtakPaaDato(any(), any(), any()) } returns LoependeYtelseDTO(false, false, LocalDate.now())
        every { behandlingService.hentBehandlingerForSak(any()) } returns emptyList()
        assertFalse(service.hendelseErRelevantForSak(sakId1, grunnlagendringType))
        verify(exactly = 1) { behandlingService.hentBehandlingerForSak(sakId1) }
        coVerify(exactly = 1) { vedtakklient.sakHarLopendeVedtakPaaDato(sakId1, any(), any()) }
    }

    @ParameterizedTest
    @EnumSource(
        GrunnlagsendringsType::class,
        names = ["DOEDSFALL", "INSTITUSJONSOPPHOLD", "GRUNNBELOEP"],
        mode = EnumSource.Mode.EXCLUDE,
    )
    fun `Ikke løpende ytelse men ikke åpen behandling`(grunnlagendringType: GrunnlagsendringsType) {
        coEvery { vedtakklient.sakHarLopendeVedtakPaaDato(any(), any(), any()) } returns LoependeYtelseDTO(false, false, LocalDate.now())

        val foerstegangsbehandlinger =
            listOf(
                foerstegangsbehandling(sakId = sakId1, status = BehandlingStatus.AVBRUTT),
            )
        every { behandlingService.hentBehandlingerForSak(any()) } returns foerstegangsbehandlinger
        assertFalse(service.hendelseErRelevantForSak(sakId1, grunnlagendringType))
        verify(exactly = 1) { behandlingService.hentBehandlingerForSak(sakId1) }
        coVerify(exactly = 1) { vedtakklient.sakHarLopendeVedtakPaaDato(sakId1, any(), any()) }
    }
}

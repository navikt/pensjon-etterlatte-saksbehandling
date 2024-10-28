package no.nav.etterlatte.behandling.omregning

import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import no.nav.etterlatte.Context
import no.nav.etterlatte.DatabaseContextTest
import no.nav.etterlatte.Self
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.domain.Foerstegangsbehandling
import no.nav.etterlatte.kafka.TestProdusent
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.sak.KjoeringStatus
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.rapidsandrivers.HENDELSE_DATA_KEY
import no.nav.etterlatte.rapidsandrivers.OmregningData
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.util.UUID

internal class OmregningJobServiceTest {
    private val kontekst = Context(Self(this::class.java.simpleName), DatabaseContextTest(mockk()), mockk(), null)
    private val kafkaProdusent: TestProdusent<String, String> = spyk(TestProdusent())
    private lateinit var omregningDao: OmregningDao
    private lateinit var behandlingService: BehandlingService
    private lateinit var omregningJobService: OmregningJobService

    @BeforeEach
    fun before() {
        omregningDao =
            mockk {
                every { hentSakerTilOmregning(OmregningJobService.kjoering, any()) } returns
                    listOf(Pair(SAK_ID, KjoeringStatus.FEILA))
            }

        behandlingService =
            mockk {
                every { hentFoerstegangsbehandling(any()) } returns
                    foerstegangsbehandling()
                every { hentBehandlingerForSak(any()) } returns
                    listOf(
                        foerstegangsbehandling(),
                        foerstegangsbehandling(BehandlingStatus.AVBRUTT),
                    )
            }

        omregningJobService =
            OmregningJobService(
                behandlingService = behandlingService,
                omregningDao = omregningDao,
                kafkaProdusent = kafkaProdusent,
            )
    }

    @Test
    fun `skal hente en sak og og publisere hendelse på kafka`() {
        omregningJobService.setupKontekstAndRun(kontekst)

        verify { omregningDao.hentSakerTilOmregning(OmregningJobService.kjoering, 1) }
        verify { behandlingService.hentFoerstegangsbehandling(SAK_ID) }
        verify { kafkaProdusent.publiser(any(), any()) }

        kafkaProdusent.publiserteMeldinger.first().let {
            val hendelseData: OmregningData =
                objectMapper
                    .readTree(it.verdi)
                    .get(HENDELSE_DATA_KEY)
                    .toJson()
                    .let { str -> objectMapper.readValue(str) }

            with(hendelseData) {
                sakId shouldBe SAK_ID
                kjoering shouldBe OmregningJobService.kjoering
                revurderingaarsak shouldBe Revurderingaarsak.OMREGNING
                verifiserUtbetalingUendret shouldBe true
                hentFraDato() shouldBe LocalDate.of(2023, Month.NOVEMBER, 1)
            }
        }
    }

    @Test
    fun `skal hente en sak og og feile pga manglende iverksatt foerstegangsbehandling`() {
        every { behandlingService.hentFoerstegangsbehandling(any()) } returns foerstegangsbehandling(BehandlingStatus.OPPRETTET)

        assertThrows<InternfeilException> {
            omregningJobService.setupKontekstAndRun(kontekst)
        }

        verify { omregningDao.hentSakerTilOmregning(OmregningJobService.kjoering, 1) }
        verify { behandlingService.hentFoerstegangsbehandling(SAK_ID) }
        verify(exactly = 0) { kafkaProdusent.publiser(any(), any()) }
    }

    @Test
    fun `skal hente en sak og og feile pga annen behandling under arbeid`() {
        every { behandlingService.hentBehandlingerForSak(any()) } returns listOf(foerstegangsbehandling(BehandlingStatus.OPPRETTET))

        assertThrows<InternfeilException> {
            omregningJobService.setupKontekstAndRun(kontekst)
        }

        verify { omregningDao.hentSakerTilOmregning(OmregningJobService.kjoering, 1) }
        verify { behandlingService.hentFoerstegangsbehandling(SAK_ID) }
        verify(exactly = 0) { kafkaProdusent.publiser(any(), any()) }
    }

    private fun foerstegangsbehandling(s: BehandlingStatus = BehandlingStatus.IVERKSATT): Foerstegangsbehandling =
        mockk {
            every { id } returns UUID.randomUUID()
            every { virkningstidspunkt } returns
                mockk {
                    every { dato } returns YearMonth.of(2023, Month.NOVEMBER)
                }
            every { status } returns s
        }

    private companion object {
        val SAK_ID = SakId(1)
    }
}

package behandling.omregning

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import no.nav.etterlatte.ConnectionAutoclosingTest
import no.nav.etterlatte.DatabaseExtension
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.domain.Revurdering
import no.nav.etterlatte.behandling.omregning.OmregningDao
import no.nav.etterlatte.behandling.omregning.OmregningService
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.ktor.token.systembruker
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.Prosesstype
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.oppgave.OppgaveSaksbehandler
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.sak.KjoeringRequest
import no.nav.etterlatte.libs.common.sak.KjoeringStatus
import no.nav.etterlatte.libs.common.sak.LagreKjoeringRequest
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.database.toList
import no.nav.etterlatte.libs.ktor.token.Claims
import no.nav.etterlatte.libs.ktor.token.Systembruker
import no.nav.etterlatte.libs.testdata.grunnlag.SOEKER_FOEDSELSNUMMER
import no.nav.etterlatte.oppgave.OppgaveService
import no.nav.etterlatte.sak.SakSkrivDao
import no.nav.etterlatte.sak.SakendringerDao
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal
import java.util.UUID
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(DatabaseExtension::class)
class OmregningServiceTest(
    val dataSource: DataSource,
) {
    val behandlingService = mockk<BehandlingService>()
    val oppgaveService = mockk<OppgaveService>()

    @Test
    fun `lagrer kjoering med all relevant informasjon`() {
        val connection = ConnectionAutoclosingTest(dataSource)

        val sak =
            SakSkrivDao(
                SakendringerDao(connection) {
                    mockk()
                },
            ).opprettSak(SOEKER_FOEDSELSNUMMER.value, SakType.BARNEPENSJON, Enheter.STEINKJER.enhetNr)

        val service =
            OmregningService(
                behandlingService = mockk(),
                omregningDao = OmregningDao(connection),
                oppgaveService = mockk(),
            )

        val request =
            LagreKjoeringRequest(
                kjoering = "Regulering-2024",
                status = KjoeringStatus.FERDIGSTILT,
                sakId = sak.id,
                beregningBeloepFoer = BigDecimal("1000"),
                beregningBeloepEtter = BigDecimal("1500"),
                beregningGFoer = BigDecimal("10000"),
                beregningGEtter = BigDecimal("15000"),
                beregningBruktOmregningsfaktor = BigDecimal("1.5"),
                avkortingFoer = BigDecimal("1000"),
                avkortingEtter = BigDecimal("2000"),
                vedtakBeloep = BigDecimal("15000"),
            )

        service.kjoeringFullfoert(request)

        val lagraIDatabasen: LagreKjoeringRequest =
            connection.hentConnection {
                with(connection) {
                    val statement =
                        it.prepareStatement(
                            """
                            SELECT kjoering, status, sak_id, beregning_beloep_foer, 
                            beregning_beloep_etter, beregning_g_foer, beregning_g_etter, 
                            beregning_brukt_omregningsfaktor, avkorting_foer, avkorting_etter, vedtak_beloep 
                            FROM omregningskjoering WHERE sak_id=${sak.id}
                            """.trimIndent(),
                        )
                    statement
                        .executeQuery()
                        .toList {
                            LagreKjoeringRequest(
                                kjoering = getString("kjoering"),
                                status = KjoeringStatus.valueOf(getString("status")),
                                sakId = SakId(getLong("sak_id")),
                                beregningBeloepFoer = getBigDecimal("beregning_beloep_foer"),
                                beregningBeloepEtter = getBigDecimal("beregning_beloep_etter"),
                                beregningGFoer = getBigDecimal("beregning_g_foer"),
                                beregningGEtter = getBigDecimal("beregning_g_etter"),
                                beregningBruktOmregningsfaktor = getBigDecimal("beregning_brukt_omregningsfaktor"),
                                avkortingFoer = getBigDecimal("avkorting_foer"),
                                avkortingEtter = getBigDecimal("avkorting_etter"),
                                vedtakBeloep = getBigDecimal("vedtak_beloep"),
                            )
                        }.first()
                }
            }

        Assertions.assertEquals(request, lagraIDatabasen)
    }

    @Test
    fun `Oppdater omregning til FEILA skal avbryte behandling`() {
        val connection = ConnectionAutoclosingTest(dataSource)

        val service =
            OmregningService(
                behandlingService = behandlingService,
                omregningDao = OmregningDao(connection),
                oppgaveService = oppgaveService,
            )

        val bruker = systembruker("EY")

        val sak =
            SakSkrivDao(
                SakendringerDao(connection) {
                    mockk()
                },
            ).opprettSak(SOEKER_FOEDSELSNUMMER.value, SakType.BARNEPENSJON, Enheter.STEINKJER.enhetNr)

        val behandlingId = UUID.randomUUID()
        val behandling =
            mockk<Revurdering> {
                every { id } returns behandlingId
                every { revurderingsaarsak } returns Revurderingaarsak.OMREGNING
                every { status } returns BehandlingStatus.BEREGNET
            }

        every { behandlingService.hentAapenOmregning(sak.id) } returns behandling
        every { behandlingService.avbrytBehandling(behandlingId, bruker) } just runs

        val request =
            KjoeringRequest(
                kjoering = "yolo",
                status = KjoeringStatus.STARTA,
                sakId = sak.id,
            )
        service.oppdaterKjoering(request, bruker)

        request.copy(status = KjoeringStatus.FEILA).let {
            service.oppdaterKjoering(it, bruker)
        }

        verify {
            behandlingService.avbrytBehandling(behandlingId, bruker)
        }
    }

    @Test
    fun `Oppdater omregning til FEILA skal bytte til manuell hvis inntektsendring og fjerne EY fra oppgave`() {
        val connection = ConnectionAutoclosingTest(dataSource)

        val service =
            OmregningService(
                behandlingService = behandlingService,
                omregningDao = OmregningDao(connection),
                oppgaveService = oppgaveService,
            )

        val bruker = systembruker("EY")

        val sak =
            SakSkrivDao(
                SakendringerDao(connection) {
                    mockk()
                },
            ).opprettSak(SOEKER_FOEDSELSNUMMER.value, SakType.BARNEPENSJON, Enheter.STEINKJER.enhetNr)

        val behandlingId = UUID.randomUUID()
        val behandling =
            mockk<Revurdering> {
                every { id } returns behandlingId
                every { revurderingsaarsak } returns Revurderingaarsak.INNTEKTSENDRING
                every { status } returns BehandlingStatus.BEREGNET
            }

        every { behandlingService.hentAapenOmregning(sak.id) } returns behandling
        every { behandlingService.endreProsesstype(any(), any()) } just runs

        val oppgaveId = UUID.randomUUID()
        every { oppgaveService.hentOppgaverForReferanse(behandlingId.toString()) } returns
            listOf(
                mockk {
                    every { id } returns oppgaveId
                    every { type } returns OppgaveType.REVURDERING
                    every { saksbehandler } returns OppgaveSaksbehandler("", "EY")
                },
            )
        every { oppgaveService.fjernSaksbehandler(any()) } just runs

        val request =
            KjoeringRequest(
                kjoering = "yolo",
                status = KjoeringStatus.STARTA,
                sakId = sak.id,
            )
        service.oppdaterKjoering(request, bruker)

        request.copy(status = KjoeringStatus.FEILA).let {
            service.oppdaterKjoering(it, bruker)
        }

        verify {
            oppgaveService.fjernSaksbehandler(oppgaveId)
            behandlingService.endreProsesstype(behandlingId, Prosesstype.MANUELL)
        }
    }

    private fun systembruker(brukernavn: String): Systembruker = systembruker(mapOf(Claims.azp_name to brukernavn))
}

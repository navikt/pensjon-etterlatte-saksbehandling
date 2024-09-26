package behandling.omregning

import io.mockk.mockk
import no.nav.etterlatte.ConnectionAutoclosingTest
import no.nav.etterlatte.DatabaseExtension
import no.nav.etterlatte.behandling.omregning.OmregningDao
import no.nav.etterlatte.behandling.omregning.OmregningService
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.sak.KjoeringStatus
import no.nav.etterlatte.libs.common.sak.LagreKjoeringRequest
import no.nav.etterlatte.libs.database.toList
import no.nav.etterlatte.libs.testdata.grunnlag.SOEKER_FOEDSELSNUMMER
import no.nav.etterlatte.sak.SakSkrivDao
import no.nav.etterlatte.sak.SakendringerDao
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(DatabaseExtension::class)
class OmregningServiceTest(
    val dataSource: DataSource,
) {
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
                                sakId = getLong("sak_id"),
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
}

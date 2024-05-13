package no.nav.etterlatte.statistikk.database

import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.UtlandstilknytningType
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.libs.database.single
import no.nav.etterlatte.statistikk.domain.BehandlingMetode
import no.nav.etterlatte.statistikk.domain.MaanedStoenadRad
import no.nav.etterlatte.statistikk.domain.SakRad
import no.nav.etterlatte.statistikk.domain.SakUtland
import no.nav.etterlatte.statistikk.domain.SakYtelsesgruppe
import no.nav.etterlatte.statistikk.domain.SoeknadFormat
import no.nav.etterlatte.statistikk.domain.StoenadRad
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.RegisterExtension
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.util.UUID
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RyddUtlandstilsnittDaoTest(private val dataSource: DataSource) {
    companion object {
        @RegisterExtension
        val dbExtension = DatabaseExtension()
    }

    @AfterEach
    fun afterEach() {
        dbExtension.resetDb()
    }

    @Test
    fun `lagrePatchetStatus kan gjenbruke innsendt connection`() {
        val ryddeRepo = RyddUtlandstilsnittDao.using(dataSource)

        val behandlingId = UUID.randomUUID()
        val sakId = 1L

        val connection = dataSource.connection

        connection.use { connection ->
            val statement =
                connection.prepareStatement(
                    """
                    INSERT INTO utlandstilknytning_fiksing VALUES (?, ?, ?, ?, ?)
                    """.trimIndent(),
                )
            statement.setObject(1, behandlingId)
            statement.setLong(2, sakId)
            statement.setString(3, UtlandstilknytningType.UTLANDSTILSNITT.name)
            statement.setString(4, HentetStatus.HENTET.name)
            statement.setString(5, PatchStatus.IKKE_PATCHET.name)
            statement.executeUpdate()

            // Gjenbruker connection
            ryddeRepo.lagrePatchetStatus(behandlingId, 2, 1, connection)

            with(
                connection.prepareStatement(
                    """
                    SELECT behandling_id, patch_status, antall_stoenad_fix, antall_sak_fix FROM utlandstilknytning_fiksing
                    WHERE behandling_id = ? 
                    """.trimIndent(),
                ),
            ) {
                setObject(1, behandlingId)
                executeQuery()
                    .single {
                        val hentetBehandlingId = getObject("behandling_id", UUID::class.java)
                        val patcheStatus = enumValueOf<PatchStatus>(getString("patch_status"))
                        val antallSakFikset = getLong("antall_sak_fix")
                        val antallStoenadFikset = getLong("antall_stoenad_fix")
                        Assertions.assertEquals(behandlingId, hentetBehandlingId)
                        Assertions.assertEquals(PatchStatus.PATCHET, patcheStatus)
                        Assertions.assertEquals(1, antallSakFikset)
                        Assertions.assertEquals(2, antallStoenadFikset)
                    }
            }
        }
    }

    @Test
    fun `patchRaderForBehandling patcher rader i sak, stoenad og maanedstoenad`() {
        val behandlingId = UUID.randomUUID()
        val sakId = 1L

        val ryddeRepo = RyddUtlandstilsnittDao.using(dataSource)
        val sakRepository = SakRepository.using(dataSource)
        val stoenadRepository = StoenadRepository.using(dataSource)

        val sakRad = sakRad(behandlingId, sakId)
        sakRepository.lagreRad(sakRad)

        val stoenadRad = stoenadRad(behandlingId, sakId)
        stoenadRepository.lagreStoenadsrad(stoenadRad)

        val maanedStoenadRad = maanedStoenadRad(behandlingId, sakId, YearMonth.of(2024, Month.JANUARY))
        stoenadRepository.lagreMaanedStatistikkRad(maanedStoenadRad)

        dataSource.connection.use { connection ->
            val statement =
                connection.prepareStatement(
                    """
                    INSERT INTO utlandstilknytning_fiksing VALUES (?, ?, ?, ?, ?)
                    """.trimIndent(),
                )
            statement.setObject(1, behandlingId)
            statement.setLong(2, sakId)
            statement.setString(3, UtlandstilknytningType.BOSATT_UTLAND.name)
            statement.setString(4, HentetStatus.HENTET.name)
            statement.setString(5, PatchStatus.IKKE_PATCHET.name)
            statement.executeUpdate()
        }

        ryddeRepo.patchRaderForBehandling(behandlingId = behandlingId, statistikkUtlandstilknytning = SakUtland.BOSATT_UTLAND)

        dataSource.connection.use {
            val statement =
                it.prepareStatement(
                    """
                    SELECT patch_status, antall_sak_fix, antall_stoenad_fix 
                    FROM utlandstilknytning_fiksing WHERE behandling_id = ?
                    """.trimIndent(),
                )
            statement.setObject(1, behandlingId)
            val resultSet = statement.executeQuery()
            resultSet.single {
                val patchStatus = enumValueOf<PatchStatus>(getString("patch_status"))
                val antallSakFikset = getLong("antall_sak_fix")
                val antallStoenadFikset = getLong("antall_stoenad_fix")

                Assertions.assertEquals(PatchStatus.PATCHET, patchStatus)
                Assertions.assertEquals(1, antallSakFikset)
                Assertions.assertEquals(2, antallStoenadFikset)
            }
        }
    }
}

private fun stoenadRad(
    behandlingId: UUID,
    sakId: Long,
) = StoenadRad(
    id = 0,
    fnrSoeker = "",
    fnrForeldre = listOf(),
    fnrSoesken = listOf(),
    anvendtTrygdetid = "",
    nettoYtelse = null,
    beregningType = "",
    anvendtSats = "",
    behandlingId = behandlingId,
    sakId = sakId,
    sakNummer = 0,
    tekniskTid = Tidspunkt.now(),
    sakYtelse = "",
    versjon = "",
    saksbehandler = "",
    attestant = null,
    vedtakLoependeFom = LocalDate.of(2024, Month.JANUARY, 1),
    vedtakLoependeTom = null,
    beregning = null,
    avkorting = null,
    vedtakType = VedtakType.INNVILGELSE,
    sakUtland = SakUtland.NASJONAL,
    virkningstidspunkt = YearMonth.of(2024, Month.JANUARY),
    utbetalingsdato = null,
    kilde = Vedtaksloesning.GJENNY,
    pesysId = null,
)

private fun maanedStoenadRad(
    behandlingId: UUID,
    sakId: Long,
    stoenadMaaned: YearMonth,
) = MaanedStoenadRad(
    id = 0,
    fnrSoeker = "",
    fnrForeldre = listOf(),
    fnrSoesken = listOf(),
    anvendtTrygdetid = "",
    nettoYtelse = null,
    avkortingsbeloep = "",
    aarsinntekt = "",
    beregningType = "",
    anvendtSats = "",
    behandlingId = behandlingId,
    sakId = sakId,
    sakNummer = 0,
    tekniskTid = Tidspunkt.now(),
    sakYtelse = "",
    versjon = "",
    saksbehandler = "",
    attestant = null,
    vedtakLoependeFom = stoenadMaaned.atDay(1),
    vedtakLoependeTom = null,
    statistikkMaaned = stoenadMaaned,
    sakUtland = SakUtland.NASJONAL,
    virkningstidspunkt = YearMonth.of(2024, Month.JANUARY),
    utbetalingsdato = null,
    kilde = Vedtaksloesning.GJENOPPRETTA,
    pesysId = null,
)

private fun sakRad(
    behandlingId: UUID,
    sakId: Long,
): SakRad {
    return SakRad(
        id = 0,
        referanseId = behandlingId,
        sakId = sakId,
        mottattTidspunkt = Tidspunkt.now(),
        registrertTidspunkt = Tidspunkt.now(),
        ferdigbehandletTidspunkt = null,
        vedtakTidspunkt = null,
        type = "",
        status = null,
        resultat = null,
        resultatBegrunnelse = null,
        saksbehandler = null,
        ansvarligEnhet = null,
        soeknadFormat = SoeknadFormat.DIGITAL,
        sakUtland = SakUtland.NASJONAL,
        behandlingMetode = BehandlingMetode.TOTRINN,
        opprettetAv = null,
        ansvarligBeslutter = null,
        aktorId = "",
        datoFoersteUtbetaling = null,
        tekniskTid = Tidspunkt.now(),
        sakYtelse = "",
        sakYtelsesgruppe = SakYtelsesgruppe.EN_AVDOED_FORELDER,
        avdoedeForeldre = listOf(),
        revurderingAarsak = null,
        vedtakLoependeFom = null,
        vedtakLoependeTom = null,
        beregning = null,
        avkorting = null,
        kilde = Vedtaksloesning.GJENNY,
        pesysId = null,
        relatertTil = null,
    )
}

package no.nav.etterlatte.statistikk.database

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.database.toList
import no.nav.etterlatte.statistikk.domain.StoenadUtbetalingsperiode
import java.sql.Date
import java.time.YearMonth
import java.util.UUID
import javax.sql.DataSource

class OppdaterVedtakRepo(
    private val datasource: DataSource,
) {
    fun vedtakSomIkkeErHentet(limit: Int): List<UUID> =
        datasource.connection.use { connection ->
            val statement =
                connection.prepareStatement(
                    """
                    SELECT behandling_id FROM oppdaterte_vedtak_opphoer_fom_perioder 
                    WHERE hentet_status = 'IKKE_HENTET' LIMIT ?;
                    """.trimIndent(),
                )
            statement.setInt(1, limit)
            statement
                .executeQuery()
                .toList { getObject("behandling_id") as UUID }
        }

    fun oppdaterHentetVedtak(
        behandlingId: UUID,
        vedtakId: Int,
        opphoerFom: YearMonth?,
        perioder: List<StoenadUtbetalingsperiode>,
    ) {
        datasource.connection.use { connection ->
            val statement =
                connection.prepareStatement(
                    """
                    UPDATE oppdaterte_vedtak_opphoer_fom_perioder SET 
                    (vedtak_id, hentet_status, opphoer_fom, vedtaksperioder) = (?, ?, ?, ?) 
                    where behandling_id = ? 
                    """.trimIndent(),
                )
            statement.setInt(1, vedtakId)
            statement.setString(2, HentetStatus.HENTET.name)
            statement.setDate(3, opphoerFom?.atDay(1)?.let { Date.valueOf(it) })
            statement.setJsonb(4, perioder)
            statement.setObject(5, behandlingId)
        }
    }

    fun oppdaterIkkeFunnetVedtak(behandlingId: UUID) {
        datasource.connection.use { connection ->
            val statement =
                connection.prepareStatement(
                    """
                    UPDATE oppdaterte_vedtak_opphoer_fom_perioder SET hentet_status = 'IKKE_FUNNET'
                    WHERE behandling_id = ? and hentet_status = 'IKKE_HENTET'
                    """.trimIndent(),
                )
            statement.setObject(1, behandlingId)
        }
    }

    fun vedtakSomIkkeErPatchet(limit: Int): List<OppdatertVedtak> =
        datasource.connection.use { connection ->
            val statement =
                connection.prepareStatement(
                    """
                    SELECT behandling_id, vedtak_id, opphoer_fom, vedtaksperioder 
                    FROM oppdaterte_vedtak_opphoer_fom_perioder 
                    WHERE hentet_status = 'HENTET' and patchet_status = 'IKKE_PATCHET'
                    LIMIT ?
                    """.trimIndent(),
                )
            statement.setInt(1, limit)
            statement.executeQuery().toList {
                OppdatertVedtak(
                    behandlingId = getObject("behandling_id") as UUID,
                    vedtakId = getInt("vedtak_id"),
                    opphoerFom =
                        getDate("opphoerFom")?.toLocalDate()?.let {
                            YearMonth.of(it.year, it.monthValue)
                        },
                    perioder =
                        checkNotNull(
                            getString("vedtaksperioder")?.let { objectMapper.readValue(it) },
                        ),
                )
            }
        }

    fun patchVedtak(oppdatertVedtak: OppdatertVedtak) {
        datasource.connection.use { connection ->
            connection.autoCommit = false

            val oppdatereStoenad =
                connection.prepareStatement(
                    """
                    UPDATE stoenad SET (opphoerFom, vedtaksperioder) = (?, ?)
                    WHERE behandlingid = ?
                    """.trimIndent(),
                )
            oppdatereStoenad.setDate(1, oppdatertVedtak.opphoerFom?.let { Date.valueOf(it.atDay(1)) })
            oppdatereStoenad.setJsonb(2, oppdatertVedtak.perioder)
            oppdatereStoenad.setObject(3, oppdatertVedtak.behandlingId)
            check(oppdatereStoenad.executeUpdate() == 1) {
                "Vi oppdaterte et uventet antall vedtak for behandlingId = ${oppdatertVedtak.behandlingId}"
            }

            val lagrePatchStatus =
                connection.prepareStatement(
                    """
                    UPDATE oppdaterte_vedtak_opphoer_fom_perioder SET patchet_status = 'PATCHET'
                    WHERE behandling_id = ? and patchet_status = 'IKKE_PATCHET'
                    """.trimIndent(),
                )
            lagrePatchStatus.setObject(1, oppdatertVedtak.behandlingId)
            check(lagrePatchStatus.executeUpdate() == 1) {
                "Vi oppdaterte patchstatus på uventet antall rader for behandlingId=${oppdatertVedtak.behandlingId}"
            }

            connection.commit()
        }
    }
}

data class OppdatertVedtak(
    val behandlingId: UUID,
    val vedtakId: Int,
    val opphoerFom: YearMonth?,
    val perioder: List<StoenadUtbetalingsperiode>,
)

enum class HentetStatus {
    IKKE_HENTET,
    HENTET,
    IKKE_FUNNET,
}

enum class PatchetStatus {
    IKKE_PATCHET,
    PATCHET,
}

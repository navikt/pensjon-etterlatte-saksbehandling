package no.nav.etterlatte.statistikk.database

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.libs.common.behandling.etteroppgjoer.EtteroppgjoerForbehandlingDto
import no.nav.etterlatte.libs.common.behandling.etteroppgjoer.EtteroppgjoerForbehandlingStatus
import no.nav.etterlatte.libs.common.behandling.etteroppgjoer.EtteroppgjoerHendelseType
import no.nav.etterlatte.libs.common.beregning.BeregnetEtteroppgjoerResultatDto
import no.nav.etterlatte.libs.common.beregning.EtteroppgjoerResultatType
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeFunnetException
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.periode.Periode
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.getTidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.setTidspunkt
import no.nav.etterlatte.libs.database.setNullableDate
import no.nav.etterlatte.libs.database.setNullableDouble
import no.nav.etterlatte.libs.database.setNullableInt
import no.nav.etterlatte.libs.database.setNullableLong
import no.nav.etterlatte.libs.database.singleOrNull
import no.nav.etterlatte.libs.database.toList
import java.sql.ResultSet
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.util.UUID
import javax.sql.DataSource

class EtteroppgjoerRepository(
    private val dataSource: DataSource,
) {
    fun lagreEtteroppgjoerRad(rad: EtteroppgjoerRad) {
        dataSource.connection.use { connection ->
            val statement =
                connection.prepareStatement(
                    """
                    INSERT INTO etteroppgjoer_statistikk (forbehandling_id, sak_id, aar, hendelse, forbehandling_status, 
                    opprettet, maaneder_ytelse, teknisk_tid, utbetalt_stoenad, ny_brutto_stoenad, differanse, 
                    rettsgebyr, rettsgebyr_gyldig_fra, tilbakekreving_grense, etterbetaling_grense, resultat_type)
                    values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """.trimIndent(),
                )
            statement.setObject(1, rad.forbehandlingId)
            statement.setLong(2, rad.sakId.sakId)
            statement.setInt(3, rad.aar)
            statement.setString(4, rad.hendelse.name)
            statement.setString(5, rad.forbehandlingStatus.name)
            statement.setTidspunkt(6, rad.opprettet)
            statement.setJsonb(7, rad.maanederYtelse)
            statement.setTidspunkt(8, rad.tekniskTid)
            statement.setNullableLong(9, rad.utbetaltStoenad)
            statement.setNullableLong(10, rad.nyBruttoStoenad)
            statement.setNullableLong(11, rad.differanse)
            statement.setNullableInt(12, rad.rettsgebyr)
            statement.setNullableDate(13, rad.rettsgebyrGyldigFra)
            statement.setNullableDouble(14, rad.tilbakekrevingGrense)
            statement.setNullableDouble(15, rad.etterbetalingGrense)
            statement.setString(16, rad.resultatType?.name)
            statement.executeUpdate()
        }
    }

    fun hentEtteroppgjoerRad(radId: Long): EtteroppgjoerRad =
        dataSource.connection.use { connection ->
            val statement =
                connection.prepareStatement(
                    """
                    SELECT id, forbehandling_id, sak_id, aar, hendelse, forbehandling_status, 
                      opprettet, maaneder_ytelse, teknisk_tid, utbetalt_stoenad, ny_brutto_stoenad, differanse, 
                      rettsgebyr, rettsgebyr_gyldig_fra, tilbakekreving_grense, etterbetaling_grense, resultat_type
                    FROM etteroppgjoer_statistikk
                    WHERE id = ?
                    """.trimIndent(),
                )
            statement.setLong(1, radId)
            statement.executeQuery().singleOrNull {
                tilEtteroppgjoerRad()
            } ?: throw IkkeFunnetException("FANT_IKKE_ETTEROPPGJOER", "Fant ikke forespurt rad for etteroppgjøret")
        }

    fun hentEtteroppgjoerRaderForForbehandling(forbehandlingId: UUID): List<EtteroppgjoerRad> =
        dataSource.connection.use { connection ->
            val statement =
                connection.prepareStatement(
                    """
                    SELECT id, forbehandling_id, sak_id, aar, hendelse, forbehandling_status, 
                      opprettet, maaneder_ytelse, teknisk_tid, utbetalt_stoenad, ny_brutto_stoenad, differanse, 
                      rettsgebyr, rettsgebyr_gyldig_fra, tilbakekreving_grense, etterbetaling_grense, resultat_type
                    FROM etteroppgjoer_statistikk
                    WHERE forbehandling_id = ?
                    """.trimIndent(),
                )
            statement.setObject(1, forbehandlingId)
            statement.executeQuery().toList { tilEtteroppgjoerRad() }
        }
}

private fun ResultSet.tilEtteroppgjoerRad(): EtteroppgjoerRad =
    EtteroppgjoerRad(
        id = getLong("id"),
        forbehandlingId = getObject("forbehandling_id") as UUID,
        sakId = SakId(getLong("sak_id")),
        aar = getInt("aar"),
        hendelse = enumValueOf<EtteroppgjoerHendelseType>(getString("hendelse")),
        forbehandlingStatus = enumValueOf(getString("forbehandling_status")),
        opprettet = getTidspunkt("opprettet"),
        maanederYtelse = objectMapper.readValue<List<Int>>(getString("maaneder_ytelse")),
        tekniskTid = getTidspunkt("teknisk_tid"),
        utbetaltStoenad = getLong("utbetalt_stoenad").takeUnless { wasNull() },
        nyBruttoStoenad = getLong("ny_brutto_stoenad").takeUnless { wasNull() },
        differanse = getLong("differanse").takeUnless { wasNull() },
        rettsgebyr = getInt("rettsgebyr").takeUnless { wasNull() },
        rettsgebyrGyldigFra = getDate("rettsgebyr_gyldig_fra").takeUnless { wasNull() }?.toLocalDate(),
        tilbakekrevingGrense = getDouble("tilbakekreving_grense").takeUnless { wasNull() },
        etterbetalingGrense = getDouble("etterbetaling_grense").takeUnless { wasNull() },
        resultatType =
            getString("resultat_type")
                .takeUnless { wasNull() }
                ?.let { enumValueOf<EtteroppgjoerResultatType>(it) },
    )

data class EtteroppgjoerRad(
    val id: Long,
    val forbehandlingId: UUID,
    val sakId: SakId,
    val aar: Int,
    val hendelse: EtteroppgjoerHendelseType,
    val forbehandlingStatus: EtteroppgjoerForbehandlingStatus,
    val opprettet: Tidspunkt,
    val maanederYtelse: List<Int>,
    val tekniskTid: Tidspunkt,
    // Følgende rader går på resultat, som ikke nødvendigvis er gitt
    val utbetaltStoenad: Long?,
    val nyBruttoStoenad: Long?,
    val differanse: Long?,
    val rettsgebyr: Int?,
    val rettsgebyrGyldigFra: LocalDate?,
    val tilbakekrevingGrense: Double?,
    val etterbetalingGrense: Double?,
    val resultatType: EtteroppgjoerResultatType?,
) {
    companion object {
        fun fraHendelseOgDto(
            hendelse: EtteroppgjoerHendelseType,
            forbehandlingDto: EtteroppgjoerForbehandlingDto,
            tekniskTid: Tidspunkt,
            resultat: BeregnetEtteroppgjoerResultatDto?,
        ): EtteroppgjoerRad =
            EtteroppgjoerRad(
                id = -1,
                forbehandlingId = forbehandlingDto.id,
                sakId = forbehandlingDto.sak.id,
                aar = forbehandlingDto.aar,
                hendelse = hendelse,
                forbehandlingStatus = forbehandlingDto.status,
                opprettet = forbehandlingDto.opprettet,
                maanederYtelse = finnMaanederMedYtelseIEtteroppgjoersaar(forbehandlingDto.innvilgetPeriode),
                tekniskTid = tekniskTid,
                utbetaltStoenad = resultat?.utbetaltStoenad,
                nyBruttoStoenad = resultat?.nyBruttoStoenad,
                differanse = resultat?.differanse,
                rettsgebyr = resultat?.grense?.rettsgebyr,
                rettsgebyrGyldigFra = resultat?.grense?.rettsgebyrGyldigFra,
                tilbakekrevingGrense = resultat?.grense?.tilbakekreving,
                etterbetalingGrense = resultat?.grense?.etterbetaling,
                resultatType = resultat?.resultatType,
            )
    }
}

private fun finnMaanederMedYtelseIEtteroppgjoersaar(periode: Periode): List<Int> {
    // implisitt hele året med ingen tom
    val tom = periode.tom ?: YearMonth.of(periode.fom.year, Month.DECEMBER)
    return (periode.fom.month.value..tom.month.value).toList()
}

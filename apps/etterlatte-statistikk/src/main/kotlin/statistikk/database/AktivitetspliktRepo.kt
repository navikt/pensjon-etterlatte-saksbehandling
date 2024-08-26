package no.nav.etterlatte.statistikk.database

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.libs.common.aktivitetsplikt.AktivitetDto
import no.nav.etterlatte.libs.common.aktivitetsplikt.AktivitetType
import no.nav.etterlatte.libs.common.aktivitetsplikt.AktivitetspliktAktivitetsgradDto
import no.nav.etterlatte.libs.common.aktivitetsplikt.AktivitetspliktDto
import no.nav.etterlatte.libs.common.aktivitetsplikt.UnntakFraAktivitetDto
import no.nav.etterlatte.libs.common.aktivitetsplikt.UnntakFraAktivitetsplikt
import no.nav.etterlatte.libs.common.aktivitetsplikt.VurdertAktivitetsgrad
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.getTidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.setTidspunkt
import no.nav.etterlatte.libs.database.singleOrNull
import no.nav.etterlatte.libs.database.toList
import java.sql.ResultSet
import java.time.LocalDate
import java.time.YearMonth
import javax.sql.DataSource

class AktivitetspliktRepo(
    private val datasource: DataSource,
) {
    // Vi trenger ikke lagre flere enn den nyeste vurderingen per registrert måned, siden de ikke vil påvirke
    // statistikken
    fun lagreAktivitetspliktForSak(
        aktivitetspliktDto: AktivitetspliktDto,
        overstyrtRegistret: Tidspunkt? = null,
    ): StatistikkAktivitet {
        val statisikkAktivitet = StatistikkAktivitet.fra(aktivitetspliktDto, overstyrtRegistret)
        datasource.connection.use { connection ->
            val statement =
                connection.prepareStatement(
                    """
                    INSERT INTO aktivitetsplikt (
                        sak_id, registrert, avdoed_doedsmaaned, unntak, brukers_aktivitet, 
                        aktivitetsgrad, varig_unntak, registrert_maaned
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)  
                    ON CONFLICT (sak_id, registrert_maaned) DO UPDATE 
                    SET registrert = EXCLUDED.registrert, avdoed_doedsmaaned = EXCLUDED.avdoed_doedsmaaned,
                        unntak = EXCLUDED.unntak, brukers_aktivitet = EXCLUDED.brukers_aktivitet,
                        aktivitetsgrad = EXCLUDED.aktivitetsgrad, varig_unntak = EXCLUDED.varig_unntak; 
                    """.trimIndent(),
                )
            statement.setLong(1, statisikkAktivitet.sakId)
            statement.setTidspunkt(2, statisikkAktivitet.registrert)
            statement.setString(3, statisikkAktivitet.avdoedDoedsmaaned.toString())
            statement.setJsonb(4, statisikkAktivitet.unntak)
            statement.setJsonb(5, statisikkAktivitet.brukersAktivitet)
            statement.setJsonb(6, statisikkAktivitet.aktivitetsgrad)
            statement.setBoolean(7, statisikkAktivitet.harVarigUnntak)
            statement.setString(8, YearMonth.from(statisikkAktivitet.registrert.toNorskLocalDate()).toString())
            statement.executeUpdate()
        }
        return statisikkAktivitet
    }

    /**
     * Henter siste kjente (om noen) aktivitetspliktvurdering for sak innenfor måneden.
     *
     * Den sjekker kun til og med angitt måned for å sikre stabilitet i statistikken som blir produsert
     */
    fun hentAktivitetspliktForMaaned(
        sakId: SakId,
        yearMonth: YearMonth,
    ): StatistikkAktivitet? =
        datasource.connection.use { connection ->
            val statement =
                connection.prepareStatement(
                    """
                    SELECT DISTINCT ON (sak_id) sak_id, registrert, avdoed_doedsmaaned, unntak, brukers_aktivitet, 
                        aktivitetsgrad, varig_unntak
                    FROM aktivitetsplikt
                    WHERE sak_id = ?
                    AND registrert_maaned <= ?
                    ORDER BY sak_id, registrert_maaned DESC
                    """.trimIndent(),
                )
            statement.setLong(1, sakId)
            statement.setString(2, yearMonth.toString())
            statement.executeQuery().singleOrNull {
                somStatistikkAktivititet()
            }
        }

    fun hentAktivitetspliktForMaaneder(
        sakIder: List<Long>,
        yearMonth: YearMonth,
    ): List<StatistikkAktivitet> =
        datasource.connection.use { connection ->
            val statement =
                connection.prepareStatement(
                    """
                    SELECT DISTINCT ON (sak_id) sak_id, registrert, avdoed_doedsmaaned, unntak, brukers_aktivitet, 
                        aktivitetsgrad, varig_unntak
                    FROM aktivitetsplikt
                    WHERE sak_id = ANY(?)
                    AND registrert_maaned <= ?
                    ORDER BY sak_id, registrert_maaned DESC
                    """.trimIndent(),
                )
            statement.setArray(1, connection.createArrayOf("bigint", sakIder.toTypedArray()))
            statement.setString(2, yearMonth.toString())
            statement
                .executeQuery()
                .toList { somStatistikkAktivititet() }
        }

    private fun ResultSet.somStatistikkAktivititet(): StatistikkAktivitet =
        StatistikkAktivitet(
            sakId = getLong("sak_id"),
            registrert = getTidspunkt("registrert"),
            avdoedDoedsmaaned = getString("avdoed_doedsmaaned").let { YearMonth.parse(it) },
            unntak = getString("unntak").let { objectMapper.readValue(it) },
            brukersAktivitet = getString("brukers_aktivitet").let { objectMapper.readValue(it) },
            aktivitetsgrad = getString("aktivitetsgrad").let { objectMapper.readValue(it) },
            harVarigUnntak = getBoolean("varig_unntak"),
        )
}

data class StatistikkAktivitet(
    val sakId: SakId,
    val registrert: Tidspunkt,
    val avdoedDoedsmaaned: YearMonth,
    val unntak: List<PeriodisertAktivitetspliktopplysning>,
    val brukersAktivitet: List<PeriodisertAktivitetspliktopplysning>,
    val aktivitetsgrad: List<AktivitetsgradPeriode>,
    val harVarigUnntak: Boolean,
) {
    companion object {
        fun fra(
            dto: AktivitetspliktDto,
            overstyrtRegistret: Tidspunkt?,
        ): StatistikkAktivitet =
            StatistikkAktivitet(
                sakId = dto.sakId,
                registrert = overstyrtRegistret ?: Tidspunkt.now(),
                avdoedDoedsmaaned = dto.avdoedDoedsmaaned,
                unntak = dto.unntak.map { PeriodisertAktivitetspliktopplysning.fra(it) },
                brukersAktivitet = dto.brukersAktivitet.map { PeriodisertAktivitetspliktopplysning.fra(it) },
                aktivitetsgrad = dto.aktivitetsgrad.map { AktivitetsgradPeriode.fra(it) },
                harVarigUnntak = dto.unntak.any { it.unntak == UnntakFraAktivitetsplikt.FOEDT_1963_ELLER_TIDLIGERE_OG_LAV_INNTEKT },
            )
    }
}

interface AktivitetspliktPeriode {
    val fom: LocalDate?
    val tom: LocalDate?

    fun erInnenforMaaned(maaned: YearMonth): Boolean {
        val erEtterFom = (fom?.let { YearMonth.from(it) } ?: maaned) <= maaned
        val erEtterTom = (tom?.let { YearMonth.from(it) } ?: maaned) >= maaned
        return erEtterFom && erEtterTom
    }
}

data class AktivitetsgradPeriode(
    override val fom: LocalDate?,
    override val tom: LocalDate?,
    val vurdering: VurdertAktivitet,
) : AktivitetspliktPeriode {
    companion object {
        fun fra(dto: AktivitetspliktAktivitetsgradDto): AktivitetsgradPeriode =
            AktivitetsgradPeriode(
                fom = dto.fom,
                tom = dto.tom,
                vurdering =
                    when (dto.vurdering) {
                        VurdertAktivitetsgrad.AKTIVITET_UNDER_50 -> VurdertAktivitet.UNDER_50_PROSENT
                        VurdertAktivitetsgrad.AKTIVITET_OVER_50 -> VurdertAktivitet.OVER_50_PROSENT
                        VurdertAktivitetsgrad.AKTIVITET_100 -> VurdertAktivitet.HUNDRE_PROSENT
                    },
            )
    }
}

enum class VurdertAktivitet {
    OVER_50_PROSENT,
    UNDER_50_PROSENT,
    HUNDRE_PROSENT,
}

/**
 * Mapper enumverdier til konstante strenger. Gjør at en eventuell endring / migrering av enums ikke
 * påvirker mappingen til statistikk.
 */
data class PeriodisertAktivitetspliktopplysning(
    val opplysning: String,
    override val fom: LocalDate?,
    override val tom: LocalDate?,
) : AktivitetspliktPeriode {
    companion object {
        fun fra(opplysning: UnntakFraAktivitetDto): PeriodisertAktivitetspliktopplysning =
            PeriodisertAktivitetspliktopplysning(
                opplysning =
                    when (opplysning.unntak) {
                        UnntakFraAktivitetsplikt.OMSORG_BARN_UNDER_ETT_AAR -> "OMSORG_BARN_UNDER_ETT_AAR"
                        UnntakFraAktivitetsplikt.OMSORG_BARN_SYKDOM -> "OMSORG_BARN_SYKDOM"
                        UnntakFraAktivitetsplikt.MANGLENDE_TILSYNSORDNING_SYKDOM -> "MANGLENDE_TILSYNSORDNING_SYKDOM"
                        UnntakFraAktivitetsplikt.SYKDOM_ELLER_REDUSERT_ARBEIDSEVNE -> "SYKDOM_ELLER_REDUSERT_ARBEIDSEVNE"
                        UnntakFraAktivitetsplikt.GRADERT_UFOERETRYGD -> "GRADERT_UFOERETRYGD"
                        UnntakFraAktivitetsplikt.MIDLERTIDIG_SYKDOM -> "MIDLERTIDIG_SYKDOM"
                        UnntakFraAktivitetsplikt.FOEDT_1963_ELLER_TIDLIGERE_OG_LAV_INNTEKT -> "FOEDT_1963_ELLER_TIDLIGERE_OG_LAV_INNTEKT"
                    },
                fom = opplysning.fom,
                tom = opplysning.tom,
            )

        fun fra(opplysning: AktivitetDto): PeriodisertAktivitetspliktopplysning =
            PeriodisertAktivitetspliktopplysning(
                opplysning =
                    when (opplysning.typeAktivitet) {
                        AktivitetType.ARBEIDSTAKER -> "ARBEIDSTAKER"
                        AktivitetType.SELVSTENDIG_NAERINGSDRIVENDE -> "SELVSTENDIG_NAERINGSDRIVENDE"
                        AktivitetType.ETABLERER_VIRKSOMHET -> "ETABLERER_VIRKSOMHET"
                        AktivitetType.ARBEIDSSOEKER -> "ARBEIDSSOEKER"
                        AktivitetType.UTDANNING -> "UTDANNING"
                    },
                fom = opplysning.fom,
                tom = opplysning.tom,
            )
    }
}

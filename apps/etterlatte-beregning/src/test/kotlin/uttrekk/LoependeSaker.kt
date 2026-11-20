package no.nav.etterlatte.uttrekk

import org.postgresql.ds.PGSimpleDataSource
import javax.sql.DataSource

class LoependeSaker(
    private val beregning: DataSource,
    private val behandling: DataSource,
) {
    fun hentSaker(
        beregningSql: String,
        behandlingSql: String,
    ): Map<Long, Boolean> {
        val kandidater = hentSakIder(beregning, beregningSql)
        if (kandidater.isEmpty()) return emptyMap()

        val loepende = hentSakIder(behandling, behandlingSql, kandidater)
        check(kandidater.containsAll(loepende)) { "Behandling returnerte saks-ID-er som ikke kom fra beregning." }
        return kandidater.sorted().associateWith { it in loepende }
    }

    private fun hentSakIder(
        database: DataSource,
        sql: String,
        kandidater: Set<Long>? = null,
    ): Set<Long> =
        database.connection.use { connection ->
            connection.isReadOnly = true
            connection.autoCommit = false
            connection.prepareStatement(sql).use { stmt ->
                stmt.queryTimeout = 120
                stmt.fetchSize = 1000
                val parameter = kandidater?.let { connection.createArrayOf("bigint", it.toTypedArray()) }
                try {
                    if (parameter != null) stmt.setArray(1, parameter)
                    stmt.executeQuery().use { rader ->
                        buildSet {
                            while (rader.next()) {
                                val id = rader.getLong("sak_id")
                                check(!rader.wasNull()) { "Spørringen returnerte NULL i sak_id." }
                                add(id)
                            }
                        }
                    }
                } finally {
                    parameter?.free()
                }
            }
        }
}

private val medSanksjon = """select distinct s.sak_id
from sanksjon s
left outer join avkorting_aarsoppgjoer a on a.behandling_id = s.behandling_id and a.aar = 2027
where s.fom > date '2026-01-01'
and a.id is null
limit 100
"""

private val utenSanksjon = """
    select distinct bp.sakid
    from beregningsperiode bp
         left outer join sanksjon s on s.sak_id = bp.sakid
         left outer join avkorting_aarsoppgjoer a on a.behandling_id = bp.behandlingid and a.aar = 2027
where bp.datofom >= '2026-05-01'
  and a.id is null
and s.id is null
limit 100
"""

private val opphoerINesteAar = """
  select sakid, vedtak.saktype from
    vedtak where datovirkfom > '2027-01-01'
and type = 'OPPHOER' 
"""

private val medOverstyrtBeregning =
    """
    select DISTINCT sakid, behandlingid, datofom, aarsak, type, beregnetdato
    from overstyr_beregningsgrunnlag og
    join beregningsperiode bp on bp.behandlingid = og.behandlings_id
    where bp.type = 'OMS'
    """.trimIndent()

private val loepende = """with RECURSIVE gjeldende_vedtak AS (
    select DISTINCT ON (sakid) sakid,
                               id,
                               type,
                               datovirkfom,
                               opphoer_fom
    FROM vedtak v
    WHERE vedtakstatus = 'IVERKSATT'
      AND type IN ('INNVILGELSE', 'OPPHOER', 'AVSLAG', 'ENDRING', 'INGEN_ENDRING')
      AND saktype = 'OMSTILLINGSSTOENAD'
    ORDER BY sakid, datoattestert DESC, datovirkfom desc, id desc
),
    loepende as (SELECT dISTINCT gjeldende_vedtak.sakid sak_id
                 FROM gjeldende_vedtak
                 WHERE gjeldende_vedtak.type IN ('INNVILGELSE', 'ENDRING', 'INGEN_ENDRING')
                   AND (opphoer_fom IS NULL OR opphoer_fom > '2027-01-01'))
select sak_id from loepende
where sak_id = ANY(?);
"""

private val tidligereFamiliepleier =
    """
    
    """.trimIndent()

/**
 * * Saker som har sanksjon
 * * Overstyrt beregning
 * * Er ingen endring i utbetaling
 */
fun main(args: Array<String>) {
    require(args.size == 3) { "Oppgi brukernavn for db-kobling, port til behandling-DB og port til beregning-DB" }
    val brukernavn = args[0]
    val uttrekk =
        LoependeSaker(
            beregning = lokalDatabase(args[2].toInt(), "beregning", brukernavn),
            behandling = lokalDatabase(args[1].toInt(), "sakogbehandlinger", brukernavn),
        )
    println("Løpende med sanksjon")
    uttrekk
        .hentSaker(medSanksjon, loepende)
        .entries
        .filter { (_, erLoepende) -> erLoepende }
        .take(10)
        .forEach { (id, _) -> println(id) }

    println("Løpende uten sanksjon")
    uttrekk
        .hentSaker(utenSanksjon, loepende)
        .entries
        .filter { (_, erLoepende) -> erLoepende }
        .take(10)
        .forEach { (id, _) -> println(id) }
}

private fun lokalDatabase(
    port: Int,
    databasenavn: String,
    brukernavn: String,
): DataSource =
    PGSimpleDataSource().apply {
        require(port in 1..65535) { "Ugyldig port for $databasenavn" }
        serverNames = arrayOf("127.0.0.1")
        portNumbers = intArrayOf(port)
        databaseName = databasenavn
        user = brukernavn
        password = ""
    }

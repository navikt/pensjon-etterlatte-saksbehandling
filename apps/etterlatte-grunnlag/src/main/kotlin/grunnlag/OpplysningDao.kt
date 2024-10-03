package no.nav.etterlatte.grunnlag

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.periode.Periode
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.database.firstOrNull
import no.nav.etterlatte.libs.database.singleOrNull
import no.nav.etterlatte.libs.database.toList
import java.sql.ResultSet
import java.sql.Types.VARCHAR
import java.time.YearMonth
import java.util.UUID
import javax.sql.DataSource

class OpplysningDao(
    private val datasource: DataSource,
) {
    private val connection get() = datasource.connection

    data class GrunnlagHendelse(
        val opplysning: Grunnlagsopplysning<JsonNode>,
        val sakId: SakId,
        val hendelseNummer: Long,
    )

    private fun ResultSet.asBehandlingOpplysning(): Grunnlagsopplysning<JsonNode> =
        Grunnlagsopplysning(
            id = getObject("opplysning_id") as UUID,
            kilde = objectMapper.readValue(getString("kilde")),
            opplysningType = Opplysningstype.valueOf(getString("opplysning_type")),
            meta = objectMapper.createObjectNode(),
            opplysning = getString("opplysning").deSerialize()!!,
            fnr = getString("fnr")?.let { Folkeregisteridentifikator.of(it) },
            periode =
                getString("fom")?.let { fom ->
                    Periode(
                        fom = YearMonth.parse(fom),
                        tom = getString("tom")?.let { tom -> YearMonth.parse(tom) },
                    )
                },
        )

    private fun ResultSet.asGrunnlagshendelse(): GrunnlagHendelse =
        GrunnlagHendelse(
            opplysning = asBehandlingOpplysning(),
            sakId = getLong("sak_id"),
            hendelseNummer = getLong("hendelsenummer"),
        )

    fun hentAlleGrunnlagForSak(sakId: SakId): List<GrunnlagHendelse> =
        connection.use {
            it
                .prepareStatement(
                    """
                    SELECT sak_id, opplysning_id, kilde, opplysning_type, opplysning, hendelsenummer, fnr, fom, tom
                    FROM grunnlagshendelse hendelse 
                    WHERE hendelse.sak_id = ?
                    """.trimIndent(),
                ).apply {
                    setLong(1, sakId)
                }.executeQuery()
                .toList { asGrunnlagshendelse() }
        }

    fun hentAlleGrunnlagForBehandling(behandlingId: UUID): List<GrunnlagHendelse> =
        connection.use {
            it
                .prepareStatement(
                    """
                    SELECT bv.sak_id, opplysning_id, kilde, opplysning_type, opplysning, hendelse.hendelsenummer, fnr, fom, tom
                    FROM grunnlagshendelse hendelse 
                    LEFT JOIN behandling_versjon bv 
                        ON bv.sak_id = hendelse.sak_id AND bv.hendelsenummer >= hendelse.hendelsenummer 
                    WHERE bv.behandling_id = ?
                    """.trimIndent(),
                ).apply {
                    setObject(1, behandlingId)
                }.executeQuery()
                .toList { asGrunnlagshendelse() }
        }

    fun hentGrunnlagAvTypeForBehandling(
        behandlingId: UUID,
        vararg typer: Opplysningstype,
    ): List<GrunnlagHendelse> =
        connection.use {
            it
                .prepareStatement(
                    """
                    SELECT bv.sak_id, opplysning_id, kilde, opplysning_type, opplysning, hendelse.hendelsenummer, fnr, fom, tom
                    FROM grunnlagshendelse hendelse 
                    LEFT JOIN behandling_versjon bv 
                        ON bv.sak_id = hendelse.sak_id AND bv.hendelsenummer >= hendelse.hendelsenummer 
                    WHERE bv.behandling_id = ?
                        AND hendelse.opplysning_type = ANY(?)
                    """.trimIndent(),
                ).apply {
                    setObject(1, behandlingId)
                    val typeArray = it.createArrayOf("text", typer.map { t -> t.name }.toTypedArray())
                    setArray(2, typeArray)
                }.executeQuery()
                .toList { asGrunnlagshendelse() }
        }

    fun finnHendelserIGrunnlag(sakId: SakId): List<GrunnlagHendelse> =
        connection.use {
            it
                .prepareStatement(
                    """
                    SELECT sak_id, opplysning_id, kilde, opplysning_type, opplysning, hendelsenummer, fnr, fom, tom
                    FROM grunnlagshendelse hendelse 
                    WHERE hendelse.sak_id = ? 
                    AND NOT EXISTS(
                        SELECT 1 FROM grunnlagshendelse annen 
                        WHERE annen.sak_id = hendelse.sak_id 
                        AND hendelse.opplysning_type = annen.opplysning_type 
                        AND annen.hendelsenummer > hendelse.hendelsenummer
                    )
                    """.trimIndent(),
                ).apply {
                    setLong(1, sakId)
                }.executeQuery()
                .toList { asGrunnlagshendelse() }
        }

    fun finnNyesteGrunnlagForSak(
        sakId: SakId,
        opplysningType: Opplysningstype,
    ): GrunnlagHendelse? =
        connection.use {
            it
                .prepareStatement(
                    """
                    SELECT sak_id, opplysning_id, kilde, opplysning_type, opplysning, hendelsenummer, fnr, fom, tom
                    FROM grunnlagshendelse hendelse 
                    WHERE hendelse.sak_id = ? 
                    AND hendelse.opplysning_type = ? 
                    AND NOT EXISTS(
                        SELECT 1 FROM grunnlagshendelse annen 
                        WHERE annen.sak_id = hendelse.sak_id 
                        AND hendelse.opplysning_type = annen.opplysning_type 
                        AND annen.hendelsenummer > hendelse.hendelsenummer
                    )
                    """.trimIndent(),
                ).apply {
                    setLong(1, sakId)
                    setString(2, opplysningType.name)
                }.executeQuery()
                .singleOrNull { asGrunnlagshendelse() }
        }

    fun finnNyesteGrunnlagForBehandling(
        behandlingId: UUID,
        opplysningType: Opplysningstype,
    ): GrunnlagHendelse? =
        connection.use {
            it
                .prepareStatement(
                    """
                    SELECT hendelse.sak_id, behandling_id, opplysning_id, kilde, opplysning_type, 
                            opplysning, hendelse.hendelsenummer, fnr, fom, tom
                    FROM grunnlagshendelse hendelse 
                    LEFT JOIN behandling_versjon versjon 
                        ON versjon.sak_id = hendelse.sak_id AND versjon.hendelsenummer >= hendelse.hendelsenummer
                    WHERE versjon.behandling_id = ?
                    AND hendelse.opplysning_type = ?
                    ORDER BY hendelse.hendelsenummer DESC
                    """.trimIndent(),
                ).apply {
                    setObject(1, behandlingId)
                    setString(2, opplysningType.name)
                }.executeQuery()
                .firstOrNull { asGrunnlagshendelse() }
        }

    fun leggOpplysningTilGrunnlag(
        sakId: SakId,
        behandlingsopplysning: Grunnlagsopplysning<JsonNode>,
        fnr: Folkeregisteridentifikator? = null,
    ): Long =
        connection.use {
            it
                .prepareStatement(
                    """INSERT INTO grunnlagshendelse(opplysning_id, sak_id, opplysning, kilde, opplysning_type, hendelsenummer, fnr, fom, tom)
                | VALUES(?, ?, ?, ?, ?, COALESCE((select max (hendelsenummer) + 1 from grunnlagshendelse where sak_id = ?), 1), ?, ?, ?) returning hendelsenummer 
                    """.trimMargin(),
                ).apply {
                    setObject(1, behandlingsopplysning.id)
                    setLong(2, sakId)
                    setString(3, behandlingsopplysning.opplysning.serialize())
                    setString(4, behandlingsopplysning.kilde.toJson())
                    setString(5, behandlingsopplysning.opplysningType.name)
                    setLong(6, sakId)
                    if (fnr != null) setString(7, fnr.value) else setNull(7, VARCHAR)
                    behandlingsopplysning.periode?.fom?.let { setString(8, it.toString()) } ?: setNull(8, VARCHAR)
                    behandlingsopplysning.periode?.tom?.let { setString(9, it.toString()) } ?: setNull(9, VARCHAR)
                }.executeQuery()
                .apply { next() }
                .getLong("hendelsenummer")
        }

    fun oppdaterVersjonForBehandling(
        behandlingId: UUID,
        sakId: SakId,
        hendelsenummer: Long,
    ) = connection.use {
        it
            .prepareStatement(
                """
                INSERT INTO behandling_versjon (behandling_id, sak_id, hendelsenummer, laast) VALUES (?::UUID, ?, ?, ?) 
                ON CONFLICT (behandling_id) 
                DO UPDATE SET hendelsenummer = excluded.hendelsenummer
                """.trimIndent(),
            ).apply {
                setObject(1, behandlingId)
                setLong(2, sakId)
                setLong(3, hendelsenummer)
                setBoolean(4, false)
            }.executeUpdate()
            .also { require(it > 0) }
    }

    fun laasGrunnlagVersjonForBehandling(behandlingId: UUID) =
        connection.use {
            it
                .prepareStatement("UPDATE behandling_versjon SET laast = true WHERE behandling_id = ?::UUID")
                .apply { setObject(1, behandlingId) }
                .executeUpdate()
        }

    fun finnAllePersongalleriHvorPersonFinnes(fnr: Folkeregisteridentifikator): List<GrunnlagHendelse> =
        connection.use {
            it
                .prepareStatement(
                    """
                    SELECT * FROM grunnlagshendelse
                    WHERE opplysning LIKE ?
                    AND opplysning_type = ?;
                    """.trimIndent(),
                ).apply {
                    setString(1, "%${fnr.value}%")
                    setString(2, Opplysningstype.PERSONGALLERI_V1.name)
                }.executeQuery()
                .toList { asGrunnlagshendelse() }
        }

    fun finnAlleSakerForPerson(fnr: Folkeregisteridentifikator): Set<SakId> =
        connection.use {
            it
                .prepareStatement(
                    """
                    SELECT distinct(sak_id) 
                    FROM grunnlagshendelse
                    WHERE opplysning LIKE ?
                    OR fnr = ?;
                    """.trimIndent(),
                ).apply {
                    setString(1, "%${fnr.value}%")
                    setString(2, fnr.value)
                }.executeQuery()
                .toList { getLong("sak_id") }
                .toSet()
        }

    fun hentBehandlingVersjon(behandlingId: UUID): BehandlingGrunnlagVersjon? =
        connection.use {
            it
                .prepareStatement("SELECT * FROM behandling_versjon WHERE behandling_id = ?::UUID")
                .apply { setObject(1, behandlingId) }
                .executeQuery()
                .singleOrNull {
                    BehandlingGrunnlagVersjon(
                        getObject("behandling_id") as UUID,
                        getLong("sak_id"),
                        getLong("hendelsenummer"),
                        getBoolean("laast"),
                    )
                }
        }
}

data class BehandlingGrunnlagVersjon(
    val behandlingId: UUID,
    val sakId: SakId,
    val hendelsenummer: Long,
    val laast: Boolean,
)

fun JsonNode?.serialize() = this?.let { objectMapper.writeValueAsString(it) }

fun String?.deSerialize() = this?.let { objectMapper.readValue(this, JsonNode::class.java) }

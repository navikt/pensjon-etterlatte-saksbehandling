package no.nav.etterlatte.beregning

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.readValue
import kotliquery.Row
import kotliquery.queryOf
import no.nav.etterlatte.beregning.grunnlag.InstitusjonsoppholdBeregningsgrunnlag
import no.nav.etterlatte.libs.common.IntBroek
import no.nav.etterlatte.libs.common.beregning.BeregningsMetode
import no.nav.etterlatte.libs.common.beregning.Beregningsperiode
import no.nav.etterlatte.libs.common.beregning.Beregningstype
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.Metadata
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toTidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toTimestamp
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.database.transaction
import java.io.Serializable
import java.time.YearMonth
import java.util.UUID
import javax.sql.DataSource

class BeregningRepository(private val dataSource: DataSource) {
    fun hent(behandlingId: UUID): Beregning? =
        dataSource.transaction { tx ->
            val beregningsperioder =
                queryOf(
                    statement = Queries.hentBeregning,
                    paramMap = mapOf("behandlingId" to behandlingId),
                ).let { query ->
                    tx.run(query.map { toBeregningsperiode(it) }.asList).ifEmpty {
                        null
                    }
                }
            beregningsperioder?.let { toBeregning(beregningsperioder) }
        }

    fun lagreEllerOppdaterBeregning(beregning: Beregning): Beregning {
        dataSource.transaction { tx ->
            queryOf(
                statement = Queries.slettBeregning,
                paramMap = mapOf("behandlingId" to beregning.behandlingId),
            ).let { query ->
                tx.run(query.asUpdate)
            }
            val queries =
                beregning.beregningsperioder.map {
                    createMapFromBeregningsperiode(it, beregning)
                }
            tx.batchPreparedNamedStatement(Queries.lagreBeregningsperioder, queries)
        }
        return hent(beregning.behandlingId)!!
    }

    fun hentOverstyrBeregning(sakId: Long): OverstyrBeregning? {
        return dataSource.transaction { tx ->
            queryOf(
                statement = Queries.hentOverstyrBeregning,
                paramMap = mapOf("sakId" to sakId),
            ).let { query ->
                tx.run(query.map { toOverstyrBeregning(it) }.asSingle)
            }
        }
    }

    fun opprettOverstyrBeregning(overstyrBeregning: OverstyrBeregning): OverstyrBeregning {
        dataSource.transaction { tx ->
            queryOf(
                statement = Queries.opprettOverstyrBeregning,
                paramMap =
                    mapOf(
                        "sakId" to overstyrBeregning.sakId,
                        "beskrivelse" to overstyrBeregning.beskrivelse,
                        "tidspunkt" to overstyrBeregning.tidspunkt.toTimestamp(),
                    ),
            ).let { query ->
                tx.run(query.asUpdate)
            }
        }

        return checkNotNull(hentOverstyrBeregning(overstyrBeregning.sakId)) {
            "Vi opprettet en overstyrt beregning på sakId=${overstyrBeregning.sakId} akkurat nå men den finnes ikke >:("
        }
    }

    private fun createMapFromBeregningsperiode(
        beregningsperiode: Beregningsperiode,
        beregning: Beregning,
    ): Map<String, Serializable?> {
        return mapOf(
            "id" to UUID.randomUUID(),
            "beregningId" to beregning.beregningId,
            "behandlingId" to beregning.behandlingId,
            "type" to beregning.type.name,
            "beregnetDato" to beregning.beregnetDato.toTimestamp(),
            "datoFOM" to beregningsperiode.datoFOM.toString(),
            "datoTOM" to beregningsperiode.datoTOM?.toString(),
            "utbetaltBeloep" to beregningsperiode.utbetaltBeloep,
            "institusjonsopphold" to beregningsperiode.institusjonsopphold?.toJson(),
            "soeskenFlokk" to beregningsperiode.soeskenFlokk?.toJson(),
            "grunnbeloepMnd" to beregningsperiode.grunnbelopMnd,
            "grunnbeloep" to beregningsperiode.grunnbelop,
            "sakId" to beregning.grunnlagMetadata.sakId,
            "grunnlagVersjon" to beregning.grunnlagMetadata.versjon,
            "trygdetid" to beregningsperiode.trygdetid,
            "trygdetidForIdent" to beregningsperiode.trygdetidForIdent,
            "beregningsMetode" to beregningsperiode.beregningsMetode?.name,
            "samletNorskTrygdetid" to beregningsperiode.samletNorskTrygdetid,
            "samletTeoretiskTrygdetid" to beregningsperiode.samletTeoretiskTrygdetid,
            "prorataBroekTeller" to beregningsperiode.broek?.teller,
            "prorataBroekNevner" to beregningsperiode.broek?.nevner,
            "regelResultat" to beregningsperiode.regelResultat?.toJson(),
            "regelVersjon" to beregningsperiode.regelVersjon,
            "kilde" to beregningsperiode.kilde?.toJson(),
        )
    }
}

private fun toOverstyrBeregning(row: Row): OverstyrBeregning =
    with(row) {
        OverstyrBeregning(
            sakId = long("sak_id"),
            beskrivelse = string("beskrivelse"),
            tidspunkt = sqlTimestamp("tidspunkt").toTidspunkt(),
        )
    }

private fun toBeregningsperiode(row: Row): BeregningsperiodeDAO =
    with(row) {
        BeregningsperiodeDAO(
            beregningId = uuid(BeregningsperiodeDatabaseColumns.BeregningId.navn),
            behandlingId = uuid(BeregningsperiodeDatabaseColumns.BehandlingId.navn),
            type = string(BeregningsperiodeDatabaseColumns.BeregningType.navn).let { Beregningstype.valueOf(it) },
            beregnetDato = sqlTimestamp(BeregningsperiodeDatabaseColumns.BeregnetDato.navn).toTidspunkt(),
            datoFOM = YearMonth.parse(string(BeregningsperiodeDatabaseColumns.DatoFOM.navn)),
            datoTOM = stringOrNull(BeregningsperiodeDatabaseColumns.DatoTOM.navn)?.let { YearMonth.parse(it) },
            utbetaltBeloep = int(BeregningsperiodeDatabaseColumns.UtbetaltBeloep.navn),
            soeskenFlokk =
                stringOrNull(BeregningsperiodeDatabaseColumns.SoeskenFlokk.navn)?.let {
                    objectMapper.readValue(it)
                },
            institusjonsopphold =
                stringOrNull(BeregningsperiodeDatabaseColumns.Institusjonsopphold.navn)?.let {
                    objectMapper.readValue(it)
                },
            grunnbelopMnd = int(BeregningsperiodeDatabaseColumns.GrunnbeloepMnd.navn),
            grunnbelop = int(BeregningsperiodeDatabaseColumns.Grunnbeloep.navn),
            grunnlagMetadata =
                Metadata(
                    sakId = long(BeregningsperiodeDatabaseColumns.SakId.navn),
                    versjon = long(BeregningsperiodeDatabaseColumns.GrunnlagVersjon.navn),
                ),
            trygdetid = int(BeregningsperiodeDatabaseColumns.Trygdetid.navn),
            // TODO stringOrNull(BeregningsperiodeDatabaseColumns.TrygdetidForIdent.navn),
            trygdetidForIdent = null,
            beregningsMetode =
                stringOrNull(BeregningsperiodeDatabaseColumns.BeregningsMetode.navn)?.let {
                    BeregningsMetode.valueOf(it)
                } ?: BeregningsMetode.NASJONAL,
            samletNorskTrygdetid = intOrNull(BeregningsperiodeDatabaseColumns.SamletNorskTrygdetid.navn),
            samletTeoretiskTrygdetid = intOrNull(BeregningsperiodeDatabaseColumns.SamletTeoretiskTrygdetid.navn),
            broek =
                intOrNull(BeregningsperiodeDatabaseColumns.ProrataBroekTeller.navn)?.let { teller ->
                    intOrNull(BeregningsperiodeDatabaseColumns.ProrataBroekNevner.navn)?.let { nevner ->
                        IntBroek(teller, nevner)
                    }
                },
            regelResultat =
                stringOrNull(BeregningsperiodeDatabaseColumns.RegelResultat.navn)?.let {
                    objectMapper.readTree(it)
                },
            regelVersjon = stringOrNull(BeregningsperiodeDatabaseColumns.RegelVersjon.navn),
            kilde = stringOrNull("kilde")?.let { objectMapper.readValue(it) },
        )
    }

private fun toBeregning(beregningsperioder: List<BeregningsperiodeDAO>): Beregning {
    val base =
        beregningsperioder.first().apply {
            check(beregningsperioder.all { it.beregningId == beregningId }) {
                "Beregningen inneholder forskjellige beregningsIder $beregningId for beregning $beregningId"
            }
            check(beregningsperioder.all { it.behandlingId == behandlingId }) {
                "Beregningen inneholder forskjellige behandlingIder $behandlingId for beregning $beregningId"
            }
            check(beregningsperioder.all { it.type == type }) {
                "Beregningen inneholder forskjellige typer $type for beregning $beregningId"
            }
            check(beregningsperioder.all { it.beregnetDato == beregnetDato }) {
                "Beregningen inneholder forskjellige beregnetDatoer $beregnetDato for beregning $beregningId"
            }
            check(beregningsperioder.all { it.grunnlagMetadata == grunnlagMetadata }) {
                "Beregningen inneholder forskjellige grunnlagMetadata $grunnlagMetadata for beregning $beregningId"
            }
        }

    return Beregning(
        beregningId = base.beregningId,
        behandlingId = base.behandlingId,
        type = base.type,
        beregnetDato = base.beregnetDato,
        grunnlagMetadata = base.grunnlagMetadata,
        beregningsperioder =
            beregningsperioder.map {
                Beregningsperiode(
                    datoFOM = it.datoFOM,
                    datoTOM = it.datoTOM,
                    utbetaltBeloep = it.utbetaltBeloep,
                    soeskenFlokk = it.soeskenFlokk,
                    institusjonsopphold = it.institusjonsopphold,
                    grunnbelopMnd = it.grunnbelopMnd,
                    grunnbelop = it.grunnbelop,
                    trygdetid = it.trygdetid,
                    trygdetidForIdent = it.trygdetidForIdent,
                    beregningsMetode = it.beregningsMetode,
                    samletNorskTrygdetid = it.samletNorskTrygdetid,
                    samletTeoretiskTrygdetid = it.samletTeoretiskTrygdetid,
                    broek = it.broek,
                    regelResultat = it.regelResultat,
                    regelVersjon = it.regelVersjon,
                    kilde = it.kilde,
                )
            },
        overstyrBeregning = null,
    )
}

private enum class BeregningsperiodeDatabaseColumns(val navn: String) {
    Id("id"),
    BeregningId("beregningId"),
    BehandlingId("behandlingId"),
    BeregningType("type"),
    BeregnetDato("beregnetDato"),
    DatoFOM("datoFOM"),
    DatoTOM("datoTOM"),
    UtbetaltBeloep("utbetaltBeloep"),
    SoeskenFlokk("soeskenFlokk"),
    GrunnbeloepMnd("grunnbeloepMnd"),
    Grunnbeloep("grunnbeloep"),
    SakId("sakId"),
    GrunnlagVersjon("grunnlagVersjon"),
    Trygdetid("trygdetid"),
    TrygdetidForIdent("trygdetidForIdent"),
    BeregningsMetode("beregnings_metode"),
    SamletNorskTrygdetid("samlet_norsk_trygdetid"),
    SamletTeoretiskTrygdetid("samlet_teoretisk_trygdetid"),
    ProrataBroekNevner("prorata_broek_nevner"),
    ProrataBroekTeller("prorata_broek_teller"),
    RegelResultat("regelResultat"),
    RegelVersjon("regelVersjon"),
    Kilde("kilde"),
    Institusjonsopphold("institusjonsopphold"),
}

private object Queries {
    val hentBeregning = """
        SELECT * 
        FROM beregningsperiode 
        WHERE ${BeregningsperiodeDatabaseColumns.BehandlingId.navn} = :behandlingId::UUID
    """

    val lagreBeregningsperioder = """
        INSERT INTO beregningsperiode(
            ${BeregningsperiodeDatabaseColumns.Id.navn}, 
            ${BeregningsperiodeDatabaseColumns.BeregningId.navn}, 
            ${BeregningsperiodeDatabaseColumns.BehandlingId.navn}, 
            ${BeregningsperiodeDatabaseColumns.BeregningType.navn}, 
            ${BeregningsperiodeDatabaseColumns.BeregnetDato.navn}, 
            ${BeregningsperiodeDatabaseColumns.DatoFOM.navn}, 
            ${BeregningsperiodeDatabaseColumns.DatoTOM.navn}, 
            ${BeregningsperiodeDatabaseColumns.UtbetaltBeloep.navn}, 
            ${BeregningsperiodeDatabaseColumns.SoeskenFlokk.navn}, 
            ${BeregningsperiodeDatabaseColumns.GrunnbeloepMnd.navn}, 
            ${BeregningsperiodeDatabaseColumns.Grunnbeloep.navn}, 
            ${BeregningsperiodeDatabaseColumns.SakId.navn}, 
            ${BeregningsperiodeDatabaseColumns.GrunnlagVersjon.navn}, 
            ${BeregningsperiodeDatabaseColumns.Trygdetid.navn},
            ${BeregningsperiodeDatabaseColumns.BeregningsMetode.navn},
            ${BeregningsperiodeDatabaseColumns.SamletNorskTrygdetid.navn},
            ${BeregningsperiodeDatabaseColumns.SamletTeoretiskTrygdetid.navn},
            ${BeregningsperiodeDatabaseColumns.ProrataBroekTeller.navn},
            ${BeregningsperiodeDatabaseColumns.ProrataBroekNevner.navn},
            ${BeregningsperiodeDatabaseColumns.RegelResultat.navn}, 
            ${BeregningsperiodeDatabaseColumns.RegelVersjon.navn},
            ${BeregningsperiodeDatabaseColumns.Kilde.navn},
            ${BeregningsperiodeDatabaseColumns.Institusjonsopphold.navn})
        VALUES(:id::UUID, :beregningId::UUID, :behandlingId::UUID, :type::TEXT, :beregnetDato::TIMESTAMP, 
            :datoFOM::TEXT, :datoTOM::TEXT, :utbetaltBeloep::BIGINT, :soeskenFlokk::JSONB, :grunnbeloepMnd::BIGINT, 
            :grunnbeloep::BIGINT, :sakId::BIGINT, :grunnlagVersjon::BIGINT, :trygdetid::BIGINT, :beregningsMetode::TEXT,
            :samletNorskTrygdetid::BIGINT, :samletTeoretiskTrygdetid::BIGINT, :prorataBroekTeller::BIGINT,
            :prorataBroekNevner::BIGINT, :regelResultat::JSONB, :regelVersjon::TEXT, :kilde::TEXT,
            :institusjonsopphold::JSONB) 
    """

    val slettBeregning = """
        DELETE FROM beregningsperiode 
        WHERE ${BeregningsperiodeDatabaseColumns.BehandlingId.navn} = :behandlingId::UUID
    """

    val hentOverstyrBeregning =
        """
        SELECT *
        FROM overstyr_beregning 
        WHERE sak_id = :sakId
        """.trimIndent()

    val opprettOverstyrBeregning =
        """
        INSERT INTO overstyr_beregning (sak_id, beskrivelse, tidspunkt)
        VALUES (:sakId, :beskrivelse, :tidspunkt)
        """.trimIndent()
}

private data class BeregningsperiodeDAO(
    val beregningId: UUID,
    val behandlingId: UUID,
    val type: Beregningstype,
    val beregnetDato: Tidspunkt,
    val datoFOM: YearMonth,
    val datoTOM: YearMonth?,
    val utbetaltBeloep: Int,
    val soeskenFlokk: List<String>?,
    val institusjonsopphold: InstitusjonsoppholdBeregningsgrunnlag? = null,
    val grunnbelopMnd: Int,
    val grunnbelop: Int,
    val grunnlagMetadata: Metadata,
    val trygdetid: Int,
    val trygdetidForIdent: String?,
    val beregningsMetode: BeregningsMetode? = null,
    val samletNorskTrygdetid: Int? = null,
    val samletTeoretiskTrygdetid: Int? = null,
    val broek: IntBroek? = null,
    val regelResultat: JsonNode? = null,
    val regelVersjon: String? = null,
    val kilde: Grunnlagsopplysning.RegelKilde? = null,
)

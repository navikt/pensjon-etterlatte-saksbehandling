package no.nav.etterlatte.beregning

import com.fasterxml.jackson.module.kotlin.readValue
import kotliquery.Row
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.etterlatte.libs.common.beregning.Beregningsperiode
import no.nav.etterlatte.libs.common.beregning.Beregningstype
import no.nav.etterlatte.libs.common.grunnlag.Metadata
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.tidspunkt.toTidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toTimestamp
import no.nav.etterlatte.libs.common.toJson
import java.io.Serializable
import java.time.YearMonth
import java.util.*
import javax.sql.DataSource

class BeregningRepository(private val dataSource: DataSource) {

    fun hent(behandlingId: UUID): Beregning? = using(sessionOf(dataSource)) { session ->
        session.transaction { tx ->
            val beregningsperioder = queryOf(
                statement = Queries.hentBeregning,
                paramMap = mapOf("behandlingId" to behandlingId)
            ).let { query ->
                tx.run(query.map { toBeregningsperiode(it) }.asList).ifEmpty {
                    null
                }
            }
            beregningsperioder?.let { toBeregning(beregningsperioder) }
        }
    }

    fun lagreEllerOppdaterBeregning(beregning: Beregning): Beregning {
        using(sessionOf(dataSource)) { session ->
            session.transaction { tx ->
                queryOf(
                    statement = Queries.slettBeregning,
                    paramMap = mapOf("behandlingId" to beregning.behandlingId)
                ).let { query ->
                    tx.run(query.asUpdate)
                }
                val queries = beregning.beregningsperioder.map {
                    createMapFromBeregningsperiode(it, beregning)
                }
                tx.batchPreparedNamedStatement(Queries.lagreBeregningsperioder, queries)
            }
        }
        return hent(beregning.behandlingId)!!
    }

    private fun createMapFromBeregningsperiode(
        beregningsperiode: Beregningsperiode,
        beregning: Beregning
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
            "soeskenFlokk" to beregningsperiode.soeskenFlokk?.toJson(),
            "grunnbeloepMnd" to beregningsperiode.grunnbelopMnd,
            "grunnbeloep" to beregningsperiode.grunnbelop,
            "sakId" to beregning.grunnlagMetadata.sakId,
            "grunnlagVersjon" to beregning.grunnlagMetadata.versjon,
            "trygdetid" to beregningsperiode.trygdetid,
            "regelResultat" to beregningsperiode.regelResultat?.toJson(),
            "regelVersjon" to beregningsperiode.regelVersjon
        )
    }
}

private fun toBeregningsperiode(row: Row): BeregningsperiodeDAO = with(row) {
    BeregningsperiodeDAO(
        beregningId = uuid(BeregningsperiodeDatabaseColumns.BeregningId.navn),
        behandlingId = uuid(BeregningsperiodeDatabaseColumns.BehandlingId.navn),
        type = string(BeregningsperiodeDatabaseColumns.BeregningType.navn).let { Beregningstype.valueOf(it) },
        beregnetDato = sqlTimestamp(BeregningsperiodeDatabaseColumns.BeregnetDato.navn).toTidspunkt(),
        datoFOM = YearMonth.parse(string(BeregningsperiodeDatabaseColumns.DatoFOM.navn)),
        datoTOM = stringOrNull(BeregningsperiodeDatabaseColumns.DatoTOM.navn)?.let { YearMonth.parse(it) },
        utbetaltBeloep = int(BeregningsperiodeDatabaseColumns.UtbetaltBeloep.navn),
        soeskenFlokk = stringOrNull(BeregningsperiodeDatabaseColumns.SoeskenFlokk.navn)?.let {
            objectMapper.readValue(it)
        },
        grunnbelopMnd = int(BeregningsperiodeDatabaseColumns.GrunnbeloepMnd.navn),
        grunnbelop = int(BeregningsperiodeDatabaseColumns.Grunnbeloep.navn),
        grunnlagMetadata = Metadata(
            sakId = long(BeregningsperiodeDatabaseColumns.SakId.navn),
            versjon = long(BeregningsperiodeDatabaseColumns.GrunnlagVersjon.navn)
        ),
        trygdetid = int(BeregningsperiodeDatabaseColumns.Trygdetid.navn),
        regelResultat = stringOrNull(BeregningsperiodeDatabaseColumns.RegelResultat.navn)?.let {
            objectMapper.readTree(it)
        },
        regelVersjon = stringOrNull(BeregningsperiodeDatabaseColumns.RegelVersjon.navn)
    )
}

private fun toBeregning(beregningsperioder: List<BeregningsperiodeDAO>): Beregning {
    val base = beregningsperioder.first().apply {
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
        beregningsperioder = beregningsperioder.map {
            Beregningsperiode(
                datoFOM = it.datoFOM,
                datoTOM = it.datoTOM,
                utbetaltBeloep = it.utbetaltBeloep,
                soeskenFlokk = it.soeskenFlokk,
                grunnbelopMnd = it.grunnbelopMnd,
                grunnbelop = it.grunnbelop,
                trygdetid = it.trygdetid,
                regelResultat = it.regelResultat,
                regelVersjon = it.regelVersjon
            )
        }

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
    RegelResultat("regelResultat"),
    RegelVersjon("regelVersjon")
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
            ${BeregningsperiodeDatabaseColumns.RegelResultat.navn}, 
            ${BeregningsperiodeDatabaseColumns.RegelVersjon.navn}) 
        VALUES(:id::UUID, :beregningId::UUID, :behandlingId::UUID, :type::TEXT, :beregnetDato::TIMESTAMP, 
            :datoFOM::TEXT, :datoTOM::TEXT, :utbetaltBeloep::BIGINT, :soeskenFlokk::JSONB, :grunnbeloepMnd::BIGINT, 
            :grunnbeloep::BIGINT, :sakId::BIGINT, :grunnlagVersjon::BIGINT, :trygdetid::BIGINT, :regelResultat::JSONB, 
            :regelVersjon::TEXT) 
    """

    val slettBeregning = """
        DELETE FROM beregningsperiode 
        WHERE ${BeregningsperiodeDatabaseColumns.BehandlingId.navn} = :behandlingId::UUID
    """
}
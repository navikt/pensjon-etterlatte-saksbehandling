package no.nav.etterlatte.beregning

import com.fasterxml.jackson.module.kotlin.readValue
import kotliquery.Row
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.etterlatte.libs.common.beregning.Beregningsperiode
import no.nav.etterlatte.libs.common.beregning.Beregningstyper
import no.nav.etterlatte.libs.common.beregning.DelytelseId
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
    val basePeriode = beregningsperioder.first()
    if (beregningsperioder.any { it.beregningId != basePeriode.beregningId }) throw IllegalStateException("Beregningen inneholder forskjellige beredningsIder ${basePeriode.beregningId} for behandling ${basePeriode.behandlingId}") // ktlint-disable argument-list-wrapping
    if (beregningsperioder.any { it.behandlingId != basePeriode.behandlingId }) throw IllegalStateException("Beregningen inneholder forskjellige behandlingIder ${basePeriode.behandlingId} for beregning ${basePeriode.beregningId}") // ktlint-disable argument-list-wrapping
    if (beregningsperioder.any { it.beregnetDato != basePeriode.beregnetDato }) throw IllegalStateException("Beregningen inneholder forskjellige beregnetDatoer ${basePeriode.beregnetDato} med behandlingId ${basePeriode.behandlingId} for beregning ${basePeriode.beregningId}") // ktlint-disable argument-list-wrapping
    if (beregningsperioder.any { it.grunnlagMetadata.versjon != basePeriode.grunnlagMetadata.versjon }) throw IllegalStateException("Beregningen inneholder forskjellige grunnlagsversjoner ${basePeriode.grunnlagMetadata.versjon} med behandlingId ${basePeriode.behandlingId} for beregning ${basePeriode.beregningId}") // ktlint-disable argument-list-wrapping

    return Beregning(
        beregningId = basePeriode.beregningId,
        behandlingId = basePeriode.behandlingId,
        beregnetDato = basePeriode.beregnetDato,
        grunnlagMetadata = basePeriode.grunnlagMetadata,
        beregningsperioder = beregningsperioder.map {
            Beregningsperiode(
                delytelsesId = DelytelseId.BP, // TODO sj: Dette feltet må mappes riktig
                type = Beregningstyper.GP, // TODO sj: Dette feltet må mappes riktig
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
        VALUES(:id::UUID, :beregningId::UUID, :behandlingId::UUID, :beregnetDato::TIMESTAMP, :datoFOM::TEXT, 
            :datoTOM::TEXT, :utbetaltBeloep::BIGINT, :soeskenFlokk::JSONB, :grunnbeloepMnd::BIGINT, 
            :grunnbeloep::BIGINT, :sakId::BIGINT, :grunnlagVersjon::BIGINT, :trygdetid::BIGINT, :regelResultat::JSONB, 
            :regelVersjon::TEXT) 
    """

    val slettBeregning = """
        DELETE FROM beregningsperiode 
        WHERE ${BeregningsperiodeDatabaseColumns.BehandlingId.navn} = :behandlingId::UUID
    """
}
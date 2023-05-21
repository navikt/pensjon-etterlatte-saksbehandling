package no.nav.etterlatte.tilbakekreving

import com.fasterxml.jackson.module.kotlin.readValue
import kotliquery.Row
import kotliquery.param
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toTimestamp
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.database.tidspunkt
import no.nav.etterlatte.libs.database.transaction
import no.nav.etterlatte.tilbakekreving.kravgrunnlag.BehandlingId
import no.nav.etterlatte.tilbakekreving.kravgrunnlag.Kravgrunnlag
import no.nav.etterlatte.tilbakekreving.kravgrunnlag.KravgrunnlagId
import no.nav.etterlatte.tilbakekreving.kravgrunnlag.SakId
import org.slf4j.LoggerFactory
import java.util.*
import javax.sql.DataSource

class TilbakekrevingDao(
    val dataSource: DataSource
) {
    fun lagreMottattKravgrunnlag(
        mottattKravgrunnlag: Tilbakekreving.MottattKravgrunnlag
    ): Tilbakekreving.MottattKravgrunnlag =
        dataSource.transaction { tx ->
            logger.info("Lagrer kravgrunnlag for vedtakId=${mottattKravgrunnlag.kravgrunnlag.vedtakId.value}")

            queryOf(
                statement = """
                        INSERT INTO tilbakekreving(sak_id, behandling_id, kravgrunnlag_id, opprettet, kravgrunnlag)
                        VALUES(:sak_id, :behandling_id, :kravgrunnlag_id, :opprettet, to_json(:kravgrunnlag::json))
                        """,
                paramMap = with(mottattKravgrunnlag) {
                    mapOf(
                        "sak_id" to sakId.value.param<Long>(),
                        "behandling_id" to behandlingId.value.param<UUID>(),
                        "kravgrunnlag_id" to kravgrunnlagId.value.param<Long>(),
                        "opprettet" to mottattKravgrunnlag.opprettet.toTimestamp().param(),
                        "kravgrunnlag" to kravgrunnlag.toJson().param<String>()
                    )
                }
            ).let { tx.run(it.asUpdate) }
        }
            .let { hentTilbakekreving(mottattKravgrunnlag.kravgrunnlagId.value) as Tilbakekreving.MottattKravgrunnlag }

    fun lagreFattetVedtak(
        fattetVedtak: Tilbakekreving.FattetVedtak,
        tidspunkt: Tidspunkt
    ): Tilbakekreving.FattetVedtak =
        dataSource.transaction { tx ->
            logger.info("Lagrer fattet vedtak for vedtakId=${fattetVedtak.kravgrunnlag.vedtakId.value}")

            queryOf(
                statement = """
                        UPDATE tilbakekreving
                        SET vedtak = to_json(:vedtak::json)
                        WHERE kravgrunnlag_id = :kravgrunnlag_id
                        """,
                paramMap =
                mapOf(
                    "kravgrunnlag_id" to fattetVedtak.kravgrunnlagId.value
                )
            ).let { tx.run(it.asUpdate) }
        }
            .let { hentTilbakekreving(fattetVedtak.kravgrunnlagId.value) as Tilbakekreving.FattetVedtak }

    // TODO lagre attestasjon

    fun hentTilbakekreving(kravgrunnlagId: Long): Tilbakekreving? =
        using(sessionOf(dataSource)) { session ->
            queryOf(
                statement = """
                    SELECT sak_id, behandling_id, kravgrunnlag_id, opprettet, kravgrunnlag, vedtak, attestasjon 
                    FROM tilbakekreving 
                    WHERE kravgrunnlag_id = :kravgrunnlag_id
                    """,
                paramMap = mapOf("kravgrunnlag_id" to kravgrunnlagId.param<Long>())
            )
                .let {
                    session.run(
                        it.map { row ->
                            toTilbakekreving(row)
                        }.asSingle
                    )
                }
        }

    private fun toTilbakekreving(row: Row): Tilbakekreving =
        with(row) {
            val attestasjon = stringOrNull("attestasjon")
            val vedtak = stringOrNull("vedtak")
            val kravgrunnlag = stringOrNull("kravgrunnlag")
            when {
                attestasjon != null && vedtak != null -> {
                    Tilbakekreving.AttestertVedtak(
                        sakId = SakId(long("sak_id")),
                        behandlingId = BehandlingId(uuid("behandling_id"), Kravgrunnlag.UUID30("")), // TODO
                        kravgrunnlagId = KravgrunnlagId(long("kravgrunnlag_id")),
                        opprettet = tidspunkt("opprettet"),
                        kravgrunnlag = objectMapper.readValue(string("kravgrunnlag")),
                        vedtak = objectMapper.readValue(vedtak),
                        attestasjon = objectMapper.readValue(attestasjon)
                    )
                }

                vedtak != null -> {
                    Tilbakekreving.FattetVedtak(
                        sakId = SakId(row.long("sak_id")),
                        behandlingId = BehandlingId(row.uuid("behandling_id"), Kravgrunnlag.UUID30("")), // TODO
                        kravgrunnlagId = KravgrunnlagId(row.long("kravgrunnlag_id")),
                        opprettet = tidspunkt("opprettet"),
                        kravgrunnlag = objectMapper.readValue(string("kravgrunnlag")),
                        vedtak = objectMapper.readValue(vedtak)
                    )
                }

                kravgrunnlag != null -> {
                    Tilbakekreving.MottattKravgrunnlag(
                        sakId = SakId(row.long("sak_id")),
                        behandlingId = BehandlingId(row.uuid("behandling_id"), Kravgrunnlag.UUID30("")), // TODO
                        kravgrunnlagId = KravgrunnlagId(row.long("kravgrunnlag_id")),
                        opprettet = tidspunkt("opprettet"),
                        kravgrunnlag = objectMapper.readValue(kravgrunnlag)
                    )
                }

                else -> throw RuntimeException("Kunne ikke mappe tilbakekreving")
            }
        }

    companion object {
        private val logger = LoggerFactory.getLogger(TilbakekrevingDao::class.java)
    }
}
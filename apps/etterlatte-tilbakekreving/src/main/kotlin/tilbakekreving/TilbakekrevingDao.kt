package no.nav.etterlatte.tilbakekreving

import com.fasterxml.jackson.module.kotlin.readValue
import kotliquery.Row
import kotliquery.param
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.tidspunkt.toTidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toTimestamp
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.tilbakekreving.kravgrunnlag.BehandlingId
import no.nav.etterlatte.tilbakekreving.kravgrunnlag.Kravgrunnlag
import no.nav.etterlatte.tilbakekreving.kravgrunnlag.KravgrunnlagId
import no.nav.etterlatte.tilbakekreving.kravgrunnlag.SakId
import org.slf4j.LoggerFactory
import javax.sql.DataSource

class TilbakekrevingDao(private val dataSource: DataSource) {
    fun lagreMottattKravgrunnlag(
        mottattKravgrunnlag: Tilbakekreving.MottattKravgrunnlag
    ): Tilbakekreving.MottattKravgrunnlag =
        using(sessionOf(dataSource)) { session ->
            session.transaction { tx ->
                logger.info("Lagrer kravgrunnlag for vedtakId=${mottattKravgrunnlag.kravgrunnlag.vedtakId.value}")

                queryOf(
                    statement = """
                        INSERT INTO tilbakekreving(sak_id, behandling_id, kravgrunnlag_id, opprettet, kravgrunnlag)
                        VALUES(:sak_id, :behandling_id, :kravgrunnlag_id, :opprettet, to_json(:kravgrunnlag::json))
                        """,
                    paramMap = with(mottattKravgrunnlag) {
                        mapOf(
                            "sak_id" to sakId.value.param(),
                            "behandling_id" to behandlingId.value.param(),
                            "kravgrunnlag_id" to kravgrunnlagId.value.param(),
                            "opprettet" to mottattKravgrunnlag.opprettet.toTimestamp().param(),
                            "kravgrunnlag" to kravgrunnlag.toJson().param()
                        )
                    }
                ).let { tx.run(it.asUpdate) }
            }
        }
            .let { hentTilbakekreving(mottattKravgrunnlag.kravgrunnlagId.value) as Tilbakekreving.MottattKravgrunnlag }

    fun hentTilbakekreving(kravgrunnlagId: Long): Tilbakekreving? =
        using(sessionOf(dataSource)) { session ->
            queryOf(
                statement = """
                    SELECT sak_id, behandling_id, kravgrunnlag_id, opprettet, kravgrunnlag, vedtak, attestasjon 
                    FROM tilbakekreving 
                    WHERE kravgrunnlag_id = :kravgrunnlag_id
                    """,
                paramMap = mapOf("kravgrunnlag_id" to kravgrunnlagId.param())
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
                        opprettet = sqlTimestamp("opprettet").toTidspunkt(),
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
                        opprettet = sqlTimestamp("opprettet").toTidspunkt(),
                        kravgrunnlag = objectMapper.readValue(string("kravgrunnlag")),
                        vedtak = objectMapper.readValue(vedtak)
                    )
                }
                kravgrunnlag != null -> {
                    Tilbakekreving.MottattKravgrunnlag(
                        sakId = SakId(row.long("sak_id")),
                        behandlingId = BehandlingId(row.uuid("behandling_id"), Kravgrunnlag.UUID30("")), // TODO
                        kravgrunnlagId = KravgrunnlagId(row.long("kravgrunnlag_id")),
                        opprettet = sqlTimestamp("opprettet").toTidspunkt(),
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
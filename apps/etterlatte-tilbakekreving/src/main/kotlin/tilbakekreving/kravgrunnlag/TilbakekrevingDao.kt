package no.nav.etterlatte.tilbakekreving.kravgrunnlag

import kotliquery.Row
import kotliquery.param
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import org.slf4j.LoggerFactory
import javax.sql.DataSource

class TilbakekrevingDao(
    val dataSource: DataSource
) {
    fun lagreKravgrunnlag(kravgrunnlag: Kravgrunnlag, tidspunkt: Tidspunkt) =
        using(sessionOf(dataSource)) { session ->
            session.transaction { tx ->
                logger.info("Lagrer kravgrunnlag for vedtakId=${kravgrunnlag.vedtakId.value}")

                queryOf(
                    statement = """
                        INSERT INTO kravgrunnlag(kravgrunnlag_id, sak_id, vedtak_id, kontrollfelt,
                            status, saksbehandler, siste_utbetalingslinje, mottatt_kravgrunnlag_xml)
                        VALUES(:kravgrunnlag_id, :sak_id, :vedtak_id, :kontrollfelt, :status,
                            :saksbehandler, :siste_utbetalingslinje, :mottatt_kravgrunnlag_xml)
                        """,
                    paramMap = with(kravgrunnlag) {
                        mapOf(
                            "kravgrunnlag_id" to kravgrunnlagId.value.param<Long>(),
                            "sak_id" to sakId.value.param<Long>(),
                            "vedtak_id" to vedtakId.value.param<Long>(),
                            "kontrollfelt" to kontrollFelt.value.param<String>(),
                            "status" to status.toString().param<String>(),
                            "saksbehandler" to saksbehandler.value.param<String>(),
                            "siste_utbetalingslinje" to sisteUtbetalingslinjeId.value.param<String>(),
                            "mottatt_kravgrunnlag_xml" to mottattKravgrunnlagXml.param<String>()
                        )
                    }
                ).let { tx.run(it.asUpdate) }
            }
        }
            .let { hentKravgrunnlagNonNull(kravgrunnlag.kravgrunnlagId) }

    fun hentKravgrunnlag(kravgrunnlagId: Kravgrunnlag.KravgrunnlagId) =
        using(sessionOf(dataSource)) { session ->
            queryOf(
                statement = """
                    SELECT kravgrunnlag_id, sak_id, vedtak_id, kontrollfelt, status, saksbehandler, 
                        siste_utbetalingslinje, mottatt_kravgrunnlag_xml  
                    FROM kravgrunnlag 
                    WHERE kravgrunnlag_id = :kravgrunnlag_id
                    """,
                paramMap = mapOf("kravgrunnlag_id" to kravgrunnlagId.value.param<Long>())
            )
                .let {
                    session.run(it.map { row ->
                        toKravgrunnlag(row)
                    }.asSingle)
                }
        }

    private fun hentKravgrunnlagNonNull(kravgrunnlagId: Kravgrunnlag.KravgrunnlagId) =
        hentKravgrunnlag(kravgrunnlagId)
            ?: throw RuntimeException("Kravgrunnlag med kravgrunnlagId=${kravgrunnlagId.value} finnes ikke")

    private fun toKravgrunnlag(row: Row) =
        with(row) {
            Kravgrunnlag(
                kravgrunnlagId = Kravgrunnlag.KravgrunnlagId(long("kravgrunnlag_id")),
                sakId = Kravgrunnlag.SakId(long("sak_id")),
                vedtakId = Kravgrunnlag.VedtakId(long("vedtak_id")),
                kontrollFelt = Kravgrunnlag.Kontrollfelt(string("kontrollfelt")),
                status = Kravgrunnlag.KravgrunnlagStatus.valueOf(string("status")),
                saksbehandler = Kravgrunnlag.NavIdent(string("saksbehandler")),
                sisteUtbetalingslinjeId = Kravgrunnlag.UUID30(string("siste_utbetalingslinje")),
                mottattKravgrunnlagXml = string("mottatt_kravgrunnlag_xml"),
                grunnlagsperioder = emptyList() // TODO
            )
        }

    companion object {
        private val logger = LoggerFactory.getLogger(TilbakekrevingDao::class.java)
    }
}
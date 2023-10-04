package no.nav.etterlatte.vedtaksvurdering

import kotliquery.Row
import kotliquery.Session
import kotliquery.TransactionalSession
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.tidspunkt.toTidspunkt
import no.nav.etterlatte.libs.common.vedtak.Attestasjon
import no.nav.etterlatte.libs.common.vedtak.TilbakekrevingFattetVedtakDto
import no.nav.etterlatte.libs.common.vedtak.VedtakFattet
import no.nav.etterlatte.libs.common.vedtak.VedtakStatus
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import java.util.UUID
import javax.sql.DataSource

class VedtakTilbakekrevingRepository(private val datasource: DataSource) {
    fun lagreFattetVedtak(nyttVedtak: TilbakekrevingFattetVedtakDto): TilbakekrevingsVedtak =
        using(sessionOf(datasource)) {
            // language=SQL
            queryOf(
                statement = """
                 INSERT INTO vedtak(
                    behandlingId, sakid, saktype, fnr, type, vedtakstatus, saksbehandlerId,
                     fattetVedtakEnhet, datoFattet
                   )
                VALUES (
                    :behandlingId, :sakid, :saktype, :fnr, :type, :vedtakstatus,
                    :saksbehandlerId, :fattetVedtakEnhet, now()
                   )
               ON CONFLICT (behandlingId) DO UPDATE SET
                        saksbehandlerId = excluded.saksbehandlerId,
                        fattetVedtakEnhet = excluded.fattetvedtakenhet,
                        datoFattet = now()
            """,
                mapOf(
                    "behandlingId" to nyttVedtak.tilbakekrevingId,
                    "sakid" to nyttVedtak.sakId,
                    "saktype" to nyttVedtak.sakType.name,
                    "fnr" to nyttVedtak.soeker.value,
                    "type" to VedtakType.TILBAKEKREVING.name,
                    "vedtakstatus" to VedtakStatus.FATTET_VEDTAK.name,
                    "saksbehandlerId" to nyttVedtak.ansvarligSaksbehandler,
                    "fattetVedtakEnhet" to nyttVedtak.ansvarligEnhet,
                    // TODO EY-2767 ObjectNode for tilbakekreving
                ),
            ).let { query -> it.run(query.asUpdate) }
            hentVedtak(nyttVedtak.tilbakekrevingId, it)
        }

    fun lagreUnderkjentVedtak(
        tilbakekrevingId: UUID,
        tx: TransactionalSession? = null,
    ) = using(sessionOf(datasource)) {
        // TODO oppdater med null og status ti RETURNERT
    }

    fun lagreFattetVedtak(
        tilbakekrevingId: UUID,
        attestert: Attestasjon,
        tx: TransactionalSession? = null,
    ) = using(sessionOf(datasource)) {
        // TODO oppdater med attestert av og status til ATTESTERT
    }

    private fun hentVedtak(
        tilbakekrevingId: UUID,
        session: Session,
    ): TilbakekrevingsVedtak =
        session.run(
            queryOf(
                statement = """
            SELECT id, sakid, behandlingId, fnr, vedtakstatus, saktype, behandlingtype, type, 
                saksbehandlerId, datoFattet, fattetVedtakEnhet,
                attestant, datoattestert, attestertVedtakEnhet 
            FROM vedtak WHERE behandlingId = ?
            """,
                tilbakekrevingId,
            ).map { it.toTilbakekrevingVedtak() }.asSingle,
        ) ?: throw Exception("Finner ikke vedtak med tilbakekrevingId=$tilbakekrevingId")

    private fun Row.toTilbakekrevingVedtak() =
        TilbakekrevingsVedtak(
            id = long("id"),
            sakId = long("sakid"),
            sakType = SakType.valueOf(string("saktype")),
            behandlingId = uuid("behandlingid"),
            soeker = string("fnr").let { Folkeregisteridentifikator.of(it) },
            status = string("vedtakstatus").let { VedtakStatus.valueOf(it) },
            type = string("type").let { VedtakType.valueOf(it) },
            vedtakFattet =
                stringOrNull("saksbehandlerid")?.let {
                    VedtakFattet(
                        ansvarligSaksbehandler = string("saksbehandlerid"),
                        ansvarligEnhet = string("fattetVedtakEnhet"),
                        tidspunkt = sqlTimestamp("datofattet").toTidspunkt(),
                    )
                },
            attestasjon =
                stringOrNull("attestant")?.let {
                    Attestasjon(
                        attestant = string("attestant"),
                        attesterendeEnhet = string("attestertVedtakEnhet"),
                        tidspunkt = sqlTimestamp("datoattestert").toTidspunkt(),
                    )
                },
            tilbakekreving = objectMapper.createObjectNode(), // TODO EY-2767
        )
}

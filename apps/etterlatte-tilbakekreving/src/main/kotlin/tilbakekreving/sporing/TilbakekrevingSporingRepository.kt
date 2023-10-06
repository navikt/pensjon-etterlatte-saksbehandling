package no.nav.etterlatte.tilbakekreving.sporing

import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import java.util.UUID
import javax.sql.DataSource

class TilbakekrevingSporingRepository(private val dataSource: DataSource) {
    fun lagreMottattKravgrunnlag(
        kravgrunnlagId: String,
        fagsystemId: String,
        kravgrunnlagPayload: String,
    ) = using(sessionOf(dataSource)) { session ->
        queryOf(
            statement =
                """
                INSERT INTO tilbakekreving_sporing(id, opprettet, kravgrunnlag_id, fagsystem_id, kravgrunnlag_payload)
                VALUES(:id, now(), :kravgrunnlagId, :fagsystemId, :kravgrunnlagPayload)
                """.trimIndent(),
            paramMap =
                mapOf(
                    "id" to UUID.randomUUID(),
                    "kravgrunnlagId" to kravgrunnlagId,
                    "fagsystemId" to fagsystemId,
                    "kravgrunnlagPayload" to kravgrunnlagPayload,
                ),
        ).let { query ->
            session.update(query).also { require(it == 1) { "Feil under lagring av mottatt kravgrunnlag" } }
        }
    }

    fun lagreTilbakekrevingsvedtakRequest(
        kravgrunnlagId: String,
        request: String,
    ) = using(sessionOf(dataSource)) { session ->
        queryOf(
            statement =
                """
                UPDATE tilbakekreving_sporing SET tilbakekrevingsvedtak_request = :request, endret = now()
                WHERE kravgrunnlag_id = :kravgrunnlagId
                """.trimIndent(),
            paramMap =
                mapOf(
                    "kravgrunnlagId" to kravgrunnlagId,
                    "request" to request,
                ),
        ).let { query ->
            session.update(query).also { require(it == 1) { "Feil under lagring av vedtak request" } }
        }
    }

    fun lagreTilbakekrevingsvedtakResponse(
        kravgrunnlagId: String,
        response: String,
    ) = using(sessionOf(dataSource)) { session ->
        queryOf(
            statement =
                """
                UPDATE tilbakekreving_sporing SET tilbakekrevingsvedtak_response = :response, endret = now()
                WHERE kravgrunnlag_id = :kravgrunnlagId
                """.trimIndent(),
            paramMap =
                mapOf(
                    "kravgrunnlagId" to kravgrunnlagId,
                    "response" to response,
                ),
        ).let { query ->
            session.update(query).also { require(it == 1) { "Feil under lagring av vedtak response" } }
        }
    }
}

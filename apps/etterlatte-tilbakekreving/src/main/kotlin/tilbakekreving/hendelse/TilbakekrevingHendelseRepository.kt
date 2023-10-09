package no.nav.etterlatte.tilbakekreving.hendelse

import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import java.util.UUID
import javax.sql.DataSource

internal enum class TilbakekrevingHendelseType {
    KRAVGRUNNLAG_MOTTATT,
    TILBAKEKREVINGSVEDTAK_SENDT,
    TILBAKEKREVINGSVEDTAK_KVITTERING,
}

class TilbakekrevingHendelseRepository(private val dataSource: DataSource) {
    fun lagreMottattKravgrunnlag(
        kravgrunnlagId: String,
        payload: String,
    ) = lagreTilbakekrevingHendelse(kravgrunnlagId, payload, TilbakekrevingHendelseType.KRAVGRUNNLAG_MOTTATT)

    fun lagreTilbakekrevingsvedtakSendt(
        kravgrunnlagId: String,
        payload: String,
    ) = lagreTilbakekrevingHendelse(kravgrunnlagId, payload, TilbakekrevingHendelseType.TILBAKEKREVINGSVEDTAK_SENDT)

    fun lagreTilbakekrevingsvedtakKvitteringMottatt(
        kravgrunnlagId: String,
        payload: String,
    ) = lagreTilbakekrevingHendelse(
        kravgrunnlagId,
        payload,
        TilbakekrevingHendelseType.TILBAKEKREVINGSVEDTAK_KVITTERING,
    )

    private fun lagreTilbakekrevingHendelse(
        kravgrunnlagId: String,
        payload: String,
        type: TilbakekrevingHendelseType,
    ) = using(sessionOf(dataSource)) { session ->
        queryOf(
            statement =
                """
                INSERT INTO tilbakekreving_hendelse(id, opprettet, kravgrunnlag_id, payload, type)
                VALUES(:id, now(), :kravgrunnlagId, :payload, :type)
                """.trimIndent(),
            paramMap =
                mapOf(
                    "id" to UUID.randomUUID(),
                    "kravgrunnlagId" to kravgrunnlagId,
                    "payload" to payload,
                    "type" to type.name,
                ),
        ).let { query ->
            session.update(query).also {
                require(it == 1) {
                    "Feil under lagring av hendelse for kravgrunnlag $kravgrunnlagId"
                }
            }
        }
    }
}

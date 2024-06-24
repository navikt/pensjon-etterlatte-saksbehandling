package no.nav.etterlatte.tilbakekreving

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.etterlatte.libs.database.singleOrNull
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.RegisterExtension
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TilbakekrevingHendelseRepositoryTest(
    private val dataSource: DataSource,
) {
    companion object {
        @RegisterExtension
        val dbExtension = DatabaseExtension()
    }

    private val repository = TilbakekrevingHendelseRepository(dataSource)

    @AfterEach
    fun afterEach() {
        dbExtension.resetDb()
    }

    @Test
    fun `skal lagre hendelse for mottatt kravgrunnlag`() {
        val sakId = 1L
        val kravgrunnlag = "<kravgrunnlag>payload</kravgrunnlag>"

        repository.lagreTilbakekrevingHendelse(sakId, kravgrunnlag, TilbakekrevingHendelseType.KRAVGRUNNLAG_MOTTATT)

        val tilbakekrevingHendelse =
            hentTilbakekrevingHendelse(sakId, TilbakekrevingHendelseType.KRAVGRUNNLAG_MOTTATT)

        tilbakekrevingHendelse?.id shouldNotBe null
        tilbakekrevingHendelse?.opprettet shouldNotBe null
        tilbakekrevingHendelse?.sakId shouldBe sakId
        tilbakekrevingHendelse?.payload shouldBe kravgrunnlag
        tilbakekrevingHendelse?.type shouldBe TilbakekrevingHendelseType.KRAVGRUNNLAG_MOTTATT
    }

    @Test
    fun `skal lagre hendelse for mottatt kravOgVedtakStatus`() {
        val sakId = 1L
        val melding = "payload"

        repository.lagreTilbakekrevingHendelse(sakId, melding, TilbakekrevingHendelseType.KRAV_VEDTAK_STATUS_MOTTATT)

        val tilbakekrevingHendelse =
            hentTilbakekrevingHendelse(sakId, TilbakekrevingHendelseType.KRAV_VEDTAK_STATUS_MOTTATT)

        tilbakekrevingHendelse?.id shouldNotBe null
        tilbakekrevingHendelse?.opprettet shouldNotBe null
        tilbakekrevingHendelse?.sakId shouldBe sakId
        tilbakekrevingHendelse?.payload shouldBe melding
        tilbakekrevingHendelse?.type shouldBe TilbakekrevingHendelseType.KRAV_VEDTAK_STATUS_MOTTATT
    }

    @Test
    fun `skal lagre hendelse for sendt tilbakekrevingsvedtak`() {
        val sakId = 1L
        val vedtakRequest = "request_payload"

        repository.lagreTilbakekrevingHendelse(sakId, vedtakRequest, TilbakekrevingHendelseType.TILBAKEKREVINGSVEDTAK_SENDT)

        val tilbakekrevingHendelse =
            hentTilbakekrevingHendelse(sakId, TilbakekrevingHendelseType.TILBAKEKREVINGSVEDTAK_SENDT)

        tilbakekrevingHendelse?.id shouldNotBe null
        tilbakekrevingHendelse?.opprettet shouldNotBe null
        tilbakekrevingHendelse?.sakId shouldBe sakId
        tilbakekrevingHendelse?.payload shouldBe vedtakRequest
        tilbakekrevingHendelse?.type shouldBe TilbakekrevingHendelseType.TILBAKEKREVINGSVEDTAK_SENDT
    }

    @Test
    fun `skal lagre hendelse for tilbakekrevingsvedtak kvittering`() {
        val sakId = 1L
        val vedtakResponse = "response_payload"

        repository.lagreTilbakekrevingHendelse(sakId, vedtakResponse, TilbakekrevingHendelseType.TILBAKEKREVINGSVEDTAK_KVITTERING)

        val tilbakekrevingHendelse =
            hentTilbakekrevingHendelse(sakId, TilbakekrevingHendelseType.TILBAKEKREVINGSVEDTAK_KVITTERING)

        tilbakekrevingHendelse?.id shouldNotBe null
        tilbakekrevingHendelse?.opprettet shouldNotBe null
        tilbakekrevingHendelse?.sakId shouldBe sakId
        tilbakekrevingHendelse?.payload shouldBe vedtakResponse
        tilbakekrevingHendelse?.type shouldBe TilbakekrevingHendelseType.TILBAKEKREVINGSVEDTAK_KVITTERING
    }

    private fun hentTilbakekrevingHendelse(
        sakId: Long,
        type: TilbakekrevingHendelseType,
    ): TilbakekrevingHendelse? {
        dataSource.connection.use {
            val stmt =
                it
                    .prepareStatement("SELECT * FROM tilbakekreving_hendelse WHERE sak_id = ? AND type = ?")
                    .apply {
                        setLong(1, sakId)
                        setString(2, type.name)
                    }

            return stmt.executeQuery().singleOrNull {
                TilbakekrevingHendelse(
                    id = getString("id"),
                    opprettet = getString("opprettet"),
                    sakId = getLong("sak_id"),
                    payload = getString("payload"),
                    type = TilbakekrevingHendelseType.valueOf(getString("type")),
                )
            }
        }
    }

    private data class TilbakekrevingHendelse(
        val id: String,
        val opprettet: String,
        val sakId: Long,
        val payload: String,
        val type: TilbakekrevingHendelseType,
    )
}

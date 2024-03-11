package no.nav.etterlatte.tilbakekreving.hendelse

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.etterlatte.libs.database.singleOrNull
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.RegisterExtension
import tilbakekreving.DatabaseExtension
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TilbakekrevingHendelseRepositoryTest(private val dataSource: DataSource) {
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
        val kravgrunnlagId = "1"
        val kravgrunnlag = "<kravgrunnlag>payload</kravgrunnlag>"

        repository.lagreMottattKravgrunnlag(kravgrunnlagId, kravgrunnlag)

        val tilbakekrevingHendelse =
            hentTilbakekrevingHendelse(kravgrunnlagId, TilbakekrevingHendelseType.KRAVGRUNNLAG_MOTTATT)

        tilbakekrevingHendelse?.id shouldNotBe null
        tilbakekrevingHendelse?.opprettet shouldNotBe null
        tilbakekrevingHendelse?.kravgrunnlagId shouldBe kravgrunnlagId
        tilbakekrevingHendelse?.payload shouldBe kravgrunnlag
        tilbakekrevingHendelse?.type shouldBe TilbakekrevingHendelseType.KRAVGRUNNLAG_MOTTATT
    }

    @Test
    fun `skal lagre hendelse for sendt tilbakekrevingsvedtak`() {
        val kravgrunnlagId = "1"
        val vedtakRequest = "request_payload"

        repository.lagreTilbakekrevingsvedtakSendt(kravgrunnlagId, vedtakRequest)

        val tilbakekrevingHendelse =
            hentTilbakekrevingHendelse(kravgrunnlagId, TilbakekrevingHendelseType.TILBAKEKREVINGSVEDTAK_SENDT)

        tilbakekrevingHendelse?.id shouldNotBe null
        tilbakekrevingHendelse?.opprettet shouldNotBe null
        tilbakekrevingHendelse?.kravgrunnlagId shouldBe kravgrunnlagId
        tilbakekrevingHendelse?.payload shouldBe vedtakRequest
        tilbakekrevingHendelse?.type shouldBe TilbakekrevingHendelseType.TILBAKEKREVINGSVEDTAK_SENDT
    }

    @Test
    fun `skal lagre hendelse for tilbakekrevingsvedtak kvittering`() {
        val kravgrunnlagId = "1"
        val vedtakResponse = "response_payload"

        repository.lagreTilbakekrevingsvedtakKvitteringMottatt(kravgrunnlagId, vedtakResponse)

        val tilbakekrevingHendelse =
            hentTilbakekrevingHendelse(kravgrunnlagId, TilbakekrevingHendelseType.TILBAKEKREVINGSVEDTAK_KVITTERING)

        tilbakekrevingHendelse?.id shouldNotBe null
        tilbakekrevingHendelse?.opprettet shouldNotBe null
        tilbakekrevingHendelse?.kravgrunnlagId shouldBe kravgrunnlagId
        tilbakekrevingHendelse?.payload shouldBe vedtakResponse
        tilbakekrevingHendelse?.type shouldBe TilbakekrevingHendelseType.TILBAKEKREVINGSVEDTAK_KVITTERING
    }

    private fun hentTilbakekrevingHendelse(
        kravgrunnlagId: String,
        type: TilbakekrevingHendelseType,
    ): TilbakekrevingHendelse? {
        dataSource.connection.use {
            val stmt =
                it.prepareStatement("SELECT * FROM tilbakekreving_hendelse WHERE kravgrunnlag_id = ? AND type = ?")
                    .apply {
                        setString(1, kravgrunnlagId)
                        setString(2, type.name)
                    }

            return stmt.executeQuery().singleOrNull {
                TilbakekrevingHendelse(
                    id = getString("id"),
                    opprettet = getString("opprettet"),
                    kravgrunnlagId = getString("kravgrunnlag_id"),
                    payload = getString("payload"),
                    type = TilbakekrevingHendelseType.valueOf(getString("type")),
                )
            }
        }
    }

    private data class TilbakekrevingHendelse(
        val id: String,
        val opprettet: String,
        val kravgrunnlagId: String,
        val payload: String,
        val type: TilbakekrevingHendelseType,
    )
}

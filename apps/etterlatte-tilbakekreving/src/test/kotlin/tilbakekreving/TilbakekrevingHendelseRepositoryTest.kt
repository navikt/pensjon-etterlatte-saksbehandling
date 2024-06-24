package no.nav.etterlatte.tilbakekreving

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.RegisterExtension
import java.time.temporal.ChronoUnit
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
        val jmsTimestamp = Tidspunkt.now()

        repository.lagreTilbakekrevingHendelse(
            sakId,
            kravgrunnlag,
            TilbakekrevingHendelseType.KRAVGRUNNLAG_MOTTATT,
            jmsTimestamp,
        )

        val tilbakekrevingHendelse =
            repository.hentSisteTilbakekrevingHendelse(sakId, TilbakekrevingHendelseType.KRAVGRUNNLAG_MOTTATT)

        tilbakekrevingHendelse?.id shouldNotBe null
        tilbakekrevingHendelse?.opprettet shouldNotBe null
        tilbakekrevingHendelse?.sakId shouldBe sakId
        tilbakekrevingHendelse?.payload shouldBe kravgrunnlag
        tilbakekrevingHendelse?.type shouldBe TilbakekrevingHendelseType.KRAVGRUNNLAG_MOTTATT
        tilbakekrevingHendelse?.status shouldBe TilbakekrevingHendelseStatus.NY
        tilbakekrevingHendelse?.jmsTimestamp shouldBe jmsTimestamp
    }

    @Test
    fun `skal lagre hendelse for mottatt kravOgVedtakStatus`() {
        val sakId = 1L
        val melding = "payload"
        val jmsTimestamp = Tidspunkt.now()

        repository.lagreTilbakekrevingHendelse(
            sakId,
            melding,
            TilbakekrevingHendelseType.KRAV_VEDTAK_STATUS_MOTTATT,
            jmsTimestamp,
        )

        val tilbakekrevingHendelse =
            repository.hentSisteTilbakekrevingHendelse(sakId, TilbakekrevingHendelseType.KRAV_VEDTAK_STATUS_MOTTATT)

        tilbakekrevingHendelse?.id shouldNotBe null
        tilbakekrevingHendelse?.opprettet shouldNotBe null
        tilbakekrevingHendelse?.sakId shouldBe sakId
        tilbakekrevingHendelse?.payload shouldBe melding
        tilbakekrevingHendelse?.type shouldBe TilbakekrevingHendelseType.KRAV_VEDTAK_STATUS_MOTTATT
        tilbakekrevingHendelse?.status shouldBe TilbakekrevingHendelseStatus.NY
        tilbakekrevingHendelse?.jmsTimestamp shouldBe jmsTimestamp
    }

    @Test
    fun `skal lagre hendelse for sendt tilbakekrevingsvedtak`() {
        val sakId = 1L
        val vedtakRequest = "request_payload"

        repository.lagreTilbakekrevingHendelse(sakId, vedtakRequest, TilbakekrevingHendelseType.TILBAKEKREVINGSVEDTAK_SENDT)

        val tilbakekrevingHendelse =
            repository.hentSisteTilbakekrevingHendelse(sakId, TilbakekrevingHendelseType.TILBAKEKREVINGSVEDTAK_SENDT)

        tilbakekrevingHendelse?.id shouldNotBe null
        tilbakekrevingHendelse?.opprettet shouldNotBe null
        tilbakekrevingHendelse?.sakId shouldBe sakId
        tilbakekrevingHendelse?.payload shouldBe vedtakRequest
        tilbakekrevingHendelse?.type shouldBe TilbakekrevingHendelseType.TILBAKEKREVINGSVEDTAK_SENDT
        tilbakekrevingHendelse?.status shouldBe TilbakekrevingHendelseStatus.FERDIGSTILT
    }

    @Test
    fun `skal lagre hendelse for tilbakekrevingsvedtak kvittering`() {
        val sakId = 1L
        val vedtakResponse = "response_payload"

        repository.lagreTilbakekrevingHendelse(sakId, vedtakResponse, TilbakekrevingHendelseType.TILBAKEKREVINGSVEDTAK_KVITTERING)

        val tilbakekrevingHendelse =
            repository.hentSisteTilbakekrevingHendelse(sakId, TilbakekrevingHendelseType.TILBAKEKREVINGSVEDTAK_KVITTERING)

        tilbakekrevingHendelse?.id shouldNotBe null
        tilbakekrevingHendelse?.opprettet shouldNotBe null
        tilbakekrevingHendelse?.sakId shouldBe sakId
        tilbakekrevingHendelse?.payload shouldBe vedtakResponse
        tilbakekrevingHendelse?.type shouldBe TilbakekrevingHendelseType.TILBAKEKREVINGSVEDTAK_KVITTERING
        tilbakekrevingHendelse?.status shouldBe TilbakekrevingHendelseStatus.FERDIGSTILT
    }

    @Test
    fun `skal hente siste hendelse for sak basert paa jms timestamp`() {
        val sakId = 1L
        val kravgrunnlag = "<kravgrunnlag>payload</kravgrunnlag>"
        val jmsTimestampKravgrunnlag = Tidspunkt.now().plus(1, ChronoUnit.MINUTES)

        repository.lagreTilbakekrevingHendelse(
            sakId,
            kravgrunnlag,
            TilbakekrevingHendelseType.KRAVGRUNNLAG_MOTTATT,
            jmsTimestampKravgrunnlag,
        )

        val status = "payload"
        val jmsTimestampStatus = Tidspunkt.now()

        repository.lagreTilbakekrevingHendelse(
            sakId,
            status,
            TilbakekrevingHendelseType.KRAV_VEDTAK_STATUS_MOTTATT,
            jmsTimestampStatus,
        )

        val sisteTilbakekrevingHendelse =
            repository.hentSisteTilbakekrevingHendelse(sakId, TilbakekrevingHendelseType.KRAVGRUNNLAG_MOTTATT)

        sisteTilbakekrevingHendelse?.type shouldBe TilbakekrevingHendelseType.KRAVGRUNNLAG_MOTTATT
        sisteTilbakekrevingHendelse?.jmsTimestamp shouldBe jmsTimestampKravgrunnlag
    }
}

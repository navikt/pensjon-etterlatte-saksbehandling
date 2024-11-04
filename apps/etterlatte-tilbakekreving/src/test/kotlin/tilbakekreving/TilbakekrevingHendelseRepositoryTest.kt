package no.nav.etterlatte.tilbakekreving

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.etterlatte.behandling.sakId1
import no.nav.etterlatte.libs.common.dbutils.Tidspunkt
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
    fun `skal lagre og ferdigstille hendelse for mottatt kravgrunnlag`() {
        val sakId = sakId1
        val kravgrunnlag = "<kravgrunnlag>payload</kravgrunnlag>"
        val jmsTimestamp = Tidspunkt.now()

        repository.lagreTilbakekrevingHendelse(
            sakId,
            kravgrunnlag,
            TilbakekrevingHendelseType.KRAVGRUNNLAG_MOTTATT,
            jmsTimestamp,
        )

        val tilbakekrevingHendelse =
            repository.hentSisteTilbakekrevingHendelse(sakId)

        tilbakekrevingHendelse?.id shouldNotBe null
        tilbakekrevingHendelse?.opprettet shouldNotBe null
        tilbakekrevingHendelse?.sakId shouldBe sakId
        tilbakekrevingHendelse?.payload shouldBe kravgrunnlag
        tilbakekrevingHendelse?.type shouldBe TilbakekrevingHendelseType.KRAVGRUNNLAG_MOTTATT
        tilbakekrevingHendelse?.status shouldBe TilbakekrevingHendelseStatus.NY
        tilbakekrevingHendelse?.jmsTimestamp shouldBe jmsTimestamp

        repository.ferdigstillTilbakekrevingHendelse(sakId, tilbakekrevingHendelse!!.id)
        val ferdigstiltTilbakekrevingHendelse =
            repository.hentSisteTilbakekrevingHendelse(sakId)

        ferdigstiltTilbakekrevingHendelse?.status shouldBe TilbakekrevingHendelseStatus.FERDIGSTILT
    }

    @Test
    fun `skal lagre og ferdigstille hendelse for mottatt kravOgVedtakStatus`() {
        val sakId = sakId1
        val melding = "payload"
        val jmsTimestamp = Tidspunkt.now()

        repository.lagreTilbakekrevingHendelse(
            sakId,
            melding,
            TilbakekrevingHendelseType.KRAV_VEDTAK_STATUS_MOTTATT,
            jmsTimestamp,
        )

        val tilbakekrevingHendelse =
            repository.hentSisteTilbakekrevingHendelse(sakId)

        tilbakekrevingHendelse?.id shouldNotBe null
        tilbakekrevingHendelse?.opprettet shouldNotBe null
        tilbakekrevingHendelse?.sakId shouldBe sakId
        tilbakekrevingHendelse?.payload shouldBe melding
        tilbakekrevingHendelse?.type shouldBe TilbakekrevingHendelseType.KRAV_VEDTAK_STATUS_MOTTATT
        tilbakekrevingHendelse?.status shouldBe TilbakekrevingHendelseStatus.NY
        tilbakekrevingHendelse?.jmsTimestamp shouldBe jmsTimestamp

        repository.ferdigstillTilbakekrevingHendelse(sakId, tilbakekrevingHendelse!!.id)
        val ferdigstiltTilbakekrevingHendelse =
            repository.hentSisteTilbakekrevingHendelse(sakId)

        ferdigstiltTilbakekrevingHendelse?.status shouldBe TilbakekrevingHendelseStatus.FERDIGSTILT
    }

    @Test
    fun `skal lagre hendelse for sendt tilbakekrevingsvedtak`() {
        val sakId = sakId1
        val vedtakRequest = "request_payload"

        repository.lagreTilbakekrevingHendelse(sakId, vedtakRequest, TilbakekrevingHendelseType.TILBAKEKREVINGSVEDTAK_SENDT)

        val tilbakekrevingHendelse =
            repository.hentSisteTilbakekrevingHendelse(sakId)

        tilbakekrevingHendelse?.id shouldNotBe null
        tilbakekrevingHendelse?.opprettet shouldNotBe null
        tilbakekrevingHendelse?.sakId shouldBe sakId
        tilbakekrevingHendelse?.payload shouldBe vedtakRequest
        tilbakekrevingHendelse?.type shouldBe TilbakekrevingHendelseType.TILBAKEKREVINGSVEDTAK_SENDT
        tilbakekrevingHendelse?.status shouldBe TilbakekrevingHendelseStatus.FERDIGSTILT
    }

    @Test
    fun `skal lagre hendelse for tilbakekrevingsvedtak kvittering`() {
        val sakId = sakId1
        val vedtakResponse = "response_payload"

        repository.lagreTilbakekrevingHendelse(sakId, vedtakResponse, TilbakekrevingHendelseType.TILBAKEKREVINGSVEDTAK_KVITTERING)

        val tilbakekrevingHendelse =
            repository.hentSisteTilbakekrevingHendelse(sakId)

        tilbakekrevingHendelse?.id shouldNotBe null
        tilbakekrevingHendelse?.opprettet shouldNotBe null
        tilbakekrevingHendelse?.sakId shouldBe sakId
        tilbakekrevingHendelse?.payload shouldBe vedtakResponse
        tilbakekrevingHendelse?.type shouldBe TilbakekrevingHendelseType.TILBAKEKREVINGSVEDTAK_KVITTERING
        tilbakekrevingHendelse?.status shouldBe TilbakekrevingHendelseStatus.FERDIGSTILT
    }

    @Test
    fun `skal hente siste hendelse for sak basert paa jms timestamp`() {
        val sakId = sakId1
        val kravgrunnlag = "<kravgrunnlag>payload</kravgrunnlag>"
        val jmsTimestampKravgrunnlag = Tidspunkt.now()

        val hendelseIdKravgrunnlag =
            repository.lagreTilbakekrevingHendelse(
                sakId,
                kravgrunnlag,
                TilbakekrevingHendelseType.KRAVGRUNNLAG_MOTTATT,
                jmsTimestampKravgrunnlag,
            )

        repository.ferdigstillTilbakekrevingHendelse(sakId, hendelseIdKravgrunnlag)

        val jmsTimestampStatus1 = Tidspunkt.now().plus(1, ChronoUnit.MINUTES)

        val hendelseIdStatus1 =
            repository.lagreTilbakekrevingHendelse(
                sakId,
                kravgrunnlag,
                TilbakekrevingHendelseType.KRAV_VEDTAK_STATUS_MOTTATT,
                jmsTimestampStatus1,
            )

        repository.ferdigstillTilbakekrevingHendelse(sakId, hendelseIdStatus1)

        val jmsTimestampStatus2 = Tidspunkt.now().plus(2, ChronoUnit.MINUTES)

        repository.lagreTilbakekrevingHendelse(
            sakId,
            kravgrunnlag,
            TilbakekrevingHendelseType.KRAV_VEDTAK_STATUS_MOTTATT,
            jmsTimestampStatus2,
        )

        val sisteTilbakekrevingHendelse =
            repository.hentSisteTilbakekrevingHendelse(sakId)

        sisteTilbakekrevingHendelse?.type shouldBe TilbakekrevingHendelseType.KRAV_VEDTAK_STATUS_MOTTATT
        sisteTilbakekrevingHendelse?.jmsTimestamp shouldBe jmsTimestampStatus2
    }
}

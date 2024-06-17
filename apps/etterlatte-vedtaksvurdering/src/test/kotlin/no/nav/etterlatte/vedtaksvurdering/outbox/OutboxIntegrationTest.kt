package no.nav.etterlatte.vedtaksvurdering.outbox

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import no.nav.etterlatte.kafka.TestProdusent
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.deserialize
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.vedtak.VedtakStatus
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.libs.database.single
import no.nav.etterlatte.vedtaksvurdering.VedtakInnhold
import no.nav.etterlatte.vedtaksvurdering.VedtaksvurderingRepository
import no.nav.etterlatte.vedtaksvurdering.VedtaksvurderingService
import no.nav.etterlatte.vedtaksvurdering.database.DatabaseExtension
import no.nav.etterlatte.vedtaksvurdering.opprettVedtak
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Month
import java.time.YearMonth
import java.util.UUID
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(DatabaseExtension::class)
class OutboxIntegrationTest(
    private val dataSource: DataSource,
) {
    private val testProdusent = TestProdusent<UUID, String>()
    private val vedtaksvurderingRepository = VedtaksvurderingRepository(dataSource)
    private val vedtaksvurderingService = VedtaksvurderingService(vedtaksvurderingRepository)
    private val outboxRepository = OutboxRepository(dataSource)
    private val outboxService = OutboxService(outboxRepository, vedtaksvurderingService, testProdusent::publiser)

    @Test
    fun `skal publisere hendelser for alle upubliserte innslag i outbox`() {
        val vedtakAttestertInnvilgelse =
            vedtaksvurderingRepository.opprettVedtak(
                opprettVedtak(
                    virkningstidspunkt = YearMonth.of(2024, Month.APRIL),
                    soeker = Folkeregisteridentifikator.of("08815997000"),
                    sakId = 1011L,
                    type = VedtakType.INNVILGELSE,
                    behandlingId = UUID.randomUUID(),
                    status = VedtakStatus.ATTESTERT,
                    sakType = SakType.OMSTILLINGSSTOENAD,
                ),
            )
        val vedtakAttestertAlleredePublisert =
            vedtaksvurderingRepository.opprettVedtak(
                opprettVedtak(
                    virkningstidspunkt = YearMonth.of(2024, Month.FEBRUARY),
                    soeker = Folkeregisteridentifikator.of("04417103428"),
                    sakId = 2022L,
                    type = VedtakType.INNVILGELSE,
                    behandlingId = UUID.randomUUID(),
                    status = VedtakStatus.ATTESTERT,
                    sakType = SakType.OMSTILLINGSSTOENAD,
                ),
            )
        val vedtakAttestertEndring =
            vedtaksvurderingRepository.opprettVedtak(
                opprettVedtak(
                    virkningstidspunkt = YearMonth.of(2024, Month.MAY),
                    soeker = Folkeregisteridentifikator.of("04417103428"),
                    sakId = 2022L,
                    type = VedtakType.ENDRING,
                    behandlingType = BehandlingType.REVURDERING,
                    behandlingId = UUID.randomUUID(),
                    status = VedtakStatus.ATTESTERT,
                    sakType = SakType.OMSTILLINGSSTOENAD,
                ),
            )

        dataSource.connection.use { conn ->
            conn.createStatement().use { stmt ->
                stmt.execute(
                    """
                    INSERT INTO outbox_vedtakshendelse (vedtakId, type, publisert)
                    VALUES 
                    (${vedtakAttestertAlleredePublisert.id}, '${OutboxItemType.ATTESTERT.name}', true),
                    (${vedtakAttestertInnvilgelse.id}, '${OutboxItemType.ATTESTERT.name}', false),
                    (${vedtakAttestertEndring.id}, '${OutboxItemType.ATTESTERT.name}', false)
                    """,
                )
            }
        }

        outboxService.run()

        // Verifiser at rett antall hendelser er publisert med rett innhold
        testProdusent.publiserteMeldinger.map { deserialize<Vedtakshendelse>(it.verdi) } shouldContainExactlyInAnyOrder
            listOf(vedtakAttestertEndring, vedtakAttestertInnvilgelse)
                .map {
                    Vedtakshendelse(
                        ident = it.soeker.value,
                        sakstype = it.sakType.toEksternApi(),
                        type = it.typeToEksternApi(),
                        vedtakId = it.id,
                        vedtaksdato = it.vedtakFattet?.tidspunkt?.toLocalDate(),
                        virkningFom = (it.innhold as VedtakInnhold.Behandling).virkningstidspunkt.atDay(1),
                    )
                }

        // Verifiser at hendelsene er merket som publisert
        dataSource.connection.use { conn ->
            conn.createStatement().use { stmt ->
                stmt
                    .executeQuery("SELECT COUNT(1) FROM outbox_vedtakshendelse WHERE publisert = false")
                    .single { getLong(1) } shouldBe 0
            }
        }
    }
}

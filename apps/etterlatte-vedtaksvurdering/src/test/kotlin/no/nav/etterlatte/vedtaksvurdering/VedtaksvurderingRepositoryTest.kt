package no.nav.etterlatte.vedtaksvurdering

import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.ints.shouldBeExactly
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.beInstanceOf
import no.nav.etterlatte.behandling.sakId1
import no.nav.etterlatte.behandling.sakId2
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.libs.common.Regelverk
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.beregning.AvkortetYtelseDto
import no.nav.etterlatte.libs.common.beregning.AvkortingDto
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.toObjectNode
import no.nav.etterlatte.libs.common.vedtak.Attestasjon
import no.nav.etterlatte.libs.common.vedtak.Periode
import no.nav.etterlatte.libs.common.vedtak.Utbetalingsperiode
import no.nav.etterlatte.libs.common.vedtak.UtbetalingsperiodeType
import no.nav.etterlatte.libs.common.vedtak.VedtakFattet
import no.nav.etterlatte.libs.common.vedtak.VedtakStatus
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.libs.testdata.grunnlag.SOEKER_FOEDSELSNUMMER
import no.nav.etterlatte.vedtaksvurdering.database.DatabaseExtension
import no.nav.etterlatte.vedtaksvurdering.outbox.OutboxItemType
import no.nav.etterlatte.vedtaksvurdering.outbox.OutboxRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.extension.RegisterExtension
import java.math.BigDecimal
import java.time.Month
import java.time.YearMonth
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class VedtaksvurderingRepositoryTest(
    private val dataSource: DataSource,
) {
    private lateinit var repository: VedtaksvurderingRepository

    companion object {
        @RegisterExtension
        private val dbExtension = DatabaseExtension()
    }

    @BeforeAll
    fun beforeAll() {
        repository = VedtaksvurderingRepository(dataSource)
    }

    @AfterEach
    fun afterEach() {
        dbExtension.resetDb()
    }

    @Test
    fun `skal opprette vedtak for behandling`() {
        val avkorting =
            AvkortingDto(
                avkortingGrunnlag = emptyList(),
                avkortetYtelse = emptyList(),
            ).toObjectNode()

        val nyttVedtak =
            opprettVedtak(
                avkorting = avkorting,
            )

        val vedtak = repository.opprettVedtak(nyttVedtak)

        with(vedtak) {
            vedtak shouldNotBe null
            id shouldNotBe null
            status shouldBe VedtakStatus.OPPRETTET
            soeker shouldBe SOEKER_FOEDSELSNUMMER
            sakId shouldBe sakId1
            sakType shouldBe SakType.BARNEPENSJON
            behandlingId shouldNotBe null
            type shouldBe VedtakType.INNVILGELSE
            innhold should beInstanceOf<VedtakInnhold.Behandling>()
            (innhold as VedtakInnhold.Behandling).let {
                it.behandlingType shouldBe BehandlingType.FØRSTEGANGSBEHANDLING
                it.virkningstidspunkt shouldBe YearMonth.of(2023, Month.JANUARY)
                it.vilkaarsvurdering shouldBe objectMapper.createObjectNode()
                it.beregning shouldBe objectMapper.createObjectNode()
                it.avkorting shouldBe avkorting
                it.utbetalingsperioder.first().let { utbetalingsperiode ->
                    utbetalingsperiode.id shouldNotBe null
                    utbetalingsperiode.periode shouldBe Periode(YearMonth.of(2023, Month.JANUARY), null)
                    utbetalingsperiode.beloep shouldBe BigDecimal.valueOf(100L)
                    utbetalingsperiode.type shouldBe UtbetalingsperiodeType.UTBETALING
                }
            }
        }
    }

    private fun lagAvkorting(ytelseEtterAvkorting: Int = 1000) =
        AvkortingDto(
            avkortingGrunnlag = emptyList(),
            avkortetYtelse =
                listOf(
                    AvkortetYtelseDto(
                        fom = YearMonth.of(2024, Month.JANUARY),
                        tom = YearMonth.of(2024, Month.APRIL),
                        ytelseFoerAvkorting = 1700,
                        ytelseEtterAvkorting = ytelseEtterAvkorting,
                        avkortingsbeloep = 500,
                        restanse = 0,
                        sanksjon = null,
                    ),
                    AvkortetYtelseDto(
                        fom = YearMonth.of(2024, Month.MAY),
                        tom = YearMonth.of(2024, Month.JUNE),
                        ytelseFoerAvkorting = 2500,
                        ytelseEtterAvkorting = ytelseEtterAvkorting * 2,
                        avkortingsbeloep = 500,
                        restanse = 0,
                        sanksjon = null,
                    ),
                ),
        )

    @Test
    fun `skal opprette vedtak og kunne deserialisere innholdet`() {
        val avkorting =
            lagAvkorting().toObjectNode()

        val nyttVedtak =
            opprettVedtak(
                avkorting = avkorting,
                revurderingAarsak = Revurderingaarsak.REGULERING,
            )

        val vedtak = repository.opprettVedtak(nyttVedtak)

        with(vedtak) {
            vedtak shouldNotBe null
            id shouldNotBe null
            status shouldBe VedtakStatus.OPPRETTET
            soeker shouldBe SOEKER_FOEDSELSNUMMER
            sakId shouldBe sakId1
            sakType shouldBe SakType.BARNEPENSJON
            behandlingId shouldNotBe null
            type shouldBe VedtakType.INNVILGELSE
            iverksettelsesTidspunkt shouldBe null
            innhold should beInstanceOf<VedtakInnhold.Behandling>()
            (innhold as VedtakInnhold.Behandling).let {
                it.behandlingType shouldBe BehandlingType.FØRSTEGANGSBEHANDLING
                it.virkningstidspunkt shouldBe YearMonth.of(2023, Month.JANUARY)
                it.vilkaarsvurdering shouldBe objectMapper.createObjectNode()
                it.beregning shouldBe objectMapper.createObjectNode()
                it.revurderingAarsak shouldBe Revurderingaarsak.REGULERING
                it.avkorting shouldBe avkorting
                it.utbetalingsperioder.first().let { utbetalingsperiode ->
                    utbetalingsperiode.id shouldNotBe null
                    utbetalingsperiode.periode shouldBe Periode(YearMonth.of(2023, Month.JANUARY), null)
                    utbetalingsperiode.beloep shouldBe BigDecimal.valueOf(100L)
                    utbetalingsperiode.type shouldBe UtbetalingsperiodeType.UTBETALING
                }
            }
        }
    }

    @Test
    fun `skal opprette vedtak for tilbakekreving`() {
        val nyttVedtak = opprettVedtakTilbakekreving()

        val vedtak = repository.opprettVedtak(nyttVedtak)

        with(vedtak) {
            id shouldNotBe null
            status shouldBe VedtakStatus.OPPRETTET
            soeker shouldBe SOEKER_FOEDSELSNUMMER
            sakId shouldBe sakId1
            sakType shouldBe SakType.BARNEPENSJON
            behandlingId shouldNotBe null
            type shouldBe VedtakType.TILBAKEKREVING
            innhold should beInstanceOf<VedtakInnhold.Tilbakekreving>()
            (innhold as VedtakInnhold.Tilbakekreving).tilbakekreving shouldBe objectMapper.createObjectNode()
        }
    }

    @Test
    fun `skal tilbakestille tilbakekrevingsvedtak riktig hvis de har feilet`() {
        val nyttVedtak = opprettVedtakTilbakekreving()
        val vedtak = repository.opprettVedtak(nyttVedtak)
        val vedtakFattet =
            VedtakFattet(
                ansvarligSaksbehandler = "saksbehandler",
                ansvarligEnhet = Enheter.defaultEnhet.enhetNr,
                tidspunkt = Tidspunkt.now(),
            )
        repository.fattVedtak(vedtak.behandlingId, vedtakFattet)
        repository.attesterVedtak(
            vedtak.behandlingId,
            Attestasjon(
                attestant = "attestant",
                attesterendeEnhet = Enheter.defaultEnhet.enhetNr,
                tidspunkt = Tidspunkt.now(),
            ),
        )
        assertDoesNotThrow {
            repository.tilbakestillTilbakekrevingsvedtak(vedtak.behandlingId)
        }
        val tilbakestiltVedtak = repository.hentVedtak(vedtak.behandlingId)
        tilbakestiltVedtak shouldNotBe null
        tilbakestiltVedtak?.status shouldBe VedtakStatus.FATTET_VEDTAK
        tilbakestiltVedtak?.attestasjon shouldBe null
    }

    @Test
    fun `skal opprette vedtak for klage`() {
        val nyttVedtak = opprettVedtakKlage()

        val vedtak = repository.opprettVedtak(nyttVedtak)

        with(vedtak) {
            id shouldNotBe null
            status shouldBe VedtakStatus.OPPRETTET
            soeker shouldBe SOEKER_FOEDSELSNUMMER
            sakId shouldBe sakId1
            sakType shouldBe SakType.BARNEPENSJON
            behandlingId shouldNotBe null
            type shouldBe VedtakType.AVVIST_KLAGE
            (innhold as VedtakInnhold.Klage).klage shouldBe objectMapper.createObjectNode()
        }
    }

    @Test
    fun `skal oppdatere vedtak for behandling`() {
        val nyttVedtak = opprettVedtak()

        val vedtak = repository.opprettVedtak(nyttVedtak)

        val nyttVirkningstidspunkt = YearMonth.of(2023, Month.MARCH)

        val oppdatertVedtak =
            repository.oppdaterVedtak(
                vedtak.copy(
                    type = VedtakType.OPPHOER,
                    innhold =
                        (vedtak.innhold as VedtakInnhold.Behandling).copy(
                            virkningstidspunkt = nyttVirkningstidspunkt,
                            utbetalingsperioder =
                                listOf(
                                    Utbetalingsperiode(
                                        periode = Periode(nyttVirkningstidspunkt, null),
                                        beloep = null,
                                        type = UtbetalingsperiodeType.OPPHOER,
                                        regelverk = Regelverk.fraDato(nyttVirkningstidspunkt.atDay(1)),
                                    ),
                                ),
                        ),
                ),
            )

        oppdatertVedtak shouldNotBe null
        oppdatertVedtak.type shouldBe VedtakType.OPPHOER
        oppdatertVedtak.innhold should beInstanceOf<VedtakInnhold.Behandling>()
        (oppdatertVedtak.innhold as VedtakInnhold.Behandling).let { innhold ->
            innhold.virkningstidspunkt shouldBe nyttVirkningstidspunkt
            innhold.utbetalingsperioder.first().let {
                it.id!! shouldBeGreaterThan 0
                it.periode shouldBe Periode(nyttVirkningstidspunkt, null)
                it.beloep shouldBe null
                it.type shouldBe UtbetalingsperiodeType.OPPHOER
            }
        }
    }

    @Test
    fun `skal oppdatere vedtak for tilbakekreving`() {
        val nyttVedtak = opprettVedtakTilbakekreving()
        val vedtak = repository.opprettVedtak(nyttVedtak)

        val oppdatertTilbakekreving = objectMapper.createObjectNode()
        oppdatertTilbakekreving.replace("endret", objectMapper.createObjectNode())

        val oppdatertVedtak =
            repository.oppdaterVedtak(
                vedtak.copy(
                    innhold =
                        (vedtak.innhold as VedtakInnhold.Tilbakekreving).copy(
                            tilbakekreving = oppdatertTilbakekreving,
                        ),
                ),
            )

        oppdatertVedtak shouldNotBe null
        oppdatertVedtak.type shouldBe VedtakType.TILBAKEKREVING
        oppdatertVedtak.innhold should beInstanceOf<VedtakInnhold.Tilbakekreving>()
        (oppdatertVedtak.innhold as VedtakInnhold.Tilbakekreving).tilbakekreving shouldBe oppdatertTilbakekreving
    }

    @Test
    fun `skal oppdatere vedtak for klage`() {
        val nyttVedtak = opprettVedtakKlage()
        val vedtak = repository.opprettVedtak(nyttVedtak)

        val oppdatertKlage = mapOf("endret" to "noe").toObjectNode()

        val oppdatertVedtak =
            repository.oppdaterVedtak(
                vedtak.copy(
                    innhold =
                        (vedtak.innhold as VedtakInnhold.Klage).copy(
                            klage = oppdatertKlage,
                        ),
                ),
            )

        oppdatertVedtak shouldNotBe null
        oppdatertVedtak.type shouldBe VedtakType.AVVIST_KLAGE
        (oppdatertVedtak.innhold as VedtakInnhold.Klage).klage shouldBe oppdatertKlage
    }

    @Test
    fun `skal opprette og hente vedtak`() {
        val nyttVedtak = opprettVedtak()

        repository.opprettVedtak(nyttVedtak)

        val vedtak = repository.hentVedtak(nyttVedtak.behandlingId)

        vedtak shouldNotBe null
    }

    @Test
    fun `skal fatte vedtak`() {
        val nyttVedtak = opprettVedtak()

        val vedtak = repository.opprettVedtak(nyttVedtak)

        val fattetVedtak =
            repository.fattVedtak(
                vedtak.behandlingId,
                VedtakFattet(SAKSBEHANDLER_1, ENHET_1, Tidspunkt.now()),
            )

        fattetVedtak.vedtakFattet shouldNotBe null
        fattetVedtak.status shouldBe VedtakStatus.FATTET_VEDTAK
        with(fattetVedtak.vedtakFattet!!) {
            ansvarligSaksbehandler shouldBe SAKSBEHANDLER_1
            ansvarligEnhet shouldBe ENHET_1
            tidspunkt shouldNotBe null
        }
    }

    @Test
    fun `skal attestere vedtak`() {
        val nyttVedtak = opprettVedtak()

        val vedtak = repository.opprettVedtak(nyttVedtak)

        repository.fattVedtak(
            vedtak.behandlingId,
            VedtakFattet(SAKSBEHANDLER_1, ENHET_1, Tidspunkt.now()),
        )

        val attestertVedtak =
            repository.attesterVedtak(
                vedtak.behandlingId,
                Attestasjon(SAKSBEHANDLER_2, ENHET_2, Tidspunkt.now()),
            )

        attestertVedtak.attestasjon shouldNotBe null
        attestertVedtak.status shouldBe VedtakStatus.ATTESTERT
        with(attestertVedtak.attestasjon!!) {
            attestant shouldBe SAKSBEHANDLER_2
            attesterendeEnhet shouldBe ENHET_2
            tidspunkt shouldNotBe null
        }

        with(OutboxRepository(dataSource).hentUpubliserte()) {
            size shouldBeExactly 1
            first().vedtakId shouldBe vedtak.id
            first().type shouldBe OutboxItemType.ATTESTERT
        }
    }

    @Test
    fun `skal sette vedtak iverksatt`() {
        val nyttVedtak = opprettVedtak()

        val vedtak = repository.opprettVedtak(nyttVedtak)

        repository.fattVedtak(
            vedtak.behandlingId,
            VedtakFattet(SAKSBEHANDLER_1, ENHET_1, Tidspunkt.now()),
        )

        repository.attesterVedtak(
            vedtak.behandlingId,
            Attestasjon(SAKSBEHANDLER_2, ENHET_2, Tidspunkt.now()),
        )

        val iverksattVedtak = repository.iverksattVedtak(vedtak.behandlingId)

        iverksattVedtak shouldNotBe null
        iverksattVedtak.status shouldBe VedtakStatus.IVERKSATT

        iverksattVedtak.iverksettelsesTidspunkt shouldNotBe null
    }

    @Test
    fun `skal underkjenne vedtak`() {
        val nyttVedtak = opprettVedtak()

        val vedtak = repository.opprettVedtak(nyttVedtak)

        repository.fattVedtak(
            vedtak.behandlingId,
            VedtakFattet(SAKSBEHANDLER_1, ENHET_1, Tidspunkt.now()),
        )

        val underkjentVedtak =
            repository.underkjennVedtak(
                vedtak.behandlingId,
            )

        underkjentVedtak shouldNotBe null
        underkjentVedtak.vedtakFattet shouldBe null
        underkjentVedtak.attestasjon shouldBe null
        underkjentVedtak.status shouldBe VedtakStatus.RETURNERT
    }

    @Test
    fun `skal hente vedtak for sak`() {
        val sakId = sakId1
        val nyeVedtak =
            listOf(
                opprettVedtak(sakId = sakId),
                opprettVedtak(sakId = sakId2),
                opprettVedtak(sakId = sakId),
                opprettVedtak(sakId = sakId),
            )
        nyeVedtak.forEach { repository.opprettVedtak(it) }

        val vedtakForSak = repository.hentVedtakForSak(sakId)

        vedtakForSak.size shouldBeExactly 3
        vedtakForSak.forEach { it.sakId shouldBe sakId }
    }

    @Test
    fun `skal hente vedtak for fnr`() {
        val soeker1 = Folkeregisteridentifikator.of(FNR_1)
        val soeker2 = Folkeregisteridentifikator.of(FNR_2)

        listOf(
            opprettVedtak(
                sakId = SakId(10),
                soeker = soeker1,
                virkningstidspunkt = YearMonth.of(2024, Month.JANUARY),
                status = VedtakStatus.IVERKSATT,
            ),
            opprettVedtak(
                sakId = SakId(20),
                soeker = soeker2,
                virkningstidspunkt = YearMonth.of(2024, Month.JANUARY),
                status = VedtakStatus.IVERKSATT,
            ),
            opprettVedtak(
                sakId = SakId(20),
                soeker = soeker2,
                virkningstidspunkt = YearMonth.of(2024, Month.MARCH),
                status = VedtakStatus.SAMORDNET,
            ),
            opprettVedtak(
                sakId = SakId(10),
                soeker = soeker1,
                virkningstidspunkt = YearMonth.of(2024, Month.APRIL),
                status = VedtakStatus.IVERKSATT,
            ),
            opprettVedtak(
                sakId = SakId(20),
                soeker = soeker2,
                virkningstidspunkt = YearMonth.of(2024, Month.JUNE),
                status = VedtakStatus.TIL_SAMORDNING,
            ),
        ).map { repository.opprettVedtak(it) }
            .forEach { repository.iverksattVedtak(it.behandlingId) }

        val results = repository.hentFerdigstilteVedtak(soeker2, SakType.BARNEPENSJON)

        results.size shouldBeExactly 3
        results.forEach {
            it.soeker shouldBe soeker2
        }
    }

    @Test
    fun `hent vedtak med utbetalingsperiode for aar`() {
        val soeker1 = Folkeregisteridentifikator.of(FNR_1)
        val sakid = SakId(10)

        listOf(
            opprettVedtak(
                sakId = sakid,
                soeker = soeker1,
                virkningstidspunkt = YearMonth.of(2023, Month.DECEMBER),
                status = VedtakStatus.IVERKSATT,
                sakType = SakType.OMSTILLINGSSTOENAD,
            ),
            opprettVedtak(
                sakId = sakid,
                soeker = soeker1,
                virkningstidspunkt = YearMonth.of(2024, Month.JANUARY),
                status = VedtakStatus.IVERKSATT,
                sakType = SakType.OMSTILLINGSSTOENAD,
            ),
            opprettVedtak(
                sakId = sakid,
                soeker = soeker1,
                virkningstidspunkt = YearMonth.of(2024, Month.JANUARY),
                status = VedtakStatus.FATTET_VEDTAK,
                sakType = SakType.OMSTILLINGSSTOENAD,
            ),
            opprettVedtak(
                sakId = sakid,
                soeker = soeker1,
                virkningstidspunkt = YearMonth.of(2024, Month.JANUARY),
                status = VedtakStatus.IVERKSATT,
                sakType = SakType.BARNEPENSJON,
            ),
        ).map { repository.opprettVedtak(it) }
            .forEach { repository.iverksattVedtak(it.behandlingId) }

        val results = repository.hentSakIdMedUtbetalingForInntektsaar(2023, SakType.OMSTILLINGSSTOENAD)
        results.size shouldBeExactly 1
        results.first().sakId shouldBe sakid.sakId
    }

    @Test
    fun `skal hente vedtak for fnr selv om ikke saktype er spesifisert`() {
        val soeker1 = Folkeregisteridentifikator.of(FNR_1)

        val vedtak =
            opprettVedtak(
                sakId = SakId(10),
                soeker = soeker1,
                virkningstidspunkt = YearMonth.of(2024, Month.JANUARY),
                status = VedtakStatus.IVERKSATT,
            )
        repository.opprettVedtak(vedtak)
        repository.iverksattVedtak(vedtak.behandlingId)

        val results = repository.hentFerdigstilteVedtak(soeker1)

        results.size shouldBe 1
    }
}

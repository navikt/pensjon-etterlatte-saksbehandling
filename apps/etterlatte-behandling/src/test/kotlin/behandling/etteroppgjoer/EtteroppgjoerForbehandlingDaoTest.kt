package behandling.etteroppgjoer

import io.kotest.matchers.equality.shouldBeEqualToIgnoringFields
import io.kotest.matchers.equals.shouldNotBeEqual
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.ConnectionAutoclosingTest
import no.nav.etterlatte.DatabaseExtension
import no.nav.etterlatte.User
import no.nav.etterlatte.behandling.BehandlingDao
import no.nav.etterlatte.behandling.etteroppgjoer.AInntekt
import no.nav.etterlatte.behandling.etteroppgjoer.PensjonsgivendeInntektFraSkatt
import no.nav.etterlatte.behandling.etteroppgjoer.forbehandling.EtteroppgjoerForbehandling
import no.nav.etterlatte.behandling.etteroppgjoer.forbehandling.EtteroppgjoerForbehandlingDao
import no.nav.etterlatte.behandling.etteroppgjoer.inntektskomponent.InntektBulkResponsDto
import no.nav.etterlatte.behandling.etteroppgjoer.inntektskomponent.InntektskomponentBeregning
import no.nav.etterlatte.behandling.etteroppgjoer.inntektskomponent.InntektskomponentenFilter
import no.nav.etterlatte.behandling.etteroppgjoer.inntektskomponent.SummerteInntekterAOrdningen
import no.nav.etterlatte.behandling.etteroppgjoer.inntektskomponent.inntektDto
import no.nav.etterlatte.behandling.etteroppgjoer.inntektskomponent.inntektsinformasjonDto
import no.nav.etterlatte.behandling.kommerbarnettilgode.KommerBarnetTilGodeDao
import no.nav.etterlatte.behandling.objectMapper
import no.nav.etterlatte.behandling.revurdering.RevurderingDao
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.EtteroppgjoerForbehandlingStatus
import no.nav.etterlatte.libs.common.behandling.Prosesstype
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.periode.Periode
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.nyKontekstMedBrukerOgDatabase
import no.nav.etterlatte.opprettBehandling
import no.nav.etterlatte.sak.SakSkrivDao
import no.nav.etterlatte.sak.SakendringerDao
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal
import java.time.Month
import java.time.YearMonth
import java.util.UUID
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(DatabaseExtension::class)
class EtteroppgjoerForbehandlingDaoTest(
    val dataSource: DataSource,
) {
    private lateinit var sakSkrivDao: SakSkrivDao
    private lateinit var etteroppgjoerForbehandlingDao: EtteroppgjoerForbehandlingDao
    private lateinit var behandlingRepo: BehandlingDao
    private lateinit var sak: Sak

    @BeforeAll
    fun setup() {
        val kommerBarnetTilGodeDao = KommerBarnetTilGodeDao(ConnectionAutoclosingTest(dataSource))
        val revurderingDao = RevurderingDao(ConnectionAutoclosingTest(dataSource))
        sakSkrivDao = SakSkrivDao(SakendringerDao(ConnectionAutoclosingTest(dataSource)))
        behandlingRepo = BehandlingDao(kommerBarnetTilGodeDao, revurderingDao, ConnectionAutoclosingTest(dataSource))
        etteroppgjoerForbehandlingDao = EtteroppgjoerForbehandlingDao(ConnectionAutoclosingTest(dataSource))

        nyKontekstMedBrukerOgDatabase(
            mockk<User>().also { every { it.name() } returns this::class.java.simpleName },
            dataSource,
        )
    }

    @BeforeEach
    fun resetTabell() {
        dataSource.connection.use {
            it.prepareStatement("""TRUNCATE TABLE etteroppgjoer_behandling CASCADE""").executeUpdate()
            it.prepareStatement("""TRUNCATE TABLE sak CASCADE """).executeUpdate()
        }
        sak =
            sakSkrivDao.opprettSak(
                fnr = "en bruker",
                type = SakType.OMSTILLINGSSTOENAD,
                enhet = Enheter.defaultEnhet.enhetNr,
            )
    }

    @Test
    fun `lagre og oppdatere forbehandling`() {
        val kopiertFra = UUID.randomUUID()
        val ny =
            EtteroppgjoerForbehandling(
                id = UUID.randomUUID(),
                status = EtteroppgjoerForbehandlingStatus.OPPRETTET,
                hendelseId = UUID.randomUUID(),
                aar = 2024,
                opprettet = Tidspunkt.now(),
                sak = sak,
                brevId = null,
                innvilgetPeriode = Periode(YearMonth.of(2024, 1), YearMonth.of(2024, 12)),
                kopiertFra = kopiertFra,
                sisteIverksatteBehandlingId = UUID.randomUUID(),
                harMottattNyInformasjon = null,
                endringErTilUgunstForBruker = null,
                beskrivelseAvUgunst = null,
            )

        etteroppgjoerForbehandlingDao.lagreForbehandling(ny)
        val lagret = etteroppgjoerForbehandlingDao.hentForbehandling(ny.id)
        with(lagret!!) {
            id shouldBe ny.id
            status shouldBe ny.status
            aar shouldBe ny.aar
            opprettet shouldBe ny.opprettet
            innvilgetPeriode shouldBe ny.innvilgetPeriode
            kopiertFra shouldBe kopiertFra
        }
    }

    @Test
    fun `hent forbehandlinger`() {
        etteroppgjoerForbehandlingDao.lagreForbehandling(
            EtteroppgjoerForbehandling(
                id = UUID.randomUUID(),
                status = EtteroppgjoerForbehandlingStatus.OPPRETTET,
                hendelseId = UUID.randomUUID(),
                aar = 2024,
                opprettet = Tidspunkt.now(),
                sak = sak,
                brevId = null,
                innvilgetPeriode = Periode(YearMonth.of(2024, 1), YearMonth.of(2024, 12)),
                kopiertFra = UUID.randomUUID(),
                sisteIverksatteBehandlingId = UUID.randomUUID(),
                harMottattNyInformasjon = null,
                endringErTilUgunstForBruker = null,
                beskrivelseAvUgunst = null,
            ),
        )
        etteroppgjoerForbehandlingDao.lagreForbehandling(
            EtteroppgjoerForbehandling(
                id = UUID.randomUUID(),
                status = EtteroppgjoerForbehandlingStatus.OPPRETTET,
                hendelseId = UUID.randomUUID(),
                aar = 2024,
                opprettet = Tidspunkt.now(),
                sak = sak,
                brevId = null,
                innvilgetPeriode = Periode(YearMonth.of(2024, 1), YearMonth.of(2024, 12)),
                kopiertFra = null,
                sisteIverksatteBehandlingId = UUID.randomUUID(),
                harMottattNyInformasjon = null,
                endringErTilUgunstForBruker = null,
                beskrivelseAvUgunst = null,
            ),
        )

        with(etteroppgjoerForbehandlingDao.hentForbehandlinger(sak.id)) {
            size shouldBe 2
        }
    }

    @Test
    fun `oppdater relatert behandling id`() {
        val relatertForbehandlingId = UUID.randomUUID()

        val sak1 = sakSkrivDao.opprettSak("123", SakType.BARNEPENSJON, Enheter.defaultEnhet.enhetNr).id
        val opprettBehandling =
            opprettBehandling(
                type = BehandlingType.REVURDERING,
                sakId = sak1,
                revurderingAarsak = Revurderingaarsak.ETTEROPPGJOER,
                prosesstype = Prosesstype.MANUELL,
                relatertBehandlingId = relatertForbehandlingId.toString(),
            )
        behandlingRepo.opprettBehandling(opprettBehandling)

        val nyRelatertForbehandlingId = UUID.randomUUID()
        etteroppgjoerForbehandlingDao.oppdaterRelatertBehandling(relatertForbehandlingId, nyRelatertForbehandlingId)

        behandlingRepo.hentBehandling(opprettBehandling.id)!!.relatertBehandlingId shouldBe nyRelatertForbehandlingId.toString()
    }

    @Test
    fun `lagre og hente pensjonsgivendeInntekt`() {
        val inntektsaar = 2024
        val forbehandlingId = UUID.randomUUID()

        // negative returnere null hvis tomt
        etteroppgjoerForbehandlingDao.hentPensjonsgivendeInntekt(forbehandlingId) shouldBe null

        etteroppgjoerForbehandlingDao.lagrePensjonsgivendeInntekt(
            PensjonsgivendeInntektFraSkatt.stub(inntektsaar),
            forbehandlingId,
        )

        with(etteroppgjoerForbehandlingDao.hentPensjonsgivendeInntekt(forbehandlingId)!!) {
            this.inntektsaar shouldBe inntektsaar
            inntekter shouldBe PensjonsgivendeInntektFraSkatt.stub(inntektsaar).inntekter
        }
    }

    @Test
    fun `lagre og hente aInntekt`() {
        val inntektsaar = 2024
        val forbehandlingId = UUID.randomUUID()

        // negative returnere null hvis tomt
        etteroppgjoerForbehandlingDao.hentAInntekt(forbehandlingId) shouldBe null

        etteroppgjoerForbehandlingDao.lagreAInntekt(
            AInntekt.stub(inntektsaar),
            forbehandlingId,
        )

        with(etteroppgjoerForbehandlingDao.hentAInntekt(forbehandlingId)!!) {
            inntektsaar shouldBe inntektsaar
            inntektsmaaneder shouldBe AInntekt.stub(inntektsaar).inntektsmaaneder
        }
    }

    @Test
    fun `kan lagre ned og hente ut summerte inntekter for forbehandling`() {
        val afp =
            InntektskomponentBeregning.beregnInntekt(
                InntektBulkResponsDto(
                    filter = "test",
                    data =
                        listOf(
                            inntektsinformasjonDto(
                                maaned = YearMonth.of(2024, Month.JANUARY),
                                inntektListe =
                                    listOf(
                                        inntektDto(BigDecimal.ONE),
                                    ),
                            ),
                        ),
                ),
                2024,
            )
        val loenn = InntektskomponentBeregning.beregnInntekt(InntektBulkResponsDto("a", emptyList()), 2024)
        val oms = InntektskomponentBeregning.beregnInntekt(InntektBulkResponsDto("a", emptyList()), 2024)

        val summerteInntekterAOrdningen =
            SummerteInntekterAOrdningen(
                afp = afp.verdi,
                loenn = loenn.verdi,
                oms = oms.verdi,
                tidspunktBeregnet = Tidspunkt(oms.opprettet),
                regelresultat =
                    mapOf(
                        InntektskomponentenFilter.ETTEROPPGJOER_AFP to objectMapper.valueToTree(afp),
                        InntektskomponentenFilter.ETTEROPPGJOER_LOENN to objectMapper.valueToTree(loenn),
                        InntektskomponentenFilter.ETTEROPPGJOER_OMS to objectMapper.valueToTree(oms),
                    ),
            )
        val ny =
            EtteroppgjoerForbehandling(
                id = UUID.randomUUID(),
                status = EtteroppgjoerForbehandlingStatus.OPPRETTET,
                hendelseId = UUID.randomUUID(),
                aar = 2024,
                opprettet = Tidspunkt.now(),
                sak = sak,
                brevId = null,
                innvilgetPeriode = Periode(YearMonth.of(2024, 1), YearMonth.of(2024, 12)),
                kopiertFra = null,
                sisteIverksatteBehandlingId = UUID.randomUUID(),
                harMottattNyInformasjon = null,
                endringErTilUgunstForBruker = null,
                beskrivelseAvUgunst = null,
            )
        etteroppgjoerForbehandlingDao.lagreForbehandling(ny)
        etteroppgjoerForbehandlingDao.lagreSummerteInntekter(ny.id, null, summerteInntekterAOrdningen)

        val hentetInntekter = etteroppgjoerForbehandlingDao.hentSummerteInntekter(ny.id, null)
        summerteInntekterAOrdningen.shouldBeEqualToIgnoringFields(hentetInntekter, SummerteInntekterAOrdningen::regelresultat)
    }

    @Test
    fun `kan oppdatere inntekter for forbehandling`() {
        val ny =
            EtteroppgjoerForbehandling(
                id = UUID.randomUUID(),
                status = EtteroppgjoerForbehandlingStatus.OPPRETTET,
                hendelseId = UUID.randomUUID(),
                aar = 2024,
                opprettet = Tidspunkt.now(),
                sak = sak,
                brevId = null,
                innvilgetPeriode = Periode(YearMonth.of(2024, 1), YearMonth.of(2024, 12)),
                kopiertFra = null,
                sisteIverksatteBehandlingId = UUID.randomUUID(),
                harMottattNyInformasjon = null,
                endringErTilUgunstForBruker = null,
                beskrivelseAvUgunst = null,
            )
        etteroppgjoerForbehandlingDao.lagreForbehandling(ny)
        val afp =
            InntektskomponentBeregning.beregnInntekt(
                InntektBulkResponsDto(
                    filter = "test",
                    data =
                        listOf(
                            inntektsinformasjonDto(
                                maaned = YearMonth.of(2024, Month.JANUARY),
                                inntektListe =
                                    listOf(
                                        inntektDto(BigDecimal.ONE),
                                    ),
                            ),
                        ),
                ),
                2024,
            )
        val loenn = InntektskomponentBeregning.beregnInntekt(InntektBulkResponsDto("a", emptyList()), 2024)
        val oms = InntektskomponentBeregning.beregnInntekt(InntektBulkResponsDto("a", emptyList()), 2024)

        val summerteInntekterAOrdningenEn =
            SummerteInntekterAOrdningen(
                afp = afp.verdi,
                loenn = loenn.verdi,
                oms = oms.verdi,
                tidspunktBeregnet = Tidspunkt(oms.opprettet),
                regelresultat =
                    mapOf(
                        InntektskomponentenFilter.ETTEROPPGJOER_AFP to objectMapper.valueToTree(afp),
                        InntektskomponentenFilter.ETTEROPPGJOER_LOENN to objectMapper.valueToTree(loenn),
                        InntektskomponentenFilter.ETTEROPPGJOER_OMS to objectMapper.valueToTree(oms),
                    ),
            )

        val summerteInntekterAOrdningenTo =
            SummerteInntekterAOrdningen(
                afp = afp.verdi,
                loenn = afp.verdi,
                oms = afp.verdi,
                tidspunktBeregnet = Tidspunkt(oms.opprettet),
                regelresultat =
                    mapOf(
                        InntektskomponentenFilter.ETTEROPPGJOER_AFP to objectMapper.valueToTree(afp),
                        InntektskomponentenFilter.ETTEROPPGJOER_LOENN to objectMapper.valueToTree(loenn),
                        InntektskomponentenFilter.ETTEROPPGJOER_OMS to objectMapper.valueToTree(oms),
                    ),
            )

        etteroppgjoerForbehandlingDao.lagreSummerteInntekter(ny.id, null, summerteInntekterAOrdningenEn)
        val hentetInntekterEn = etteroppgjoerForbehandlingDao.hentSummerteInntekter(ny.id, null)
        summerteInntekterAOrdningenEn.shouldBeEqualToIgnoringFields(hentetInntekterEn, SummerteInntekterAOrdningen::regelresultat)

        etteroppgjoerForbehandlingDao.lagreSummerteInntekter(ny.id, null, summerteInntekterAOrdningenTo)
        val hentetInntekterTo = etteroppgjoerForbehandlingDao.hentSummerteInntekter(ny.id, null)
        summerteInntekterAOrdningenTo.shouldBeEqualToIgnoringFields(hentetInntekterTo, SummerteInntekterAOrdningen::regelresultat)
        hentetInntekterEn shouldNotBeEqual hentetInntekterTo
    }

    @Test
    fun `kopier aInntekt til ny forbehandling`() {
        val inntektsaar = 2024
        val forbehandlingId = UUID.randomUUID()
        val nyForbehandlingId = UUID.randomUUID()
        etteroppgjoerForbehandlingDao.hentAInntekt(forbehandlingId) shouldBe null
        etteroppgjoerForbehandlingDao.hentAInntekt(nyForbehandlingId) shouldBe null
        etteroppgjoerForbehandlingDao.lagreAInntekt(
            AInntekt.stub(inntektsaar),
            forbehandlingId,
        )

        etteroppgjoerForbehandlingDao.kopierAInntekt(forbehandlingId, nyForbehandlingId)

        val aInntekt = etteroppgjoerForbehandlingDao.hentAInntekt(forbehandlingId)!!
        val aInntektKopi = etteroppgjoerForbehandlingDao.hentAInntekt(nyForbehandlingId)!!

        aInntekt.inntektsmaaneder shouldBe aInntektKopi.inntektsmaaneder
        aInntekt.aar shouldBe aInntektKopi.aar
    }

    @Test
    fun `kopier pensjonsgivendeInntekt til ny forbehandling`() {
        val inntektsaar = 2024
        val forbehandlingId = UUID.randomUUID()
        val nyForbehandlingId = UUID.randomUUID()

        etteroppgjoerForbehandlingDao.hentPensjonsgivendeInntekt(forbehandlingId) shouldBe null
        etteroppgjoerForbehandlingDao.hentPensjonsgivendeInntekt(nyForbehandlingId) shouldBe null
        etteroppgjoerForbehandlingDao.lagrePensjonsgivendeInntekt(
            PensjonsgivendeInntektFraSkatt.stub(inntektsaar),
            forbehandlingId,
        )

        etteroppgjoerForbehandlingDao.kopierPensjonsgivendeInntekt(forbehandlingId, nyForbehandlingId)

        val pensjonsgivendeInntekt = etteroppgjoerForbehandlingDao.hentPensjonsgivendeInntekt(forbehandlingId)!!
        val pensjonsgivendeInntektKopi = etteroppgjoerForbehandlingDao.hentPensjonsgivendeInntekt(nyForbehandlingId)!!

        pensjonsgivendeInntekt.inntekter shouldBe pensjonsgivendeInntektKopi.inntekter
        pensjonsgivendeInntekt.inntektsaar shouldBe pensjonsgivendeInntektKopi.inntektsaar
    }
}

package behandling.etteroppgjoer

import com.fasterxml.jackson.databind.node.TextNode
import io.kotest.matchers.equality.shouldBeEqualToIgnoringFields
import io.kotest.matchers.equals.shouldNotBeEqual
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.ConnectionAutoclosingTest
import no.nav.etterlatte.DatabaseExtension
import no.nav.etterlatte.User
import no.nav.etterlatte.behandling.BehandlingDao
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
import no.nav.etterlatte.libs.common.behandling.Prosesstype
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.etteroppgjoer.AarsakTilAvbryteForbehandling
import no.nav.etterlatte.libs.common.behandling.etteroppgjoer.EtteroppgjoerForbehandlingStatus
import no.nav.etterlatte.libs.common.behandling.etteroppgjoer.InntektSummert
import no.nav.etterlatte.libs.common.behandling.etteroppgjoer.Inntektsmaaned
import no.nav.etterlatte.libs.common.beregning.EtteroppgjoerResultatType
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
import java.time.LocalDate
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
        val dato = LocalDate.now()
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
                varselbrevSendt = dato,
                etteroppgjoerResultatType = EtteroppgjoerResultatType.ETTERBETALING,
            )

        etteroppgjoerForbehandlingDao.lagreForbehandling(ny.copy())
        val lagret = etteroppgjoerForbehandlingDao.hentForbehandling(ny.id)
        with(lagret!!) {
            id shouldBe ny.id
            status shouldBe ny.status
            aar shouldBe ny.aar
            opprettet shouldBe ny.opprettet
            innvilgetPeriode shouldBe ny.innvilgetPeriode
            kopiertFra shouldBe kopiertFra
            varselbrevSendt shouldBe dato
        }
    }

    @Test
    fun `lagre med varselbrev sendt`() {
        val forbehandling =
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
                varselbrevSendt = null,
            )
        etteroppgjoerForbehandlingDao.lagreForbehandling(forbehandling)
        etteroppgjoerForbehandlingDao.hentForbehandling(forbehandling.id)!!.varselbrevSendt shouldBe null
        etteroppgjoerForbehandlingDao.lagreForbehandling(forbehandling.medVarselbrevSendt())
        etteroppgjoerForbehandlingDao.hentForbehandling(forbehandling.id)!!.varselbrevSendt shouldNotBe null
    }

    @Test
    fun `hent forbehandlinger`() {
        opprettForbehandling(UUID.randomUUID())
        opprettForbehandling(UUID.randomUUID())

        with(etteroppgjoerForbehandlingDao.hentForbehandlinger(sak.id)) {
            size shouldBe 2
            forEach {
                it.aarsakTilAvbrytelse shouldBe AarsakTilAvbryteForbehandling.FEILREGISTRERT
            }
        }
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
                varselbrevSendt = null,
            )
        etteroppgjoerForbehandlingDao.lagreForbehandling(ny)
        etteroppgjoerForbehandlingDao.lagreSummerteInntekter(ny.id, summerteInntekterAOrdningen)

        val hentetInntekter = etteroppgjoerForbehandlingDao.hentSummerteInntekterNonNull(ny.id)
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
                varselbrevSendt = null,
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

        etteroppgjoerForbehandlingDao.lagreSummerteInntekter(ny.id, summerteInntekterAOrdningenEn)
        val hentetInntekterEn = etteroppgjoerForbehandlingDao.hentSummerteInntekterNonNull(ny.id)
        summerteInntekterAOrdningenEn.shouldBeEqualToIgnoringFields(hentetInntekterEn, SummerteInntekterAOrdningen::regelresultat)

        etteroppgjoerForbehandlingDao.lagreSummerteInntekter(ny.id, summerteInntekterAOrdningenTo)
        val hentetInntekterTo = etteroppgjoerForbehandlingDao.hentSummerteInntekterNonNull(ny.id)
        summerteInntekterAOrdningenTo.shouldBeEqualToIgnoringFields(hentetInntekterTo, SummerteInntekterAOrdningen::regelresultat)
        hentetInntekterEn shouldNotBeEqual hentetInntekterTo
    }

    @Test
    fun `kopier summerte inntekter til ny forbehandling`() {
        val inntektsaar = 2024
        val forbehandlingId = UUID.randomUUID()
        val nyForbehandlingId = UUID.randomUUID()
        opprettForbehandling(forbehandlingId)

        etteroppgjoerForbehandlingDao.hentSummerteInntekter(forbehandlingId) shouldBe null
        etteroppgjoerForbehandlingDao.lagreSummerteInntekter(
            forbehandlingId,
            SummerteInntekterAOrdningen(
                afp = InntektSummert("A", tolvMndInntekter(2024, 5000.toBigDecimal())),
                loenn = InntektSummert("B", tolvMndInntekter(2024, 6000.toBigDecimal())),
                oms = InntektSummert("C", tolvMndInntekter(2024, 7000.toBigDecimal())),
                tidspunktBeregnet = Tidspunkt.now(),
                regelresultat =
                    mapOf(
                        InntektskomponentenFilter.ETTEROPPGJOER_LOENN to TextNode("test"),
                        InntektskomponentenFilter.ETTEROPPGJOER_AFP to TextNode("test"),
                        InntektskomponentenFilter.ETTEROPPGJOER_OMS to TextNode("test"),
                    ),
            ),
        )
        opprettForbehandling(nyForbehandlingId)

        etteroppgjoerForbehandlingDao.kopierSummerteInntekter(forbehandlingId, nyForbehandlingId)

        val summerteInntekter = etteroppgjoerForbehandlingDao.hentSummerteInntekterNonNull(forbehandlingId)
        val summerteInntekterKopi = etteroppgjoerForbehandlingDao.hentSummerteInntekterNonNull(nyForbehandlingId)

        summerteInntekterKopi.loenn shouldBe summerteInntekter.loenn
        summerteInntekterKopi.afp shouldBe summerteInntekter.afp
        summerteInntekterKopi.oms shouldBe summerteInntekter.oms
        summerteInntekterKopi.regelresultat shouldBe summerteInntekter.regelresultat
        summerteInntekterKopi.tidspunktBeregnet shouldBe summerteInntekter.tidspunktBeregnet
    }

    private fun opprettForbehandling(forbehandlingId: UUID) {
        etteroppgjoerForbehandlingDao.lagreForbehandling(
            EtteroppgjoerForbehandling(
                id = forbehandlingId,
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
                varselbrevSendt = null,
                aarsakTilAvbrytelse = AarsakTilAvbryteForbehandling.FEILREGISTRERT,
                aarsakTilAvbrytelseBeskrivelse = "test",
            ),
        )
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

    private fun tolvMndInntekter(
        aar: Int,
        beloep: BigDecimal,
    ): List<Inntektsmaaned> =
        (1..12).map { mnd ->
            Inntektsmaaned(YearMonth.of(aar, mnd), beloep)
        }
}

package no.nav.etterlatte.trygdetid.avtale

import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import no.nav.etterlatte.libs.common.trygdetid.avtale.Trygdeavtale
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import trygdetid.trygdeavtale
import java.time.LocalDate
import java.time.Month
import java.util.UUID.randomUUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class AvtaleServiceTest {
    private val repository = mockk<AvtaleRepository>()
    private val service = AvtaleService(repository)

    @BeforeEach
    fun beforeEach() {
        clearAllMocks()
    }

    @AfterEach
    fun afterEach() {
        confirmVerified()
    }

    @Test
    fun `skal levere informasjon om avtaler`() {
        val avtaler = service.hentAvtaler()

        avtaler.size shouldBe 26
        with(avtaler.first { it.kode == "EOS_NOR" }) {
            this.beskrivelse shouldBe "Eøs-Avtalen/Nordisk Konvensjon"
            this.fraDato shouldBe LocalDate.of(1899, Month.DECEMBER, 31)
        }

        with(avtaler.first { it.kode == "BIH" }) {
            this.beskrivelse shouldBe "Bosnia-Hercegovina"
            this.fraDato shouldBe LocalDate.of(1899, Month.DECEMBER, 31)

            val trygdetidDato = this.datoer.first()

            trygdetidDato.kode shouldBe "BIH1992"
            trygdetidDato.beskrivelse shouldBe "01.03.1992"
            trygdetidDato.fraDato shouldBe LocalDate.of(1899, Month.DECEMBER, 31)
        }
    }

    @Test
    fun `skal levere informasjon om avtale kriterier`() {
        val avtaler = service.hentAvtaleKriterier()

        avtaler.size shouldBe 7
        with(avtaler.first { it.kode == "YRK_MEDL" }) {
            this.beskrivelse shouldBe "Yrkesaktiv i Norge eller EØS, ett års medlemskap i Norge"
            this.fraDato shouldBe LocalDate.of(1899, Month.DECEMBER, 31)
        }
    }

    @Test
    fun `skal kunne hente en avtale`() {
        val behandlingId = randomUUID()

        every { repository.hentAvtale(any()) } returns trygdeavtale(behandlingId, "TEST")

        val avtale = service.hentAvtaleForBehandling(behandlingId)

        avtale?.avtaleKode shouldBe "TEST"

        verify(exactly = 1) { repository.hentAvtale(any()) }
    }

    @Test
    fun `skal opprette avtaler`() {
        val avtaleSlot = slot<Trygdeavtale>()
        val behandlingId = randomUUID()

        every { repository.opprettAvtale(capture(avtaleSlot)) } just runs

        service.opprettAvtale(
            trygdeavtale(behandlingId = behandlingId, avtaleKode = "TEST", avtaleDatoKode = "TESTDATO"),
        )

        avtaleSlot.captured.avtaleKode shouldBe "TEST"
        avtaleSlot.captured.avtaleDatoKode shouldBe "TESTDATO"
        avtaleSlot.captured.behandlingId shouldBe behandlingId

        verify(exactly = 1) { repository.opprettAvtale(any()) }
    }

    @Test
    fun `skal oppdatere avtaler`() {
        val avtaleSlot = slot<Trygdeavtale>()
        val behandlingId = randomUUID()

        every { repository.lagreAvtale(capture(avtaleSlot)) } just runs

        service.lagreAvtale(
            trygdeavtale(behandlingId = behandlingId, avtaleKode = "TEST", avtaleKriteriaKode = "TESTKRITERIA"),
        )

        avtaleSlot.captured.avtaleKode shouldBe "TEST"
        avtaleSlot.captured.avtaleKriteriaKode shouldBe "TESTKRITERIA"
        avtaleSlot.captured.behandlingId shouldBe behandlingId

        verify(exactly = 1) { repository.lagreAvtale(any()) }
    }
}

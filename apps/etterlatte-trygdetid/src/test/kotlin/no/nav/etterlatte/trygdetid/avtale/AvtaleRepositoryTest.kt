package no.nav.etterlatte.trygdetid.avtale

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.JaNei
import no.nav.etterlatte.trygdetid.DatabaseExtension
import no.nav.etterlatte.trygdetid.trygdeavtale
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.RegisterExtension
import java.util.UUID.randomUUID
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class AvtaleRepositoryTest(
    dataSource: DataSource,
) {
    companion object {
        @RegisterExtension
        val dbExtension = DatabaseExtension()
    }

    private val repository = AvtaleRepository(dataSource)

    @AfterEach
    fun afterEach() {
        dbExtension.resetDb()
    }

    @Test
    fun `skal opprette og hente avtaler`() {
        val behandling = behandlingMock()
        val avtale = trygdeavtale(behandling.id, "EOS_NOR", "EOS2010", "YRK_MEDL")

        repository.opprettAvtale(avtale)

        repository.hentAvtale(behandling.id) shouldBe avtale
    }

    @Test
    fun `skal oppdatere avtaler`() {
        val behandling = behandlingMock()
        val avtale =
            trygdeavtale(
                behandling.id,
                "EOS_NOR",
                "EOS2010",
                "YRK_MEDL",
                JaNei.JA,
                JaNei.JA,
                "hei",
                JaNei.NEI,
                "hei",
                JaNei.NEI,
                "hei igjen",
            )

        repository.opprettAvtale(avtale)

        val oppdatertAvtale =
            repository.hentAvtale(behandling.id)!!.copy(
                avtaleKode = "ISR",
                avtaleDatoKode = null,
                avtaleKriteriaKode = "YRK_TRYGD",
                personKrets = JaNei.JA,
                arbInntekt1G = null,
                arbInntekt1GKommentar = null,
                beregArt50 = JaNei.JA,
                beregArt50Kommentar = null,
                nordiskTrygdeAvtale = JaNei.NEI,
                nordiskTrygdeAvtaleKommentar = null,
            )

        repository.lagreAvtale(oppdatertAvtale)

        repository.hentAvtale(behandling.id) shouldBe oppdatertAvtale
    }

    private fun behandlingMock() =
        mockk<DetaljertBehandling>().apply {
            every { id } returns randomUUID()
            every { sak } returns 123L
        }
}

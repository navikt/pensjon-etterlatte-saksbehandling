package avkorting

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.avkorting.Avkorting
import no.nav.etterlatte.avkorting.AvkortingReparerAarsoppgjoeret
import no.nav.etterlatte.avkorting.AvkortingRepository
import no.nav.etterlatte.avkorting.NyeAarMedInntektMaaStarteIJanuar
import no.nav.etterlatte.beregning.regler.aarsoppgjoer
import no.nav.etterlatte.libs.common.vedtak.VedtakSammendragDto
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID

class AvkortingReparerAarsoppgjoeretTest {
    private val repo = mockk<AvkortingRepository>()
    private val service = AvkortingReparerAarsoppgjoeret(repo)

    @Test
    fun `hvis det ikke mangler tidligere aarsoppgjoer paa forrige avkorting skal den returneres`() {
        val forrigeAvkorting =
            mockk<Avkorting> {
                every { aarsoppgjoer } returns
                    listOf(
                        aarsoppgjoer(aar = 2024),
                        aarsoppgjoer(aar = 2025),
                    )
            }
        val virk = YearMonth.of(2024, 12)
        val alleVedtak = emptyList<VedtakSammendragDto>()

        every { repo.hentAlleAarsoppgjoer(alleVedtak.map { it.behandlingId }) } returns
            listOf(
                aarsoppgjoer(aar = 2024),
                aarsoppgjoer(aar = 2025),
            )

        val reparertAvkorting =
            service.hentSisteAvkortingMedReparertAarsoppgjoer(
                forrigeAvkorting,
                virk,
                alleVedtak,
            )

        reparertAvkorting shouldBe forrigeAvkorting
    }

    @Test
    fun `hvis det mangler aarsoppgjoer men det er fordi nytt aarsoppgjoer er et nytt aar skal forrige avkorting returneres`() {
        val forrigeAvkorting =
            mockk<Avkorting> {
                every { aarsoppgjoer } returns
                    listOf(
                        aarsoppgjoer(aar = 2024),
                    )
            }
        val virk = YearMonth.of(2025, 1)
        val alleVedtak =
            listOf(
                vedtakSammendragDto(
                    virk = YearMonth.of(2024, 6),
                    datoAttestert = YearMonth.of(2024, 6),
                ),
            )
        every { repo.hentAlleAarsoppgjoer(alleVedtak.map { it.behandlingId }) } returns
            listOf(
                aarsoppgjoer(aar = 2024),
                aarsoppgjoer(aar = 2025),
            )

        val reparertAvkorting =
            service.hentSisteAvkortingMedReparertAarsoppgjoer(
                forrigeAvkorting,
                virk,
                alleVedtak,
            )

        reparertAvkorting shouldBe forrigeAvkorting
    }

    @Test
    fun `hvis det mangler aarsoppgjoer men det er fordi nytt aarsoppgjoer er et nytt aar skal det feile hvis virk ikke er januar`() {
        val forrigeAvkorting =
            mockk<Avkorting> {
                every { aarsoppgjoer } returns
                    listOf(
                        aarsoppgjoer(aar = 2024),
                    )
            }
        val virk = YearMonth.of(2025, 2)
        val alleVedtak =
            listOf(
                vedtakSammendragDto(
                    virk = YearMonth.of(2024, 1),
                    datoAttestert = YearMonth.of(2024, 1),
                ),
            )

        every { repo.hentAlleAarsoppgjoer(alleVedtak.map { it.behandlingId }) } returns
            listOf(
                aarsoppgjoer(aar = 2024),
                aarsoppgjoer(aar = 2025),
            )

        assertThrows<NyeAarMedInntektMaaStarteIJanuar> {
            service.hentSisteAvkortingMedReparertAarsoppgjoer(
                forrigeAvkorting,
                virk,
                alleVedtak,
            )
        }
    }

    @Test
    fun `hvis det mangler aarsoppgjoer for aar samme som virk skal det tidligere avkorting hentes for kopiering`() {
        val forrigeAvkorting =
            Avkorting(
                aarsoppgjoer = listOf(aarsoppgjoer(aar = 2025)),
            )
        val virk = YearMonth.of(2024, 11)

        val sistebehandlingIdManglendeAar = UUID.randomUUID()
        val sisteAvkortingMangelndeAar =
            Avkorting(
                aarsoppgjoer =
                    listOf(
                        aarsoppgjoer(aar = 2024),
                    ),
            )

        val alleVedtak =
            listOf(
                vedtakSammendragDto(virk = YearMonth.of(2025, 1), datoAttestert = YearMonth.of(2024, 11)),
                vedtakSammendragDto(
                    behandlingId = sistebehandlingIdManglendeAar,
                    virk = YearMonth.of(2024, 10),
                    datoAttestert = YearMonth.of(2024, 9),
                ),
                vedtakSammendragDto(
                    type = VedtakType.INNVILGELSE,
                    virk = YearMonth.of(2024, 3),
                    datoAttestert = YearMonth.of(2024, 2),
                ),
            )

        every { repo.hentAlleAarsoppgjoer(alleVedtak.map { it.behandlingId }) } returns
            listOf(
                aarsoppgjoer(aar = 2024),
                aarsoppgjoer(aar = 2025),
            )
        every { repo.hentAvkorting(sistebehandlingIdManglendeAar) } returns sisteAvkortingMangelndeAar

        val reparertAvkorting =
            service.hentSisteAvkortingMedReparertAarsoppgjoer(
                forrigeAvkorting,
                virk,
                alleVedtak,
            )

        reparertAvkorting.aarsoppgjoer.size shouldBe 2
        reparertAvkorting.aarsoppgjoer[0].aar shouldBe 2024
        reparertAvkorting.aarsoppgjoer[1].aar shouldBe 2025
    }

    @Test
    fun `hvis det mangler aarsoppgjoer for tidligere aar enn virk skal disse hentes og legges til`() {
        val forrigeAvkorting =
            Avkorting(
                aarsoppgjoer = listOf(aarsoppgjoer(aar = 2025)),
            )
        val virk = YearMonth.of(2025, 2)

        val sistebehandlingIdManglendeAar = UUID.randomUUID()
        val sisteAvkortingManglendeAar =
            Avkorting(
                aarsoppgjoer =
                    listOf(
                        aarsoppgjoer(aar = 2024),
                    ),
            )

        val alleVedtak =
            listOf(
                vedtakSammendragDto(virk = YearMonth.of(2025, 1), datoAttestert = YearMonth.of(2024, 11)),
                vedtakSammendragDto(
                    behandlingId = sistebehandlingIdManglendeAar,
                    virk = YearMonth.of(2024, 10),
                    datoAttestert = YearMonth.of(2024, 9),
                ),
                vedtakSammendragDto(
                    type = VedtakType.INNVILGELSE,
                    virk = YearMonth.of(2024, 3),
                    datoAttestert = YearMonth.of(2024, 2),
                ),
            )

        every { repo.hentAlleAarsoppgjoer(alleVedtak.map { it.behandlingId }) } returns
            listOf(
                aarsoppgjoer(aar = 2024),
                aarsoppgjoer(aar = 2025),
            )
        every { repo.hentAvkorting(sistebehandlingIdManglendeAar) } returns sisteAvkortingManglendeAar

        val reparertAvkorting =
            service.hentSisteAvkortingMedReparertAarsoppgjoer(
                forrigeAvkorting,
                virk,
                alleVedtak,
            )

        reparertAvkorting.aarsoppgjoer.size shouldBe 2
        reparertAvkorting.aarsoppgjoer[0].aar shouldBe 2024
        reparertAvkorting.aarsoppgjoer[1].aar shouldBe 2025
    }

    @Test
    fun `skal hente avkorting for sist iverksatte`() {
        val forrigeAvkorting =
            Avkorting(
                aarsoppgjoer = listOf(aarsoppgjoer(aar = 2025)),
            )

        val sistebehandlingIdManglendeAar = UUID.randomUUID()
        val sisteAvkortingManglendeAar =
            Avkorting(
                aarsoppgjoer =
                    listOf(
                        aarsoppgjoer(aar = 2024),
                    ),
            )

        val alleVedtak =
            listOf(
                vedtakSammendragDto(virk = YearMonth.of(2025, 1), datoAttestert = YearMonth.of(2024, 11)),
                vedtakSammendragDto(
                    behandlingId = sistebehandlingIdManglendeAar,
                    virk = YearMonth.of(2024, 10),
                    datoAttestert = YearMonth.of(2024, 9),
                ),
                vedtakSammendragDto(
                    type = VedtakType.INNVILGELSE,
                    virk = YearMonth.of(2024, 3),
                    datoAttestert = YearMonth.of(2024, 2),
                ),
            )

        every { repo.hentAlleAarsoppgjoer(alleVedtak.map { it.behandlingId }) } returns
            listOf(
                aarsoppgjoer(aar = 2024),
                aarsoppgjoer(aar = 2025),
            )
        every { repo.hentAvkorting(sistebehandlingIdManglendeAar) } returns sisteAvkortingManglendeAar

        val reparertAvkorting =
            service.hentAvkortingForSistIverksattMedReparertAarsoppgjoer(
                alleVedtak,
                forrigeAvkorting,
            )

        reparertAvkorting.aarsoppgjoer.size shouldBe 2
        reparertAvkorting.aarsoppgjoer[0].aar shouldBe 2024
        reparertAvkorting.aarsoppgjoer[1].aar shouldBe 2025
    }

    companion object {
        fun vedtakSammendragDto(
            behandlingId: UUID = UUID.randomUUID(),
            type: VedtakType = VedtakType.ENDRING,
            datoAttestert: YearMonth,
            virk: YearMonth,
        ) = VedtakSammendragDto(
            id = "id",
            behandlingId = behandlingId,
            vedtakType = type,
            behandlendeSaksbehandler = null,
            datoFattet = null,
            attesterendeSaksbehandler = null,
            datoAttestert =
                ZonedDateTime.of(
                    datoAttestert.year,
                    datoAttestert.monthValue,
                    1,
                    1,
                    1,
                    1,
                    1,
                    ZoneId.of("Europe/Oslo"),
                ),
            virkningstidspunkt = virk,
            opphoerFraOgMed = null,
        )
    }
}

package no.nav.etterlatte.brev.model

import io.mockk.mockk
import no.nav.etterlatte.brev.behandling.Behandling
import no.nav.etterlatte.brev.behandling.ForenkletVedtak
import no.nav.etterlatte.libs.common.behandling.Navn
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import no.nav.etterlatte.libs.common.behandling.RevurderingInfo
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.deserialize
import no.nav.etterlatte.libs.common.vedtak.VedtakStatus
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.util.*

class SlateFletterTest {
    @Test
    fun fletterInnFlettefelt() {
        val originalJson = this.javaClass.getResource("/maler/bp-revurdering-omgjoering-av-farskap.json")!!.readText()

        val deserialized = deserialize<Slate>(originalJson)
        val behandling = Behandling(
            sakId = 1L,
            sakType = SakType.BARNEPENSJON,
            behandlingId = UUID.randomUUID(),
            spraak = Spraak.NB,
            persongalleri = mockk(),
            vedtak = ForenkletVedtak(
                id = 2L,
                status = VedtakStatus.IVERKSATT,
                type = VedtakType.OPPHOER,
                ansvarligEnhet = "",
                saksbehandlerIdent = "",
                attestantIdent = null
            ),
            revurderingsaarsak = RevurderingAarsak.OMGJOERING_AV_FARSKAP,
            revurderingInfo = RevurderingInfo.OmgjoeringAvFarskap(
                naavaerendeFar = Navn(fornavn = "Peder", etternavn = "Ås"),
                forrigeFar = Navn(fornavn = "Lars", etternavn = "Holm")
            ),
            virkningsdato = YearMonth.of(2023, Month.JUNE),
            innvilgelsesdato = LocalDate.of(2023, Month.JULY, 5)
        )
        val ferdigFletta =
            SlateFletter.erstatt(deserialized, OmgjoeringAvFarskapRevurderingBrevdata.fra(behandling))
                .elements.flatMap { it.children.mapNotNull { i -> i.text } }.joinToString()
        Assertions.assertTrue(ferdigFletta.contains("Peder Ås"))
        Assertions.assertTrue(ferdigFletta.contains("Lars Holm"))
        Assertions.assertFalse(ferdigFletta.contains("<)"))
    }
}
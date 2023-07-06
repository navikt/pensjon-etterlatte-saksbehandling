package no.nav.etterlatte.brev.model

import io.kotest.matchers.shouldBe
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
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Month
import java.time.YearMonth
import java.util.*

internal class SlateTest {

    @Test
    fun serde() {
        val originalJson = this.javaClass.getResource("/brev/oms/slate_serde.json")!!.readText()

        val deserialized = deserialize<Slate>(originalJson)

        // Bare noen stikkprøver
        with(deserialized) {
            elements.size shouldBe 6
            elements[0].type shouldBe Slate.ElementType.HEADING_TWO
            elements[0].children.size shouldBe 1
            elements[2].children.size shouldBe 3

            elements.last().type shouldBe Slate.ElementType.BULLETED_LIST
            elements.last().children.count { it.type == Slate.ElementType.LIST_ITEM } shouldBe 3
        }
    }

    @Test
    fun fletterInnFlettefelt() {
        val originalJson = this.javaClass.getResource("/maler/bp-revurdering-adopsjon.json")!!.readText()

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
            revurderingsaarsak = RevurderingAarsak.ADOPSJON,
            revurderingInfo = RevurderingInfo.Adopsjon(
                adoptertAv1 = Navn(fornavn = "Navn", etternavn = "Navnesen")
            ),
            virkningsdato = YearMonth.of(2023, Month.JUNE)
        )
        val ferdigFletta =
            deserialized.flettInn(behandling).elements.flatMap { it.children.mapNotNull { i -> i.text } }.joinToString()
        assertTrue(ferdigFletta.contains("Navn Navnesen"))
        assertFalse(ferdigFletta.contains("<navn>"))
    }
}
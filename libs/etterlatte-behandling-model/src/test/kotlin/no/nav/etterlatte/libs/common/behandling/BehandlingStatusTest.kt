package no.nav.etterlatte.libs.common.behandling

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import no.nav.etterlatte.libs.common.objectMapper
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.io.File

class BehandlingStatusTest {
    private val redigerbareStatuser =
        setOf(
            BehandlingStatus.OPPRETTET,
            BehandlingStatus.VILKAARSVURDERT,
            BehandlingStatus.TRYGDETID_OPPDATERT,
            BehandlingStatus.BEREGNET,
            BehandlingStatus.AVKORTET,
            BehandlingStatus.RETURNERT,
        )

    @ParameterizedTest
    @EnumSource(BehandlingStatus::class)
    fun `kanEndres er bare sann for statuser der saksbehandler kan jobbe med behandlingen`(status: BehandlingStatus) {
        status.kanEndres() shouldBe (status in redigerbareStatuser)
    }

    @Test
    fun `kanEndres har samme statuser som frontend (paritetsfixture)`() {
        val paritet = lesParitetsfixture()

        BehandlingStatus.entries.map { it.name } shouldContainExactlyInAnyOrder paritet.alleStatuser
        BehandlingStatus.kanEndres().map { it.name } shouldContainExactlyInAnyOrder paritet.kanEndres
    }

    internal data class Paritetsfixture(
        val beskrivelse: String,
        val alleStatuser: List<String>,
        val kanEndres: List<String>,
    )

    private fun lesParitetsfixture(): Paritetsfixture {
        // Fixturen ligger i frontend slik at en endring i den trigger frontend-testene også.
        val fil = File("../../$PARITETSFIXTURE")
        return objectMapper.readValue(fil.readText(), Paritetsfixture::class.java)
    }

    companion object {
        private const val PARITETSFIXTURE =
            "apps/etterlatte-saksbehandling-ui/client/src/components/behandling/felles/paritet/behandlingStatusKanEndres.json"
    }
}

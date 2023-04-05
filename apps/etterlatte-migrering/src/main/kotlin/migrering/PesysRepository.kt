package no.nav.etterlatte.migrering

import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import rapidsandrivers.migrering.Enhet
import rapidsandrivers.migrering.PesysId
import java.time.LocalDateTime

internal class PesysRepository {

    fun hentSaker(): List<Pesyssak> = listOf(
        Pesyssak(
            id = PesysId("1234"),
            enhet = Enhet("4808"),
            folkeregisteridentifikator = Folkeregisteridentifikator.of("08071272487"),
            mottattdato = LocalDateTime.now(),
            persongalleri = Persongalleri("08071272487")
        )
    )
}
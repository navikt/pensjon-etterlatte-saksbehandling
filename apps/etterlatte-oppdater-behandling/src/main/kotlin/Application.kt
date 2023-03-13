package no.nav.etterlatte

import no.nav.etterlatte.rapidsandrivers.init
import no.nav.helse.rapids_rivers.RapidsConnection

fun main() = init(
    { AppBuilder(it) },
    { rc: RapidsConnection, ab: AppBuilder ->
        val behandlingservice = ab.createBehandlingService()
        PdlHendelser(rc, behandlingservice)
        OmregningsHendelser(rc, behandlingservice)
        Reguleringsforespoersel(rc, behandlingservice)
    }
)
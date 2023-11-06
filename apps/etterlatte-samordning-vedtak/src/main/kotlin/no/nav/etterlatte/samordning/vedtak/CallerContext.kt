package no.nav.etterlatte.samordning.vedtak

sealed interface CallerContext

data class MaskinportenTpContext(val tpnr: Tjenestepensjonnummer, val organisasjonsnr: String) : CallerContext

object PensjonContext : CallerContext

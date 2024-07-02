package no.nav.etterlatte.samordning.sak

import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.ktor.route.FoedselsnummerDTO

class BehandlingService(
    private val behandlingKlient: BehandlingKlient,
) {
    suspend fun hentSakforPerson(ident: FoedselsnummerDTO): List<Sak> = behandlingKlient.hentSakForPerson(ident)
}

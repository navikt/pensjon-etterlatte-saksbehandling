package no.nav.etterlatte.samordning.sak

import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.ktor.route.FoedselsnummerDTO

class BehandlingService(
    private val behandlingKlient: BehandlingKlient,
) {
    suspend fun hentSakforPerson(ident: FoedselsnummerDTO): List<Long> = behandlingKlient.hentSakForPerson(ident)

    suspend fun hentSak(id: Long): Sak? = behandlingKlient.hentSak(id)
}

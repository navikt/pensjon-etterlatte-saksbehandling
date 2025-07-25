package no.nav.etterlatte.behandling.sak

import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.sak.SakUtenGradering
import no.nav.etterlatte.libs.ktor.route.FoedselsnummerDTO

class BehandlingService(
    private val behandlingKlient: BehandlingKlient,
    private val vedtaksvurderingKlientSak: VedtaksvurderingSakKlient,
) {
    suspend fun hentSakforPerson(ident: FoedselsnummerDTO): List<SakId> = behandlingKlient.hentSakForPerson(ident)

    suspend fun hentLopendeSakForPerson(ident: FoedselsnummerDTO): Boolean {
        val saker = behandlingKlient.hentSakForPerson(ident)
        for (sak in saker) {
            vedtaksvurderingKlientSak.hentLoependeVedtak(sak).let {
                if (it.erLoepende) {
                    return true
                }
            }
        }
        return false
    }

    suspend fun hentSak(id: SakId): SakUtenGradering? = behandlingKlient.hentSak(id)
}

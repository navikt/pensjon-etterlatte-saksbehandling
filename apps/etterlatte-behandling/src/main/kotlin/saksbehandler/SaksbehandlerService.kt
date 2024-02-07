package no.nav.etterlatte.saksbehandler

class SaksbehandlerService(private val dao: SaksbehandlerInfoDaoTrans) {
    fun hentSaksbehandlereForEnhet(enhet: String) = dao.hentSaksbehandlereForEnhet(enhet)
}

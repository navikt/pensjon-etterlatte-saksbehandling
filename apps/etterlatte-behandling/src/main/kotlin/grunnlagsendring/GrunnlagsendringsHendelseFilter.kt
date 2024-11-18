package no.nav.etterlatte.grunnlagsendring

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.SystemUser
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.domain.GrunnlagsendringsType
import no.nav.etterlatte.behandling.klienter.VedtakKlient
import no.nav.etterlatte.libs.common.sak.SakId
import org.slf4j.LoggerFactory
import java.time.LocalDate

class KunSystembrukerException(
    msg: String,
) : Exception(msg)

class GrunnlagsendringsHendelseFilter(
    val vedtakKlient: VedtakKlient,
    val behandlingService: BehandlingService,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun hendelseErRelevantForSak(
        sakId: SakId,
        grunnlagendringType: GrunnlagsendringsType,
    ): Boolean {
        if (!ikkeRelevanteHendelserForOpphoertSak(grunnlagendringType)) {
            logger.info("Grunnlagsendring av type $grunnlagendringType i sak $sakId er ikke relevant")
            return false
        }

        val behandlingerForSak = behandlingService.hentBehandlingerForSak(sakId)
        val harAapenBehandling = behandlingerForSak.any { it.status.aapenBehandling() }
        if (harAapenBehandling) {
            return true
        } else {
            val appUser = Kontekst.get().AppUser
            if (appUser is SystemUser) {
                val loependeYtelseDTO =
                    runBlocking {
                        vedtakKlient.sakHarLopendeVedtakPaaDato(
                            sakId,
                            LocalDate.now(),
                            appUser.brukerTokenInfo,
                        )
                    }
                if (!loependeYtelseDTO.erLoepende) {
                    logger.info("Grunnlagsendring av type $grunnlagendringType i sak $sakId er ikke relevant fordi ytelsen ikke er løpende")
                }
                return loependeYtelseDTO.erLoepende
            } else {
                throw KunSystembrukerException("Hendelser kan kun utføres av systembruker")
            }
        }
    }

    private fun ikkeRelevanteHendelserForOpphoertSak(grunnlagendringType: GrunnlagsendringsType) =
        when (grunnlagendringType) {
            GrunnlagsendringsType.DOEDSFALL -> true
            GrunnlagsendringsType.UTFLYTTING -> true
            GrunnlagsendringsType.FORELDER_BARN_RELASJON -> true
            GrunnlagsendringsType.VERGEMAAL_ELLER_FREMTIDSFULLMAKT -> true
            GrunnlagsendringsType.SIVILSTAND -> true
            GrunnlagsendringsType.GRUNNBELOEP -> false
            GrunnlagsendringsType.INSTITUSJONSOPPHOLD -> false
            GrunnlagsendringsType.BOSTED -> true
            GrunnlagsendringsType.FOLKEREGISTERIDENTIFIKATOR -> true
            GrunnlagsendringsType.UFOERETRYGD -> true
        }
}

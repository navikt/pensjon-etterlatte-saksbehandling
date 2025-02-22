package no.nav.etterlatte.behandling.etteroppgjoer

import io.ktor.server.plugins.NotFoundException
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeFunnetException
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.oppgave.OppgaveService
import no.nav.etterlatte.sak.SakLesDao
import java.util.UUID

class EtteroppgjoerService(
    private val dao: EtteroppgjoerDao,
    private val sakDao: SakLesDao,
    private val oppgaveService: OppgaveService,
) {
    fun hentEtteroppgjoer(behandlingId: UUID): Etteroppgjoer {
        val etteroppgjoerBehandling =
            inTransaction {
                dao.hentEtteroppgjoer(behandlingId)
            } ?: throw IkkeFunnetException(
                code = "MANGLER_FORBEHANDLING_ETTEROPPGJOER",
                detail = "Fant ikke forbehandling etteroppgjør $behandlingId",
            )

        val opplysningerSkatt = dao.hentOpplysningerSkatt(behandlingId)
        val opplysningerAInntekt = dao.hentOpplysningerAInntekt(behandlingId)
        val opplysninger =
            EtteroppgjoerOpplysninger(
                skatt = opplysningerSkatt,
                ainntekt = opplysningerAInntekt,
            )

        return Etteroppgjoer(
            behandling = etteroppgjoerBehandling,
            opplysninger = opplysninger,
        )
    }

    fun opprettEtteroppgjoer(sakId: SakId) {
        inTransaction {
            val sak = sakDao.hentSak(sakId) ?: throw NotFoundException("Fant ikke sak med id=$sakId")

            hentOgLagreOpplysninger(sak.ident)

            val nyBehandling =
                EtteroppgjoerBehandling(
                    id = UUID.randomUUID(),
                    status = "opprettet",
                    sak = sak,
                    aar = 2024,
                    opprettet = Tidspunkt.now(),
                )

            dao.lagreEtteroppgjoer(nyBehandling)
            oppgaveService.opprettOppgave(
                referanse = nyBehandling.id.toString(),
                sakId = sakId,
                kilde = OppgaveKilde.BEHANDLING,
                type = OppgaveType.ETTEROPPGJOER,
                merknad = null,
                frist = null,
                saksbehandler = null,
                gruppeId = null,
            )
        }
    }

    private fun hentOgLagreOpplysninger(ident: String) {
        // TODO hent og lagre OpplysnignerSkatt
        dao.lagreOpplysningerSkatt()

        // TODO hent og lagre AInntekt
        dao.lagreOpplysningerAInntekt()
    }
}

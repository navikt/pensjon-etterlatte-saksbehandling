package no.nav.etterlatte.behandling.etteroppgjoer

import io.ktor.server.plugins.NotFoundException
import no.nav.etterlatte.behandling.etteroppgjoer.inntektskomponent.InntektskomponentService
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
    private val inntektskomponentService: InntektskomponentService,
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

        return Etteroppgjoer(
            behandling = etteroppgjoerBehandling,
            opplysninger =
                EtteroppgjoerOpplysninger(
                    skatt = opplysningerSkatt,
                    ainntekt = opplysningerAInntekt.toView(),
                ),
        )
    }

    suspend fun opprettEtteroppgjoer(
        sakId: SakId,
        aar: Int,
    ) {
        val sak = sakDao.hentSak(sakId) ?: throw NotFoundException("Fant ikke sak med id=$sakId")

        hentOgLagreOpplysninger(sak.ident, aar)

        inTransaction {
            val nyBehandling =
                EtteroppgjoerBehandling(
                    id = UUID.randomUUID(),
                    sak = sak,
                    sekvensnummerSkatt = "123",
                    status = "opprettet",
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

    private suspend fun hentOgLagreOpplysninger(
        ident: String,
        aar: Int,
    ) {
        // TODO hent og lagre OpplysnignerSkatt
        inTransaction {
            dao.lagreOpplysningerSkatt()
        }

        val aInntekt = inntektskomponentService.hentInntektFraAInntekt(ident, aar)
        inTransaction {
            dao.lagreOpplysningerAInntekt(aInntekt)
        }
    }
}

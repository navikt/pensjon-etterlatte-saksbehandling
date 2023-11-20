package no.nav.etterlatte.behandling.generellbehandling

import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.domain.Foerstegangsbehandling
import no.nav.etterlatte.behandling.hendelse.HendelseDao
import no.nav.etterlatte.behandling.klienter.GrunnlagKlient
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.feilhaandtering.ForespoerselException
import no.nav.etterlatte.libs.common.generellbehandling.DokumentMedSendtDato
import no.nav.etterlatte.libs.common.generellbehandling.GenerellBehandling
import no.nav.etterlatte.libs.common.generellbehandling.GenerellBehandlingHendelseType
import no.nav.etterlatte.libs.common.generellbehandling.Innhold
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.oppgave.OppgaveIntern
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.oppgave.SakIdOgReferanse
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.oppgave.OppgaveService
import no.nav.etterlatte.token.BrukerTokenInfo
import no.nav.etterlatte.token.Saksbehandler
import org.slf4j.LoggerFactory
import java.time.temporal.ChronoUnit
import java.util.UUID

class DokumentManglerDatoException(message: String) : Exception(message)

class DokumentErIkkeMarkertSomSendt(message: String) : Exception(message)

class LandFeilIsokodeException(message: String) : Exception(message)

class ManglerLandkodeException(message: String) : Exception(message)

class ManglerRinanummerException(message: String) : Exception(message)

class KanIkkeEndreFattetEllerAttestertBehandling(message: String) : Exception(message)

data class Kommentar(val begrunnelse: String)

class GenerellBehandlingService(
    private val generellBehandlingDao: GenerellBehandlingDao,
    private val oppgaveService: OppgaveService,
    private val behandlingService: BehandlingService,
    private val grunnlagKlient: GrunnlagKlient,
    private val hendelseDao: HendelseDao,
) {
    private val logger = LoggerFactory.getLogger(GenerellBehandlingService::class.java)

    fun opprettBehandling(
        generellBehandling: GenerellBehandling,
        saksbehandler: Saksbehandler?,
    ): GenerellBehandling {
        val opprettetbehandling = generellBehandlingDao.opprettGenerellbehandling(generellBehandling)
        val oppgaveForGenerellBehandling =
            oppgaveService.opprettNyOppgaveMedSakOgReferanse(
                opprettetbehandling.id.toString(),
                opprettetbehandling.sakId,
                OppgaveKilde.GENERELL_BEHANDLING,
                OppgaveType.KRAVPAKKE_UTLAND,
                null,
            )
        tildelSaksbehandlerTilNyOppgaveHvisFinnes(oppgaveForGenerellBehandling, opprettetbehandling)
        opprettHendelse(GenerellBehandlingHendelseType.OPPRETTET, opprettetbehandling, saksbehandler)
        return opprettetbehandling
    }

    private fun tildelSaksbehandlerTilNyOppgaveHvisFinnes(
        oppgave: OppgaveIntern,
        generellBehandling: GenerellBehandling,
    ) {
        if (generellBehandling.tilknyttetBehandling !== null) {
            val kanskjeOppgaveMedSaksbehandler =
                oppgaveService.hentOppgaveForSaksbehandlerFraFoerstegangsbehandling(
                    behandlingId = generellBehandling.tilknyttetBehandling!!,
                )
            if (kanskjeOppgaveMedSaksbehandler != null) {
                val saksbehandler = kanskjeOppgaveMedSaksbehandler.saksbehandler
                if (saksbehandler != null) {
                    oppgaveService.tildelSaksbehandler(oppgave.id, saksbehandler)
                    logger.info(
                        "Opprettet generell behandling for utland for sak: ${generellBehandling.sakId} " +
                            "og behandling: ${generellBehandling.tilknyttetBehandling}. Gjelder oppgave: ${oppgave.id}",
                    )
                }
            }
        }
    }

    fun sendTilAttestering(
        generellBehandling: GenerellBehandling,
        saksbehandler: Saksbehandler,
    ) {
        val hentetBehandling = hentBehandlingMedId(generellBehandling.id)
        require(hentetBehandling !== null) { "Behandlingen må finnes, fant ikke id: ${generellBehandling.id}" }
        check(hentetBehandling!!.status == GenerellBehandling.Status.OPPRETTET) {
            "Behandlingen må ha status opprettet, hadde ${generellBehandling.status}"
        }
        when (generellBehandling.innhold) {
            is Innhold.KravpakkeUtland -> validerUtland(generellBehandling.innhold as Innhold.KravpakkeUtland)
            is Innhold.Annen -> throw NotImplementedError("Ikke implementert")
            null -> throw NotImplementedError("Ikke implementert")
        }

        oppdaterBehandling(generellBehandling.copy(status = GenerellBehandling.Status.FATTET))
        opprettHendelse(GenerellBehandlingHendelseType.FATTET, hentetBehandling, saksbehandler)
        oppgaveService.ferdigStillOppgaveUnderBehandling(generellBehandling.id.toString(), saksbehandler)
        val trettiDagerFremITid = Tidspunkt.now().plus(30L, ChronoUnit.DAYS)
        oppgaveService.opprettNyOppgaveMedSakOgReferanse(
            generellBehandling.id.toString(),
            generellBehandling.sakId,
            OppgaveKilde.GENERELL_BEHANDLING,
            OppgaveType.ATTESTERING,
            merknad = "Attestering av ${generellBehandling.type.name}",
            frist = trettiDagerFremITid,
        )
    }

    fun attester(
        generellbehandlingId: UUID,
        saksbehandler: Saksbehandler,
    ) {
        val hentetBehandling = hentBehandlingMedId(generellbehandlingId)
        require(hentetBehandling !== null) { "Behandlingen må finnes, fant ikke id: $generellbehandlingId" }
        require(hentetBehandling?.status === GenerellBehandling.Status.FATTET) {
            "Behandlingen må ha status FATTET, hadde: ${hentetBehandling?.status}"
        }

        oppdaterBehandling(hentetBehandling!!.copy(status = GenerellBehandling.Status.ATTESTERT))
        opprettHendelse(GenerellBehandlingHendelseType.ATTESTERT, hentetBehandling, saksbehandler)
        oppgaveService.ferdigStillOppgaveUnderBehandling(generellbehandlingId.toString(), saksbehandler)
    }

    fun underkjenn(
        generellbehandlingId: UUID,
        saksbehandler: Saksbehandler,
        kommentar: Kommentar,
    ) {
        val hentetBehandling = hentBehandlingMedId(generellbehandlingId)
        require(hentetBehandling !== null) { "Behandlingen må finnes, fant ikke id: $generellbehandlingId" }
        require(hentetBehandling?.status === GenerellBehandling.Status.FATTET) {
            "Behandlingen må ha status FATTET, hadde: ${hentetBehandling?.status}"
        }
        val behandling = hentetBehandling!!
        oppgaveService.ferdigstillOppgaveUnderbehandlingOgLagNyMedType(
            fattetoppgaveReferanseOgSak = SakIdOgReferanse(behandling.sakId, behandling.id.toString()),
            oppgaveType = OppgaveType.UNDERKJENT,
            merknad = kommentar.begrunnelse,
            saksbehandler = saksbehandler,
        )
        oppdaterBehandling(behandling.copy(status = GenerellBehandling.Status.OPPRETTET))
        opprettHendelse(GenerellBehandlingHendelseType.UNDERKJENT, hentetBehandling, saksbehandler, kommentar.begrunnelse)
    }

    private fun opprettHendelse(
        hendelseType: GenerellBehandlingHendelseType,
        behandling: GenerellBehandling,
        saksbehandler: Saksbehandler?,
        begrunnelse: String? = null,
    ) {
        hendelseDao.generellBehandlingHendelse(
            behandlingId = behandling.id,
            sakId = behandling.sakId,
            hendelse = hendelseType,
            inntruffet = Tidspunkt.now(),
            saksbehandler = saksbehandler?.ident,
            begrunnelse = begrunnelse,
        )
    }

    private fun validerUtland(innhold: Innhold.KravpakkeUtland) {
        if (innhold.landIsoKode.isEmpty()) {
            throw ManglerLandkodeException("Mangler landkode")
        }
        if (innhold.landIsoKode.any { it.length != 3 }) {
            throw LandFeilIsokodeException("Landkoden er feil ${innhold.landIsoKode.toJson()}")
        }
        if (innhold.rinanummer.isEmpty()) {
            throw ManglerRinanummerException("Må ha rinanummer")
        }

        innhold.dokumenter.forEach { doc -> validerMaaHaDatoOgVaereMarkertSomSendt(doc) }
    }

    private fun validerMaaHaDatoOgVaereMarkertSomSendt(dokumentMedSendtDato: DokumentMedSendtDato) {
        if (!dokumentMedSendtDato.sendt) {
            throw DokumentErIkkeMarkertSomSendt("Dokument ${dokumentMedSendtDato.dokumenttype} er ikke markert som sendt ")
        }
        dokumentMedSendtDato.dato
            ?: throw DokumentManglerDatoException("Dokument ${dokumentMedSendtDato.dokumenttype} er markert som sendt men mangler dato")
    }

    fun lagreNyeOpplysninger(generellBehandling: GenerellBehandling): GenerellBehandling {
        val lagretBehandling = generellBehandlingDao.hentGenerellBehandlingMedId(generellBehandling.id)
        kanLagreNyBehandling(lagretBehandling)
        return this.oppdaterBehandling(generellBehandling)
    }

    private fun kanLagreNyBehandling(generellBehandling: GenerellBehandling?) {
        requireNotNull(generellBehandling) { "Behandlingen finnes ikke " }
        if (!generellBehandling.kanEndres()) {
            throw KanIkkeEndreFattetEllerAttestertBehandling("Behandling kan ikke være fattet eller attestert hvis du skal endre den")
        }
    }

    private fun oppdaterBehandling(generellBehandling: GenerellBehandling): GenerellBehandling {
        return generellBehandlingDao.oppdaterGenerellBehandling(generellBehandling)
    }

    fun hentBehandlingMedId(id: UUID): GenerellBehandling? {
        return generellBehandlingDao.hentGenerellBehandlingMedId(id)
    }

    fun hentBehandlingerForSak(sakId: Long): List<GenerellBehandling> {
        return generellBehandlingDao.hentGenerellBehandlingForSak(sakId)
    }

    private fun hentGenerellbehandlingSinTilknyttetedeBehandling(tilknyttetBehandlingId: UUID): GenerellBehandling? {
        return generellBehandlingDao.hentBehandlingForTilknyttetBehandling(tilknyttetBehandlingId)
    }

    suspend fun hentKravpakkeForSak(
        sakId: Long,
        brukerTokenInfo: BrukerTokenInfo,
    ): KravPakkeMedAvdoed {
        val (kravpakke, forstegangsbehandlingMedKravpakke) =
            inTransaction {
                val behandlingerForSak = behandlingService.hentBehandlingerISak(sakId)
                val foerstegangsbehandlingMedKravpakke =
                    behandlingerForSak.find { behandling ->
                        (behandling is Foerstegangsbehandling) && behandling.boddEllerArbeidetUtlandet?.skalSendeKravpakke == true
                    }
                        ?: throw FantIkkeFoerstegangsbehandlingForKravpakkeOgSak("Fant ikke behandlingen for sak $sakId")
                val kravpakke =
                    this.hentGenerellbehandlingSinTilknyttetedeBehandling(foerstegangsbehandlingMedKravpakke.id)
                        ?: throw FantIkkeKravpakkeForFoerstegangsbehandling(
                            "Fant ikke" +
                                " kravpakke for ${foerstegangsbehandlingMedKravpakke.id}",
                        )
                Pair(kravpakke, foerstegangsbehandlingMedKravpakke)
            }
        if (kravpakke.status == GenerellBehandling.Status.ATTESTERT) {
            val avdoed =
                grunnlagKlient.finnPersonOpplysning(
                    forstegangsbehandlingMedKravpakke.id,
                    Opplysningstype.AVDOED_PDL_V1,
                    brukerTokenInfo,
                ) ?: throw FantIkkeAvdoedException(
                    "Fant ikke avdød for sak: $sakId behandlingid: ${forstegangsbehandlingMedKravpakke.id}",
                )
            return KravPakkeMedAvdoed(kravpakke, avdoed.opplysning)
        } else {
            throw RuntimeException("Kravpakken for sak $sakId har ikke blitt attestert, status: ${kravpakke.status}")
        }
    }
}

class FantIkkeAvdoedException(msg: String) :
    ForespoerselException(
        status = 400,
        code = "FANT_IKKE_AVDOED",
        detail = msg,
    )

class FantIkkeFoerstegangsbehandlingForKravpakkeOgSak(msg: String) :
    ForespoerselException(
        status = 400,
        code = "FANT_IKKE_FOERSTEGANGSBEHANDLING_FOR_KRAVPAKKE",
        detail = msg,
    )

class FantIkkeKravpakkeForFoerstegangsbehandling(msg: String) :
    ForespoerselException(
        status = 400,
        code = "FANT_IKKE_KRAVPAKKE_FOR_FOERSTEGANGSBEHANDLING",
        detail = msg,
    )

data class KravPakkeMedAvdoed(
    val kravpakke: GenerellBehandling,
    val avdoed: Person,
)

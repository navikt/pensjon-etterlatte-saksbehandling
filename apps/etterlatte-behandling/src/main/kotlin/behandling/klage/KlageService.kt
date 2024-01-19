package no.nav.etterlatte.behandling.klage

import io.ktor.server.plugins.NotFoundException
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.hendelse.HendelseDao
import no.nav.etterlatte.behandling.klienter.BrevApiKlient
import no.nav.etterlatte.behandling.klienter.KlageKlient
import no.nav.etterlatte.libs.common.behandling.BehandlingResultat
import no.nav.etterlatte.libs.common.behandling.EkstradataInnstilling
import no.nav.etterlatte.libs.common.behandling.Formkrav
import no.nav.etterlatte.libs.common.behandling.InnkommendeKlage
import no.nav.etterlatte.libs.common.behandling.InnstillingTilKabal
import no.nav.etterlatte.libs.common.behandling.KabalStatus
import no.nav.etterlatte.libs.common.behandling.Kabalrespons
import no.nav.etterlatte.libs.common.behandling.Klage
import no.nav.etterlatte.libs.common.behandling.KlageBrevInnstilling
import no.nav.etterlatte.libs.common.behandling.KlageResultat
import no.nav.etterlatte.libs.common.behandling.KlageUtfall
import no.nav.etterlatte.libs.common.behandling.KlageUtfallUtenBrev
import no.nav.etterlatte.libs.common.behandling.SendtInnstillingsbrev
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.klage.KlageHendelseType
import no.nav.etterlatte.libs.common.klage.StatistikkKlage
import no.nav.etterlatte.libs.common.oppgave.OppgaveIntern
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.oppgave.OppgaveService
import no.nav.etterlatte.sak.SakDao
import no.nav.etterlatte.token.Saksbehandler
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.format.DateTimeFormatter
import java.util.UUID

interface KlageService {
    fun opprettKlage(
        sakId: Long,
        innkommendeKlage: InnkommendeKlage,
    ): Klage

    fun hentKlage(id: UUID): Klage?

    fun hentKlagerISak(sakId: Long): List<Klage>

    fun lagreFormkravIKlage(
        klageId: UUID,
        formkrav: Formkrav,
        saksbehandler: Saksbehandler,
    ): Klage

    fun lagreUtfallAvKlage(
        klageId: UUID,
        utfall: KlageUtfallUtenBrev,
        saksbehandler: Saksbehandler,
    ): Klage

    fun haandterKabalrespons(
        klageId: UUID,
        kabalrespons: Kabalrespons,
    )

    fun ferdigstillKlage(
        klageId: UUID,
        saksbehandler: Saksbehandler,
    ): Klage
}

class KlageServiceImpl(
    private val klageDao: KlageDao,
    private val sakDao: SakDao,
    private val hendelseDao: HendelseDao,
    private val oppgaveService: OppgaveService,
    private val brevApiKlient: BrevApiKlient,
    private val klageKlient: KlageKlient,
    private val klageHendelser: IKlageHendelserService,
) : KlageService {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    override fun opprettKlage(
        sakId: Long,
        innkommendeKlage: InnkommendeKlage,
    ): Klage {
        val sak = sakDao.hentSak(sakId) ?: throw NotFoundException("Fant ikke sak med id=$sakId")

        val klage = Klage.ny(sak, innkommendeKlage)
        logger.info("Oppretter klage med id=${klage.id} på sak med id=$sakId")

        klageDao.lagreKlage(klage)

        oppgaveService.opprettNyOppgaveMedSakOgReferanse(
            referanse = klage.id.toString(),
            sakId = sakId,
            oppgaveKilde = OppgaveKilde.EKSTERN,
            oppgaveType = OppgaveType.KLAGE,
            merknad = null,
        )

        hendelseDao.klageHendelse(
            klageId = klage.id,
            sakId = klage.sak.id,
            hendelse = KlageHendelseType.OPPRETTET,
            inntruffet = Tidspunkt.now(),
            saksbehandler = null,
            kommentar = null,
            begrunnelse = null,
        )

        klageHendelser.sendKlageHendelse(StatistikkKlage(klage.id, klage.sak, Tidspunkt.now()), KlageHendelseType.OPPRETTET)

        return klage
    }

    override fun hentKlage(id: UUID): Klage? {
        return klageDao.hentKlage(id)
    }

    override fun hentKlagerISak(sakId: Long): List<Klage> {
        return klageDao.hentKlagerISak(sakId)
    }

    override fun lagreFormkravIKlage(
        klageId: UUID,
        formkrav: Formkrav,
        saksbehandler: Saksbehandler,
    ): Klage {
        val klage = klageDao.hentKlage(klageId) ?: throw NotFoundException("Klage med id=$klageId finnes ikke")
        logger.info("Lagrer vurdering av formkrav for klage med id=$klageId")

        if (!Formkrav.erFormkravKonsistente(formkrav)) {
            logger.warn(
                "Mottok 'rare' formkrav fra frontend, der det er mismatch mellom svar på formkrav. Gjelder " +
                    "klagen med id=$klageId. Stopper ikke opp lagringen av formkravene, siden alt er vurdert av " +
                    "saksbehandler.",
            )
        }

        val oppdatertKlage = klage.oppdaterFormkrav(formkrav, saksbehandler.ident)
        klageDao.lagreKlage(oppdatertKlage)

        return oppdatertKlage
    }

    override fun lagreUtfallAvKlage(
        klageId: UUID,
        utfall: KlageUtfallUtenBrev,
        saksbehandler: Saksbehandler,
    ): Klage {
        val klage = klageDao.hentKlage(klageId) ?: throw NotFoundException("Fant ikke klage med id=$klageId")

        val utfallMedBrev =
            when (utfall) {
                // Vurder å slette brev hvis det finnes her? Så vi ikke polluter med hengende brev
                is KlageUtfallUtenBrev.Omgjoering ->
                    KlageUtfall.Omgjoering(
                        omgjoering = utfall.omgjoering,
                        saksbehandler = Grunnlagsopplysning.Saksbehandler.create(saksbehandler.ident),
                    )

                is KlageUtfallUtenBrev.DelvisOmgjoering ->
                    KlageUtfall.DelvisOmgjoering(
                        omgjoering = utfall.omgjoering,
                        innstilling =
                            InnstillingTilKabal(
                                lovhjemmel = enumValueOf(utfall.innstilling.lovhjemmel),
                                tekst = utfall.innstilling.tekst,
                                brev = brevForInnstilling(klage, saksbehandler),
                            ),
                        saksbehandler = Grunnlagsopplysning.Saksbehandler.create(saksbehandler.ident),
                    )

                is KlageUtfallUtenBrev.StadfesteVedtak ->
                    KlageUtfall.StadfesteVedtak(
                        innstilling =
                            InnstillingTilKabal(
                                lovhjemmel = enumValueOf(utfall.innstilling.lovhjemmel),
                                tekst = utfall.innstilling.tekst,
                                brev = brevForInnstilling(klage, saksbehandler),
                            ),
                        saksbehandler = Grunnlagsopplysning.Saksbehandler.create(saksbehandler.ident),
                    )
            }

        val klageMedOppdatertUtfall = klage.oppdaterUtfall(utfallMedBrev)
        klageDao.lagreKlage(klageMedOppdatertUtfall)

        return klageMedOppdatertUtfall
    }

    private fun brevForInnstilling(
        klage: Klage,
        saksbehandler: Saksbehandler,
    ): KlageBrevInnstilling {
        return when (val utfall = klage.utfall) {
            is KlageUtfall.DelvisOmgjoering -> utfall.innstilling.brev
            is KlageUtfall.StadfesteVedtak -> utfall.innstilling.brev
            else -> {
                val brev = runBlocking { brevApiKlient.opprettKlageInnstillingsbrevISak(klage.sak.id, saksbehandler) }
                KlageBrevInnstilling(brev.id)
            }
        }
    }

    override fun haandterKabalrespons(
        klageId: UUID,
        kabalrespons: Kabalrespons,
    ) {
        val opprinneligKlage = klageDao.hentKlage(klageId) ?: throw NotFoundException()

        if (kabalrespons.kabalStatus == KabalStatus.FERDIGSTILT) {
            // Se på hva kabalresponsen er, og opprett oppgave om det er nødvendig.
            when (kabalrespons.resultat) {
                BehandlingResultat.HENLAGT,
                BehandlingResultat.IKKE_SATT,
                -> {
                } // Her trenger vi ikke gjøre noe
                BehandlingResultat.IKKE_MEDHOLD,
                BehandlingResultat.IKKE_MEDHOLD_FORMKRAV_AVVIST,
                -> {
                } // trenger vi å gjøre noe spesifikt her?
                BehandlingResultat.MEDHOLD -> lagOppgaveForOmgjoering(opprinneligKlage)
            }
        }

        hendelseDao.klageHendelse(
            klageId = opprinneligKlage.id,
            sakId = opprinneligKlage.sak.id,
            hendelse = KlageHendelseType.KABAL_HENDELSE,
            inntruffet = Tidspunkt.now(),
            saksbehandler = null,
            kommentar = "Mottok status=${kabalrespons.kabalStatus} fra kabal, med resultat=${kabalrespons.resultat}",
            begrunnelse = null,
        )

        klageDao.oppdaterKabalStatus(klageId, kabalrespons)
    }

    override fun ferdigstillKlage(
        klageId: UUID,
        saksbehandler: Saksbehandler,
    ): Klage {
        val klage = klageDao.hentKlage(klageId) ?: throw NotFoundException("Kunne ikke finne klage med id=$klageId")

        if (!klage.kanFerdigstille()) {
            throw IllegalStateException("Klagen med id=$klageId kan ikke ferdigstilles siden den har feil status / tilstand")
        }

        val saksbehandlerForKlageOppgave = oppgaveService.hentSaksbehandlerForOppgaveUnderArbeidByReferanse(klageId.toString())
        if (saksbehandlerForKlageOppgave != saksbehandler.ident) {
            throw IllegalArgumentException(
                "Saksbehandler er ikke ansvarlig på oppgaven til klagen med id=$klageId, " +
                    "og kan derfor ikke ferdigstille behandlingen",
            )
        }

        val (innstilling, omgjoering) =
            when (val utfall = klage.utfall) {
                is KlageUtfall.StadfesteVedtak -> utfall.innstilling to null
                is KlageUtfall.DelvisOmgjoering -> utfall.innstilling to utfall.omgjoering
                is KlageUtfall.Omgjoering -> null to utfall.omgjoering
                null -> null to null
            }

        val oppgaveOmgjoering = omgjoering?.let { lagOppgaveForOmgjoering(klage) }
        val sendtInnstillingsbrev = innstilling?.let { ferdigstillInnstilling(it, klage, saksbehandler) }

        val resultat =
            KlageResultat(
                opprettetOppgaveOmgjoeringId = oppgaveOmgjoering?.id,
                sendtInnstillingsbrev = sendtInnstillingsbrev,
            )
        val klageMedOppdatertResultat = klage.ferdigstill(resultat)
        klageDao.lagreKlage(klageMedOppdatertResultat)

        oppgaveService.ferdigStillOppgaveUnderBehandling(klage.id.toString(), saksbehandler)
        hendelseDao.klageHendelse(
            klageId = klageId,
            sakId = klage.sak.id,
            hendelse = KlageHendelseType.FERDIGSTILT,
            inntruffet = Tidspunkt.now(),
            saksbehandler = saksbehandler.ident,
            kommentar = null,
            begrunnelse = null,
        )

        klageHendelser.sendKlageHendelse(
            StatistikkKlage(klage.id, klage.sak, Tidspunkt.now(), saksbehandler.ident),
            KlageHendelseType.FERDIGSTILT,
        )

        return klageMedOppdatertResultat
    }

    private fun ferdigstillInnstilling(
        innstilling: InnstillingTilKabal,
        klage: Klage,
        saksbehandler: Saksbehandler,
    ): SendtInnstillingsbrev {
        val innstillingsbrev = innstilling.brev
        val (tidJournalfoert, journalpostId) =
            runBlocking {
                ferdigstillOgDistribuerBrev(
                    sakId = klage.sak.id,
                    brevId = innstillingsbrev.brevId,
                    saksbehandler = saksbehandler,
                )
            }
        val brev =
            runBlocking {
                brevApiKlient.hentBrev(
                    sakId = klage.sak.id,
                    brevId = innstillingsbrev.brevId,
                    brukerTokenInfo = saksbehandler,
                )
            }
        runBlocking {
            klageKlient.sendKlageTilKabal(
                klage = klage,
                ekstradataInnstilling =
                    EkstradataInnstilling(
                        mottakerInnstilling = brev.mottaker,
                        // TODO: Håndter verge
                        vergeEllerFullmektig = null,
                        journalpostInnstillingsbrev = journalpostId,
                        journalpostKlage = klage.innkommendeDokument?.journalpostId,
                        // TODO: koble på når vi har journalpost soeknad inn
                        journalpostSoeknad = null,
                        // TODO: hent ut vedtaksbrev for vedtaket
                        journalpostVedtak = null,
                    ),
            )
        }

        return SendtInnstillingsbrev(
            journalfoerTidspunkt = tidJournalfoert,
            sendtKabalTidspunkt = Tidspunkt.now(),
            brevId = innstillingsbrev.brevId,
            journalpostId = journalpostId,
        )
    }

    private fun lagOppgaveForOmgjoering(klage: Klage): OppgaveIntern {
        val vedtaketKlagenGjelder =
            klage.formkrav?.formkrav?.vedtaketKlagenGjelder ?: throw OmgjoeringMaaGjeldeEtVedtakException(klage)

        return oppgaveService.opprettNyOppgaveMedSakOgReferanse(
            referanse = klage.id.toString(),
            sakId = klage.sak.id,
            oppgaveKilde = OppgaveKilde.BEHANDLING,
            oppgaveType = OppgaveType.OMGJOERING,
            merknad = "Vedtak om ${vedtaketKlagenGjelder.vedtakType?.toString()?.lowercase()} (${
                vedtaketKlagenGjelder.datoAttestert?.format(
                    DateTimeFormatter.ISO_LOCAL_DATE,
                )
            }) skal omgjøres",
        )
    }

    private suspend fun ferdigstillOgDistribuerBrev(
        sakId: Long,
        brevId: Long,
        saksbehandler: Saksbehandler,
    ): Pair<Tidspunkt, String> {
        // TODO: Her bør vi ha noe error recovery: Hent status på brevet, og forsøk en resume fra der.
        brevApiKlient.ferdigstillBrev(sakId, brevId, saksbehandler)
        val journalpostIdJournalfoering = brevApiKlient.journalfoerBrev(sakId, brevId, saksbehandler).journalpostId
        val tidspunktJournalfoert = Tidspunkt.now()
        val bestillingsIdDistribuering = brevApiKlient.distribuerBrev(sakId, brevId, saksbehandler).bestillingsId
        logger.info(
            "Distribusjon av innstillingsbrevet med id=$brevId bestilt til klagen i sak med sakId=$sakId, " +
                "med bestillingsId $bestillingsIdDistribuering",
        )
        return tidspunktJournalfoert to journalpostIdJournalfoering
    }
}

class OmgjoeringMaaGjeldeEtVedtakException(klage: Klage) :
    Exception("Klagen med id=${klage.id} skal resultere i en omgjøring, men mangler et vedtak som klagen gjelder")

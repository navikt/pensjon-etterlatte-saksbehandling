package no.nav.etterlatte.behandling.klage

import io.ktor.server.plugins.NotFoundException
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.hendelse.HendelseDao
import no.nav.etterlatte.behandling.hendelse.HendelseType
import no.nav.etterlatte.behandling.klienter.BrevApiKlient
import no.nav.etterlatte.behandling.klienter.KlageKlient
import no.nav.etterlatte.behandling.klienter.VedtakKlient
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.libs.common.FeatureIkkeStoettetException
import no.nav.etterlatte.libs.common.behandling.BehandlingResultat
import no.nav.etterlatte.libs.common.behandling.EkstradataInnstilling
import no.nav.etterlatte.libs.common.behandling.Formkrav
import no.nav.etterlatte.libs.common.behandling.InitieltUtfallMedBegrunnelseDto
import no.nav.etterlatte.libs.common.behandling.InnkommendeKlage
import no.nav.etterlatte.libs.common.behandling.InnstillingTilKabal
import no.nav.etterlatte.libs.common.behandling.KabalStatus
import no.nav.etterlatte.libs.common.behandling.Kabalrespons
import no.nav.etterlatte.libs.common.behandling.Klage
import no.nav.etterlatte.libs.common.behandling.KlageBrevInnstilling
import no.nav.etterlatte.libs.common.behandling.KlageResultat
import no.nav.etterlatte.libs.common.behandling.KlageUtfall
import no.nav.etterlatte.libs.common.behandling.KlageUtfallMedData
import no.nav.etterlatte.libs.common.behandling.KlageUtfallUtenBrev
import no.nav.etterlatte.libs.common.behandling.KlageVedtak
import no.nav.etterlatte.libs.common.behandling.KlageVedtaksbrev
import no.nav.etterlatte.libs.common.behandling.SendtInnstillingsbrev
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeFunnetException
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeTillattException
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.klage.AarsakTilAvbrytelse
import no.nav.etterlatte.libs.common.klage.KlageHendelseType
import no.nav.etterlatte.libs.common.klage.StatistikkKlage
import no.nav.etterlatte.libs.common.oppgave.OppgaveIntern
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.oppgave.Status
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

    fun lagreInitieltUtfallMedBegrunnelseAvKlage(
        klageId: UUID,
        utfall: InitieltUtfallMedBegrunnelseDto,
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

    fun avbrytKlage(
        klageId: UUID,
        aarsak: AarsakTilAvbrytelse,
        kommentar: String,
        saksbehandler: Saksbehandler,
    )

    fun fattVedtak(
        klageId: UUID,
        saksbehandler: Saksbehandler,
    ): Klage

    fun attesterVedtak(
        klageId: UUID,
        kommentar: String,
        saksbehandler: Saksbehandler,
    ): Klage

    fun underkjennVedtak(
        klageId: UUID,
        kommentar: String,
        valgtBegrunnelse: String,
        saksbehandler: Saksbehandler,
    ): Klage
}

class ManglerSaksbehandlerException(msg: String) : UgyldigForespoerselException(
    code = "MANGLER_SAKSBEHANDLER_PAA_OPPGAVE",
    detail = msg,
)

class KlageServiceImpl(
    private val klageDao: KlageDao,
    private val sakDao: SakDao,
    private val hendelseDao: HendelseDao,
    private val oppgaveService: OppgaveService,
    private val brevApiKlient: BrevApiKlient,
    private val klageKlient: KlageKlient,
    private val klageHendelser: IKlageHendelserService,
    private val vedtakKlient: VedtakKlient,
    private val featureToggleService: FeatureToggleService,
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

        klageHendelser.sendKlageHendelseRapids(
            StatistikkKlage(klage.id, klage, Tidspunkt.now()),
            KlageHendelseType.OPPRETTET,
        )

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

    override fun lagreInitieltUtfallMedBegrunnelseAvKlage(
        klageId: UUID,
        utfall: InitieltUtfallMedBegrunnelseDto,
        saksbehandler: Saksbehandler,
    ): Klage {
        if (utfall.utfall == KlageUtfall.DELVIS_OMGJOERING &&
            !featureToggleService.isEnabled(
                KlageFeatureToggle.StoetterUtfallDelvisOmgjoering,
                false,
            )
        ) {
            throw FeatureIkkeStoettetException()
        }
        val klage = klageDao.hentKlage(klageId) ?: throw KlageIkkeFunnetException(klageId)
        val oppdatertKlage = klage.oppdaterInitieltUtfallMedBegrunnelse(utfall, saksbehandler.ident)
        klageDao.lagreKlage(oppdatertKlage)
        return oppdatertKlage
    }

    override fun lagreUtfallAvKlage(
        klageId: UUID,
        utfall: KlageUtfallUtenBrev,
        saksbehandler: Saksbehandler,
    ): Klage {
        if (utfall is KlageUtfallUtenBrev.DelvisOmgjoering &&
            !featureToggleService.isEnabled(
                KlageFeatureToggle.StoetterUtfallDelvisOmgjoering,
                false,
            )
        ) {
            throw FeatureIkkeStoettetException()
        }

        val klage = klageDao.hentKlage(klageId) ?: throw KlageIkkeFunnetException(klageId)
        if (klage.utfall is KlageUtfallMedData.Avvist) {
            // Vi må rydde bort det vedtaksbrevet som ble opprettet ved forrige utfall
            runBlocking { brevApiKlient.slettVedtaksbrev(klageId, saksbehandler) }
        }

        val utfallMedBrev =
            when (utfall) {
                is KlageUtfallUtenBrev.Omgjoering ->
                    KlageUtfallMedData.Omgjoering(
                        omgjoering = utfall.omgjoering,
                        saksbehandler = Grunnlagsopplysning.Saksbehandler.create(saksbehandler.ident),
                    )

                is KlageUtfallUtenBrev.DelvisOmgjoering ->
                    KlageUtfallMedData.DelvisOmgjoering(
                        omgjoering = utfall.omgjoering,
                        innstilling =
                            InnstillingTilKabal(
                                lovhjemmel = enumValueOf(utfall.innstilling.lovhjemmel),
                                internKommentar = utfall.innstilling.internKommentar,
                                innstillingTekst = utfall.innstilling.innstillingTekst,
                                brev = brevForInnstilling(klage, saksbehandler),
                            ),
                        saksbehandler = Grunnlagsopplysning.Saksbehandler.create(saksbehandler.ident),
                    )

                is KlageUtfallUtenBrev.StadfesteVedtak ->
                    KlageUtfallMedData.StadfesteVedtak(
                        innstilling =
                            InnstillingTilKabal(
                                lovhjemmel = enumValueOf(utfall.innstilling.lovhjemmel),
                                internKommentar = utfall.innstilling.internKommentar,
                                innstillingTekst = utfall.innstilling.innstillingTekst,
                                brev = brevForInnstilling(klage, saksbehandler),
                            ),
                        saksbehandler = Grunnlagsopplysning.Saksbehandler.create(saksbehandler.ident),
                    )

                is KlageUtfallUtenBrev.Avvist -> {
                    val (vedtak, brev) = opprettVedtakOgBrev(klage, saksbehandler)
                    KlageUtfallMedData.Avvist(
                        saksbehandler = Grunnlagsopplysning.Saksbehandler.create(saksbehandler.ident),
                        vedtak = vedtak,
                        brev = brev,
                    )
                }

                is KlageUtfallUtenBrev.AvvistMedOmgjoering ->
                    KlageUtfallMedData.AvvistMedOmgjoering(
                        omgjoering = utfall.omgjoering,
                        saksbehandler = Grunnlagsopplysning.Saksbehandler.create(saksbehandler.ident),
                    )
            }

        val klageMedOppdatertUtfall = klage.oppdaterUtfall(utfallMedBrev)
        klageDao.lagreKlage(klageMedOppdatertUtfall)

        return klageMedOppdatertUtfall
    }

    private fun opprettVedtakOgBrev(
        klage: Klage,
        saksbehandler: Saksbehandler,
    ): Pair<KlageVedtak, KlageVedtaksbrev> {
        return when (val utfall = klage.utfall) {
            is KlageUtfallMedData.Avvist -> Pair(utfall.vedtak, utfall.brev)
            else -> {
                return runBlocking {
                    val vedtakId = lagreVedtakForAvvisning(klage, saksbehandler)
                    val vedtaksbrevId = opprettVedtaksbrevForAvvisning(klage, saksbehandler)
                    Pair(vedtakId, vedtaksbrevId)
                }
            }
        }
    }

    private fun brevForInnstilling(
        klage: Klage,
        saksbehandler: Saksbehandler,
    ): KlageBrevInnstilling {
        return when (val utfall = klage.utfall) {
            is KlageUtfallMedData.DelvisOmgjoering -> utfall.innstilling.brev
            is KlageUtfallMedData.StadfesteVedtak -> utfall.innstilling.brev
            else -> {
                val brev =
                    runBlocking {
                        brevApiKlient.opprettKlageOversendelsesbrevISak(
                            klage.id,
                            saksbehandler,
                        )
                    }
                KlageBrevInnstilling(brev.id)
            }
        }
    }

    private fun opprettVedtaksbrevForAvvisning(
        klage: Klage,
        saksbehandler: Saksbehandler,
    ): KlageVedtaksbrev {
        val brevDto = runBlocking { brevApiKlient.opprettVedtaksbrev(klage.id, klage.sak.id, saksbehandler) }
        return KlageVedtaksbrev(brevDto.id)
    }

    private fun lagreVedtakForAvvisning(
        klage: Klage,
        saksbehandler: Saksbehandler,
    ): KlageVedtak {
        val vedtak = runBlocking { vedtakKlient.lagreVedtakKlage(klage, saksbehandler) }
        return KlageVedtak(vedtak.id)
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
        val klage = klageDao.hentKlage(klageId) ?: throw KlageIkkeFunnetException(klageId)

        if (!klage.kanFerdigstille()) {
            throw UgyldigForespoerselException(
                code = "KLAGE_KAN_IKKE_FERDIGSTILLES",
                detail = "Klagen med id=$klageId kan ikke ferdigstilles siden den har feil status / tilstand",
            )
        }
        sjekkAtSaksbehandlerHarOppgaven(klageId, saksbehandler, "ferdigstille behandlingen")

        val (innstilling, omgjoering) =
            when (val utfall = klage.utfall) {
                is KlageUtfallMedData.StadfesteVedtak -> utfall.innstilling to null
                is KlageUtfallMedData.DelvisOmgjoering -> utfall.innstilling to utfall.omgjoering
                is KlageUtfallMedData.Omgjoering -> null to utfall.omgjoering
                is KlageUtfallMedData.Avvist -> null to null
                is KlageUtfallMedData.AvvistMedOmgjoering -> null to utfall.omgjoering
                null -> null to null
            }

        val oppgaveOmgjoering = omgjoering?.let { lagOppgaveForOmgjoering(klage) }
        val sendtInnstillingsbrev =
            innstilling?.let { runBlocking { ferdigstillInnstilling(it, klage, saksbehandler) } }

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

        klageHendelser.sendKlageHendelseRapids(
            StatistikkKlage(klage.id, klage, Tidspunkt.now(), saksbehandler.ident),
            KlageHendelseType.FERDIGSTILT,
        )

        return klageMedOppdatertResultat
    }

    override fun avbrytKlage(
        klageId: UUID,
        aarsak: AarsakTilAvbrytelse,
        kommentar: String,
        saksbehandler: Saksbehandler,
    ) {
        val klage =
            hentKlage(klageId)
                ?: throw NotFoundException("Klage med id=$klageId finnes ikke")
        if (!klage.kanAvbryte()) {
            throw IkkeTillattException("KLAGE_KAN_IKKE_AVBRYTES", "Klage med id=$klageId kan ikke avbrytes")
        }

        val avbruttKlage = klage.avbryt(aarsak)

        klageDao.lagreKlage(avbruttKlage).also {
            oppgaveService.avbrytOppgaveUnderBehandling(klageId.toString(), saksbehandler)

            hendelseDao.klageHendelse(
                klageId = avbruttKlage.id,
                sakId = avbruttKlage.sak.id,
                hendelse = KlageHendelseType.AVBRUTT,
                inntruffet = Tidspunkt.now(),
                saksbehandler = saksbehandler.ident(),
                kommentar = kommentar,
                begrunnelse = aarsak.name,
            )
        }

        klageHendelser.sendKlageHendelseRapids(
            StatistikkKlage(avbruttKlage.id, avbruttKlage, Tidspunkt.now(), saksbehandler.ident),
            KlageHendelseType.AVBRUTT,
        )
    }

    override fun fattVedtak(
        klageId: UUID,
        saksbehandler: Saksbehandler,
    ): Klage {
        val klage = klageDao.hentKlage(klageId) ?: throw NotFoundException("Klage med id=$klageId finnes ikke")
        sjekkAtSaksbehandlerHarOppgaven(klageId, saksbehandler, "fatte vedtak")

        val oppdatertKlage = klage.fattVedtak()
        val vedtak =
            runBlocking {
                vedtakKlient.fattVedtakKlage(klage, saksbehandler)
            }
        sjekkVedtakIdLagretIUtfall(klage, vedtak.id)
        klageDao.lagreKlage(oppdatertKlage)

        hendelseDao.vedtakHendelse(
            behandlingId = klage.id,
            sakId = klage.sak.id,
            vedtakId = vedtak.id,
            hendelse = HendelseType.FATTET,
            inntruffet = Tidspunkt.now(),
            saksbehandler = saksbehandler.ident(),
            kommentar = null,
            begrunnelse = null,
        )

        oppgaveService.hentOppgaveUnderBehandlingForReferanse(klageId.toString()).let { oppgave ->
            oppgaveService.oppdaterStatusOgMerknad(
                oppgave.id,
                "Vedtak er fattet",
                Status.UNDER_BEHANDLING,
            )
            oppgaveService.fjernSaksbehandler(oppgave.id)
        }

        return oppdatertKlage
    }

    private fun sjekkAtSaksbehandlerHarOppgaven(
        klageId: UUID,
        saksbehandler: Saksbehandler,
        handling: String,
    ) {
        val saksbehandlerForKlageOppgave =
            oppgaveService.hentSaksbehandlerForOppgaveUnderArbeidByReferanse(klageId.toString())
                ?: throw ManglerSaksbehandlerException("Fant ingen saksbehandler på oppgave relatert til klageid $klageId")

        if (saksbehandlerForKlageOppgave.ident != saksbehandler.ident) {
            throw UgyldigForespoerselException(
                code = "SAKSBEHANDLER_HAR_IKKE_OPPGAVEN",
                detail =
                    "Saksbehandler er ikke ansvarlig på oppgaven til klagen med id=$klageId, " +
                        "og kan derfor ikke $handling",
            )
        }
    }

    private fun sjekkVedtakIdLagretIUtfall(
        klage: Klage,
        vedtakId: Long,
    ) {
        val utfall = klage.utfall as KlageUtfallMedData.Avvist
        if (vedtakId != utfall.vedtak.vedtakId) {
            throw IllegalStateException(
                "VedtakId=$vedtakId er forskjellig fra det som ligger i utfall=$vedtakId",
            )
        }
    }

    override fun attesterVedtak(
        klageId: UUID,
        kommentar: String,
        saksbehandler: Saksbehandler,
    ): Klage {
        logger.info("Attesterer vedtak for klage=$klageId")
        val klage = klageDao.hentKlage(klageId) ?: throw NotFoundException("Klage med id=$klageId finnes ikke")
        sjekkAtSaksbehandlerHarOppgaven(klageId, saksbehandler, "attestere vedtak")

        val oppdatertKlage = klage.attesterVedtak()

        checkNotNull(klage.utfall as? KlageUtfallMedData.Avvist) {
            "Vi har en klage som kunne attesteres, men har feil utfall lagret. Id: $klageId"
        }
        runBlocking {
            brevApiKlient.ferdigstillBrev(klage.id, klage.sak.id, saksbehandler)
        }
        val vedtak =
            runBlocking {
                vedtakKlient.attesterVedtakKlage(
                    klage = klage,
                    brukerTokenInfo = saksbehandler,
                )
            }
        sjekkVedtakIdLagretIUtfall(oppdatertKlage, vedtak.id)
        klageDao.lagreKlage(oppdatertKlage)

        hendelseDao.vedtakHendelse(
            behandlingId = klageId,
            sakId = klage.sak.id,
            vedtakId = vedtak.id,
            hendelse = HendelseType.ATTESTERT,
            inntruffet = Tidspunkt.now(),
            saksbehandler = saksbehandler.ident(),
            kommentar = kommentar,
            begrunnelse = null,
        )

        oppgaveService.ferdigStillOppgaveUnderBehandling(klageId.toString(), saksbehandler)
        return oppdatertKlage
    }

    override fun underkjennVedtak(
        klageId: UUID,
        kommentar: String,
        valgtBegrunnelse: String,
        saksbehandler: Saksbehandler,
    ): Klage {
        logger.info("Underkjenner vedtak for klage=$klageId")
        val klage = klageDao.hentKlage(klageId) ?: throw NotFoundException("Klage med id=$klageId finnes ikke")

        val oppdatertKlage = klage.underkjennVedtak()

        val vedtak =
            runBlocking {
                vedtakKlient.underkjennVedtakKlage(
                    klageId = klageId,
                    brukerTokenInfo = saksbehandler,
                )
            }

        klageDao.lagreKlage(oppdatertKlage)

        hendelseDao.vedtakHendelse(
            behandlingId = klage.id,
            sakId = klage.sak.id,
            vedtakId = vedtak.id,
            hendelse = HendelseType.UNDERKJENT,
            inntruffet = Tidspunkt.now(),
            saksbehandler = saksbehandler.ident(),
            kommentar = kommentar,
            begrunnelse = valgtBegrunnelse,
        )

        oppgaveService.hentOppgaveUnderBehandlingForReferanse(klageId.toString()).let { oppgave ->
            oppgaveService.oppdaterStatusOgMerknad(
                oppgave.id,
                "Vedtak er underkjent",
                Status.UNDER_BEHANDLING,
            )
            oppgaveService.fjernSaksbehandler(oppgave.id)
        }

        return oppdatertKlage
    }

    private suspend fun ferdigstillInnstilling(
        innstilling: InnstillingTilKabal,
        klage: Klage,
        saksbehandler: Saksbehandler,
    ): SendtInnstillingsbrev {
        val innstillingsbrev = innstilling.brev
        val (tidJournalfoert, journalpostId) =
            ferdigstillOgDistribuerBrev(
                sakId = klage.sak.id,
                brevId = innstillingsbrev.brevId,
                saksbehandler = saksbehandler,
            )
        val brev =
            brevApiKlient.hentBrev(
                sakId = klage.sak.id,
                brevId = innstillingsbrev.brevId,
                brukerTokenInfo = saksbehandler,
            )
        val notatTilKa =
            brevApiKlient.journalfoerNotatKa(
                klage = klage,
                brukerInfoToken = saksbehandler,
            )
        logger.info(
            "Journalførte notat til KA for innstilling i klageId=${klage.id} på " +
                "journalpostId=${notatTilKa.journalpostId}",
        )

        klageKlient.sendKlageTilKabal(
            klage = klage,
            ekstradataInnstilling =
                EkstradataInnstilling(
                    mottakerInnstilling = brev.mottaker,
                    // TODO: Håndter verge
                    vergeEllerFullmektig = null,
                    journalpostInnstillingsbrev = notatTilKa.journalpostId,
                    journalpostKlage = klage.innkommendeDokument?.journalpostId,
                    // TODO: koble på når vi har journalpost soeknad inn
                    journalpostSoeknad = null,
                    // TODO: hent ut vedtaksbrev for vedtaket
                    journalpostVedtak = null,
                ),
        )
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
        val eksisterendeInnstillingsbrev = brevApiKlient.hentBrev(sakId, brevId, saksbehandler)
        if (eksisterendeInnstillingsbrev.status.ikkeFerdigstilt()) {
            brevApiKlient.ferdigstillOversendelseBrev(sakId, brevId, saksbehandler)
        } else {
            logger.info("Brev med id=$brevId har status ${eksisterendeInnstillingsbrev.status} og er allerede ferdigstilt")
        }

        val journalpostIdJournalfoering =
            if (eksisterendeInnstillingsbrev.status.ikkeJournalfoert()) {
                brevApiKlient.journalfoerBrev(sakId, brevId, saksbehandler).journalpostId
            } else {
                logger.info(
                    "Brev med id=$brevId har status ${eksisterendeInnstillingsbrev.status} og er allerede " +
                        "journalført på journalpostId=${eksisterendeInnstillingsbrev.journalpostId}",
                )
                requireNotNull(eksisterendeInnstillingsbrev.journalpostId) {
                    "Har et brev med id=$brevId med status=${eksisterendeInnstillingsbrev.status} som mangler journalpostId"
                }
            }
        val tidspunktJournalfoert = Tidspunkt.now()

        if (eksisterendeInnstillingsbrev.status.ikkeDistribuert()) {
            val bestillingsIdDistribuering = brevApiKlient.distribuerBrev(sakId, brevId, saksbehandler).bestillingsId
            logger.info(
                "Distribusjon av innstillingsbrevet med id=$brevId bestilt til klagen i sak med sakId=$sakId, " +
                    "med bestillingsId $bestillingsIdDistribuering",
            )
        } else {
            logger.info(
                "Brev med id=$brevId har status ${eksisterendeInnstillingsbrev.status} og er allerede " +
                    "distribuert med bestillingsid=${eksisterendeInnstillingsbrev.bestillingsID}",
            )
        }

        return tidspunktJournalfoert to journalpostIdJournalfoering
    }
}

class OmgjoeringMaaGjeldeEtVedtakException(klage: Klage) :
    UgyldigForespoerselException(
        code = "OMGJOERING_MAA_GJELDE_ET_VEDTAK",
        detail = "Klagen med id=${klage.id} skal resultere i en omgjøring, men mangler et vedtak som klagen gjelder",
    )

class KlageIkkeFunnetException(klageId: UUID) :
    IkkeFunnetException(code = "KLAGE_IKKE_FUNNET", detail = "Kunne ikke finne klage med id=$klageId")

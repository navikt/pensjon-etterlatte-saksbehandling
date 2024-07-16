package no.nav.etterlatte.rivers.opprettogjournalfoer

import no.nav.etterlatte.brev.Brevkoder
import no.nav.etterlatte.brev.Brevoppretter
import no.nav.etterlatte.brev.EtterlatteBrevKode
import no.nav.etterlatte.brev.behandling.Avdoed
import no.nav.etterlatte.brev.hentinformasjon.grunnlag.GrunnlagService
import no.nav.etterlatte.brev.model.BrevID
import no.nav.etterlatte.brev.model.ManueltBrevData
import no.nav.etterlatte.brev.model.bp.BarnepensjonInformasjonDoedsfall
import no.nav.etterlatte.brev.model.oms.OmstillingsstoenadInformasjonDoedsfall
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.retryOgPakkUt
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.libs.ktor.token.Systembruker
import no.nav.etterlatte.rapidsandrivers.BOR_I_UTLAND_KEY
import no.nav.etterlatte.rapidsandrivers.ER_OVER_18_AAR
import no.nav.etterlatte.rivers.FerdigstillJournalfoerOgDistribuerBrev
import no.nav.helse.rapids_rivers.JsonMessage
import org.slf4j.LoggerFactory

class OpprettJournalfoerOgDistribuerRiverException(
    override val detail: String,
    override val cause: Throwable?,
) : InternfeilException(detail, cause)

class OpprettJournalfoerOgDistribuerService(
    private val grunnlagService: GrunnlagService,
    private val brevoppretter: Brevoppretter,
    private val ferdigstillJournalfoerOgDistribuerBrev: FerdigstillJournalfoerOgDistribuerBrev,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    internal suspend fun opprettJournalfoerOgDistribuer(
        sakId: Long,
        brevKode: Brevkoder,
        brukerTokenInfo: BrukerTokenInfo,
        packet: JsonMessage,
    ): BrevID {
        logger.info("Oppretter $brevKode-brev i sak $sakId")

        val brevOgData =
            try {
                retryOgPakkUt {
                    brevoppretter.opprettBrev(
                        sakId = sakId,
                        behandlingId = null,
                        bruker = brukerTokenInfo,
                        brevKode = { brevKode.redigering },
                        brevtype = brevKode.redigering.brevtype,
                    ) {
                        when (brevKode.redigering) {
                            EtterlatteBrevKode.BARNEPENSJON_INFORMASJON_DOEDSFALL -> {
                                val borIutland = packet.hentVerdiEllerKastFeil(BOR_I_UTLAND_KEY).toBoolean()
                                val erOver18aar = packet.hentVerdiEllerKastFeil(ER_OVER_18_AAR).toBoolean()
                                opprettBarnepensjonInformasjonDoedsfall(sakId, borIutland, erOver18aar)
                            }
                            EtterlatteBrevKode.OMSTILLINGSSTOENAD_INFORMASJON_DOEDSFALL -> {
                                val borIutland = packet.hentVerdiEllerKastFeil(BOR_I_UTLAND_KEY).toBoolean()
                                opprettOmstillingsstoenadInformasjonDoedsfall(
                                    sakId,
                                    borIutland,
                                )
                            }
                            else -> ManueltBrevData()
                        }
                    }
                }
            } catch (e: Exception) {
                val feilMelding = "Fikk feil ved opprettelse av brev for sak $sakId for brevkode: $brevKode"
                logger.error(feilMelding)
                throw OpprettJournalfoerOgDistribuerRiverException(
                    feilMelding,
                    e,
                )
            }

        val brevID =
            ferdigstillJournalfoerOgDistribuerBrev.ferdigstillOgGenererPDF(
                brevKode,
                sakId,
                brukerTokenInfo,
                brevOgData.second,
                brevOgData.first.id,
            )
        ferdigstillJournalfoerOgDistribuerBrev.journalfoerOgDistribuer(
            brevKode,
            sakId,
            brevID,
            brukerTokenInfo,
        )
        return brevID
    }

    private suspend fun opprettBarnepensjonInformasjonDoedsfall(
        sakId: Long,
        borIutland: Boolean,
        erOver18aar: Boolean,
    ) = BarnepensjonInformasjonDoedsfall.fra(
        borIutland,
        erOver18aar,
        hentAvdoede(sakId),
    )

    private suspend fun opprettOmstillingsstoenadInformasjonDoedsfall(
        sakId: Long,
        borIutland: Boolean,
    ) = OmstillingsstoenadInformasjonDoedsfall.fra(
        borIutland,
        hentAvdoede(sakId),
    )

    private suspend fun hentAvdoede(sakId: Long): List<Avdoed> =
        grunnlagService.hentPersonerISak(grunnlagService.hentGrunnlagForSak(sakId, Systembruker.brev), null, null).avdoede
}

private fun JsonMessage.hentVerdiEllerKastFeil(key: String): String {
    val verdi = this[key].toString()
    if (verdi.isEmpty()) {
        throw RuntimeException("Må ha verdi for key $key")
    } else {
        return verdi
    }
}

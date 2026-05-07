package no.nav.etterlatte.brev

import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.aktivitetsplikt.AktivitetspliktService
import no.nav.etterlatte.behandling.klienter.BeregningKlient
import no.nav.etterlatte.behandling.klienter.TrygdetidKlient
import no.nav.etterlatte.behandling.klienter.VedtakInternalService
import no.nav.etterlatte.grunnlag.GrunnlagService
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.tidspunkt.toNorskTid
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import org.slf4j.LoggerFactory
import java.util.UUID

class BehandlingsvurderingNotatService(
    private val behandlingService: BehandlingService,
    private val trygdetidKlient: TrygdetidKlient,
    private val beregningKlient: BeregningKlient,
    private val aktivitetspliktService: AktivitetspliktService,
    private val grunnlagService: GrunnlagService,
    private val vedtakInternalService: VedtakInternalService,
    private val brevKlient: BrevKlient,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    suspend fun opprettOgJournalfoer(
        behandlingId: UUID,
        bruker: BrukerTokenInfo,
    ) {
        val behandling =
            behandlingService.hentDetaljertBehandling(behandlingId, bruker)
                ?: throw UgyldigForespoerselException(
                    code = "BEHANDLING_IKKE_FUNNET",
                    detail = "Fant ikke behandling $behandlingId",
                )

        if (behandling.status !in BehandlingStatus.iverksattEllerAttestert()) {
            throw BehandlingIkkeAttestertException(behandlingId)
        }

        val vedtak =
            vedtakInternalService.hentVedtak(behandlingId, bruker)
                ?: throw UgyldigForespoerselException(
                    code = "VEDTAK_IKKE_FUNNET",
                    detail = "Fant ikke vedtak for behandling $behandlingId",
                )

        logger.info("Bygger behandlingsvurdering-notat for behandling $behandlingId")

        val personopplysninger = grunnlagService.hentPersonopplysninger(behandlingId, behandling.sakType)
        val avdoede = personopplysninger.avdoede.map { it.opplysning.tilAvdoedPersonopplysninger() }

        val trygdetider = trygdetidKlient.hentTrygdetid(behandlingId, bruker)
        val trygdeavtale = trygdetidKlient.hentTrygdeavtale(behandlingId, bruker)

        val beregningsgrunnlag =
            when (vedtak.type) {
                // Vedtak med beregning har beregningsgrunnlag
                VedtakType.INNVILGELSE,
                VedtakType.ENDRING,
                VedtakType.INGEN_ENDRING,
                -> beregningKlient.hentBeregningsgrunnlag(behandlingId, bruker)

                // Vedtak uten beregnet ytelse har ikke beregningsgrunnlag
                VedtakType.AVSLAG,
                VedtakType.OPPHOER,
                -> null

                // Disse vedtakstypene er ikke "vanlige" behandlinger
                VedtakType.AVVIST_KLAGE,
                VedtakType.TILBAKEKREVING,
                -> throw UgyldigForespoerselException(
                    "BEHANDLING_NOTAT_IKKE_STOETTET",
                    "Behandlingsnotat er ikke støttet for vedtakstype ${vedtak.type}",
                )
            }

        val aktivitetsplikt =
            if (behandling.sakType == SakType.OMSTILLINGSSTOENAD) {
                aktivitetspliktService.hentAktivitetspliktDto(behandling.sak, behandlingId)
            } else {
                null
            }

        val slate =
            byggBehandlingsvurderingSlate(
                sakId = behandling.sak,
                vedtakId = vedtak.id,
                vedtakType = vedtak.type,
                datoAttestert =
                    vedtak.attestasjon
                        ?.tidspunkt
                        ?.toNorskTid()
                        ?.toLocalDate(),
                avdoede = avdoede,
                trygdetider = trygdetider,
                trygdeavtale = trygdeavtale,
                beregningsgrunnlag = beregningsgrunnlag,
                aktivitetsplikt = aktivitetsplikt,
            )

        brevKlient.opprettOgJournalfoerBehandlingsvurderingNotat(
            behandlingId = behandlingId,
            request = BehandlingsvurderingNotatRequest(sakId = behandling.sak, slate = slate),
            brukerTokenInfo = bruker,
        )
    }
}

class BehandlingIkkeAttestertException(
    behandlingId: UUID,
) : UgyldigForespoerselException(
        code = "BEHANDLING_IKKE_FERDIG_BEHANDLET",
        detail = "Behandling $behandlingId er ikke attestert eller iverksatt",
    )

package no.nav.etterlatte.testdata.automatisk

import com.github.michaelbull.result.mapBoth
import no.nav.etterlatte.behandling.VirkningstidspunktRequest
import no.nav.etterlatte.libs.common.behandling.Aldersgruppe
import no.nav.etterlatte.libs.common.behandling.BoddEllerArbeidetUtlandetRequest
import no.nav.etterlatte.libs.common.behandling.BrevutfallDto
import no.nav.etterlatte.libs.common.behandling.BrevutfallOgEtterbetalingDto
import no.nav.etterlatte.libs.common.behandling.JaNei
import no.nav.etterlatte.libs.common.behandling.JaNeiMedBegrunnelse
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.UtlandstilknytningType
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.oppgave.OppgaveIntern
import no.nav.etterlatte.libs.common.oppgave.SaksbehandlerEndringDto
import no.nav.etterlatte.libs.common.retryOgPakkUt
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.Resource
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.readValue
import no.nav.etterlatte.sak.UtlandstilknytningRequest
import no.nav.etterlatte.testdata.BEGRUNNELSE
import java.time.YearMonth
import java.util.UUID

class BehandlingService(
    private val klient: DownstreamResourceClient,
    private val url: String,
    private val clientId: String,
) {
    suspend fun hentSak(
        sakId: SakId,
        bruker: BrukerTokenInfo,
    ): Sak =
        retryOgPakkUt {
            klient.get(Resource(clientId, "$url/saker/${sakId.sakId}"), bruker).mapBoth(
                success = { readValue(it) },
                failure = { throw it },
            )
        }

    suspend fun settKommerBarnetTilGode(
        behandling: UUID,
        bruker: BrukerTokenInfo,
    ) {
        retryOgPakkUt {
            klient
                .post(
                    Resource(clientId, "$url/api/behandling/$behandling/kommerbarnettilgode"),
                    bruker,
                    postBody =
                        JaNeiMedBegrunnelse(
                            JaNei.JA,
                            BEGRUNNELSE,
                        ),
                ).mapBoth(
                    success = {},
                    failure = { throw it },
                )
        }
    }

    suspend fun lagreGyldighetsproeving(
        behandling: UUID,
        bruker: BrukerTokenInfo,
    ) {
        retryOgPakkUt {
            klient
                .post(
                    Resource(clientId, "$url/api/behandling/$behandling/gyldigfremsatt"),
                    bruker,
                    JaNeiMedBegrunnelse(JaNei.JA, BEGRUNNELSE),
                ).mapBoth(
                    success = {},
                    failure = { throw it },
                )
        }
    }

    suspend fun lagreUtlandstilknytning(
        behandling: UUID,
        bruker: BrukerTokenInfo,
    ) {
        retryOgPakkUt {
            klient
                .post(
                    Resource(clientId, "$url/api/behandling/$behandling/utlandstilknytning"),
                    bruker,
                    UtlandstilknytningRequest(
                        utlandstilknytningType = UtlandstilknytningType.NASJONAL,
                        begrunnelse = BEGRUNNELSE,
                    ),
                ).mapBoth(
                    success = {},
                    failure = { throw it },
                )
        }
    }

    suspend fun lagreBoddEllerArbeidetUtlandet(
        behandling: UUID,
        bruker: BrukerTokenInfo,
    ) {
        retryOgPakkUt {
            klient
                .post(
                    Resource(clientId, "$url/api/behandling/$behandling/boddellerarbeidetutlandet"),
                    bruker,
                    BoddEllerArbeidetUtlandetRequest(
                        boddEllerArbeidetUtlandet = false,
                        begrunnelse = BEGRUNNELSE,
                    ),
                ).mapBoth(
                    success = {},
                    failure = { throw it },
                )
        }
    }

    suspend fun lagreVirkningstidspunkt(
        behandling: UUID,
        bruker: BrukerTokenInfo,
        virkningstidspunkt: YearMonth,
    ) {
        retryOgPakkUt {
            klient
                .post(
                    Resource(clientId, "$url/api/behandling/$behandling/virkningstidspunkt"),
                    bruker,
                    VirkningstidspunktRequest(
                        _dato = virkningstidspunkt.toString(),
                        begrunnelse = BEGRUNNELSE,
                        kravdato = null,
                    ),
                ).mapBoth(
                    success = {},
                    failure = { throw it },
                )
        }
    }

    suspend fun tildelSaksbehandler(
        navn: String,
        sakId: SakId,
        bruker: BrukerTokenInfo,
    ) {
        val oppgaver: List<OppgaveIntern> =
            retryOgPakkUt {
                klient
                    .get(Resource(clientId, "$url/oppgaver/sak/${sakId.sakId}/oppgaver"), bruker)
                    .mapBoth(
                        success = { readValue(it) },
                        failure = { throw it },
                    )
            }

        oppgaver.forEach {
            retryOgPakkUt {
                klient
                    .post(
                        Resource(clientId, "$url/api/oppgaver/${it.id}/tildel-saksbehandler"),
                        bruker,
                        SaksbehandlerEndringDto(navn),
                    ).mapBoth(
                        success = {},
                        failure = { err -> throw err },
                    )
            }
        }
    }

    suspend fun lagreBrevutfall(
        behandling: UUID,
        sakType: SakType,
        bruker: BrukerTokenInfo,
    ) {
        retryOgPakkUt {
            klient
                .post(
                    Resource(clientId, "$url/api/behandling/$behandling/info/brevutfall"),
                    bruker,
                    BrevutfallOgEtterbetalingDto(
                        behandlingId = behandling,
                        opphoer = false,
                        etterbetaling = null,
                        brevutfall =
                            BrevutfallDto(
                                behandlingId = behandling,
                                aldersgruppe = if (sakType == SakType.BARNEPENSJON) Aldersgruppe.UNDER_18 else null,
                                feilutbetaling = null,
                                frivilligSkattetrekk = false,
                                kilde = Grunnlagsopplysning.automatiskSaksbehandler,
                            ),
                    ),
                ).mapBoth(
                    success = {},
                    failure = { throw it },
                )
        }
    }
}

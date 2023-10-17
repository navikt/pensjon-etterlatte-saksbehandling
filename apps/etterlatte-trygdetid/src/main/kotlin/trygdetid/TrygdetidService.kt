package no.nav.etterlatte.trygdetid

import com.fasterxml.jackson.databind.JsonNode
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.Prosesstype
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsdata
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning.RegelKilde
import no.nav.etterlatte.libs.common.grunnlag.hentDoedsdato
import no.nav.etterlatte.libs.common.grunnlag.hentFoedselsdato
import no.nav.etterlatte.libs.common.grunnlag.hentFoedselsnummer
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.toJsonNode
import no.nav.etterlatte.libs.common.trygdetid.DetaljertBeregnetTrygdetidResultat
import no.nav.etterlatte.token.BrukerTokenInfo
import no.nav.etterlatte.trygdetid.klienter.BehandlingKlient
import no.nav.etterlatte.trygdetid.klienter.GrunnlagKlient
import no.nav.etterlatte.trygdetid.klienter.VilkaarsvuderingKlient
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.util.UUID

interface TrygdetidService {
    suspend fun hentTrygdetid(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Trygdetid?

    suspend fun opprettTrygdetid(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Trygdetid

    suspend fun lagreTrygdetidGrunnlag(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        trygdetidGrunnlag: TrygdetidGrunnlag,
    ): Trygdetid

    suspend fun lagreYrkesskadeTrygdetidGrunnlag(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Trygdetid

    suspend fun slettTrygdetidGrunnlag(
        behandlingId: UUID,
        trygdetidGrunnlagId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Trygdetid

    suspend fun kopierSisteTrygdetidberegning(
        behandlingId: UUID,
        forrigeBehandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Trygdetid

    fun overstyrBeregnetTrygdetid(
        behandlingId: UUID,
        beregnetTrygdetid: DetaljertBeregnetTrygdetidResultat,
    ): Trygdetid

    fun overstyrNorskPoengaar(
        trygdetidId: UUID,
        behandlingsId: UUID,
        overstyrtNorskPoengaar: Int?,
    ): Trygdetid
}

class TrygdetidServiceImpl(
    private val trygdetidRepository: TrygdetidRepository,
    private val behandlingKlient: BehandlingKlient,
    private val grunnlagKlient: GrunnlagKlient,
    private val vilkaarsvurderingKlient: VilkaarsvuderingKlient,
    private val beregnTrygdetidService: TrygdetidBeregningService,
) : TrygdetidService {
    private val logger = LoggerFactory.getLogger(TrygdetidServiceImpl::class.java)

    override suspend fun hentTrygdetid(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Trygdetid? {
        return trygdetidRepository.hentTrygdetid(behandlingId)
            ?.let { trygdetid -> sjekkYrkesskadeForEndring(behandlingId, brukerTokenInfo, trygdetid) }
    }

    override suspend fun opprettTrygdetid(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Trygdetid =
        tilstandssjekk(
            behandlingId,
            brukerTokenInfo,
        ) {
            trygdetidRepository.hentTrygdetid(behandlingId)?.let {
                throw IllegalArgumentException("Trygdetid finnes allerede for behandling $behandlingId")
            }
            val behandling = behandlingKlient.hentBehandling(behandlingId, brukerTokenInfo)
            when (behandling.behandlingType) {
                BehandlingType.FØRSTEGANGSBEHANDLING -> {
                    logger.info("Oppretter trygdetid for behandling $behandlingId")
                    opprettTrygdetid(behandling, brukerTokenInfo)
                }

                BehandlingType.REVURDERING -> {
                    logger.info("Oppretter trygdetid for behandling $behandlingId for revurdering")
                    val forrigeBehandling =
                        behandlingKlient.hentSisteIverksatteBehandling(
                            behandling.sak,
                            brukerTokenInfo,
                        )

                    when (val forrigeTrygdetid = hentTrygdetid(forrigeBehandling.id, brukerTokenInfo)) {
                        null -> opprettTrygdetidForRevurdering(behandling, brukerTokenInfo)
                        else -> kopierSisteTrygdetidberegning(behandling, forrigeTrygdetid)
                    }
                }

                else -> throw RuntimeException(
                    "Støtter ikke trygdetid for behandlingType=${behandling.behandlingType}",
                )
            }
        }.also { behandlingKlient.settBehandlingStatusTrygdetidOppdatert(behandlingId, brukerTokenInfo) }

    private suspend fun opprettTrygdetidForRevurdering(
        behandling: DetaljertBehandling,
        brukerTokenInfo: BrukerTokenInfo,
    ) = if (behandling.revurderingsaarsak == RevurderingAarsak.REGULERING &&
        behandling.prosesstype == Prosesstype.AUTOMATISK
    ) {
        logger.info("Forrige trygdetid for ${behandling.id} finnes ikke - må reguleres manuelt")
        throw RuntimeException(
            "Forrige trygdetid for ${behandling.id} finnes ikke - må reguleres manuelt",
        )
    } else {
        logger.info("Oppretter trygdetid for behandling ${behandling.id} revurdering")
        opprettTrygdetid(behandling, brukerTokenInfo)
    }

    private suspend fun opprettTrygdetid(
        behandling: DetaljertBehandling,
        brukerTokenInfo: BrukerTokenInfo,
    ): Trygdetid {
        val avdoed = grunnlagKlient.hentGrunnlag(behandling.sak, behandling.id, brukerTokenInfo).hentAvdoed()
        val trygdetid =
            Trygdetid(
                sakId = behandling.sak,
                behandlingId = behandling.id,
                opplysninger = hentOpplysninger(avdoed),
                ident = avdoed.hentFoedselsnummer()?.verdi?.value,
            )
        return trygdetidRepository.opprettTrygdetid(trygdetid)
    }

    private suspend fun sjekkYrkesskadeForEndring(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        trygdetid: Trygdetid,
    ): Trygdetid {
        val vurdering = vilkaarsvurderingKlient.hentVilkaarsvurdering(behandlingId, brukerTokenInfo)

        if (trygdetid.isYrkesskade() != vurdering.isYrkesskade()) {
            return trygdetid.copy(trygdetidGrunnlag = emptyList()).nullstillBeregnetTrygdetid()
        }

        return trygdetid
    }

    override suspend fun lagreYrkesskadeTrygdetidGrunnlag(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Trygdetid =
        tilstandssjekk(behandlingId, brukerTokenInfo) {
            val gjeldendeTrygdetid: Trygdetid =
                trygdetidRepository.hentTrygdetid(behandlingId)
                    ?: throw Exception("Fant ikke gjeldende trygdetid for behandlingId=$behandlingId")

            val sjekketGjeldendeTrygdetid = sjekkYrkesskadeForEndring(behandlingId, brukerTokenInfo, gjeldendeTrygdetid)

            val nyBeregnetTrygdetid =
                beregnTrygdetidService.beregnTrygdetidForYrkesskade(
                    Grunnlagsopplysning.Saksbehandler(
                        brukerTokenInfo.ident(),
                        Tidspunkt.now(),
                    ),
                )

            val nyTrygdetid = sjekketGjeldendeTrygdetid.oppdaterBeregnetTrygdetid(nyBeregnetTrygdetid)
            trygdetidRepository.oppdaterTrygdetid(nyTrygdetid).also {
                behandlingKlient.settBehandlingStatusTrygdetidOppdatert(behandlingId, brukerTokenInfo)
            }
        }

    override suspend fun lagreTrygdetidGrunnlag(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        trygdetidGrunnlag: TrygdetidGrunnlag,
    ): Trygdetid =
        tilstandssjekk(behandlingId, brukerTokenInfo) {
            val gjeldendeTrygdetid: Trygdetid =
                trygdetidRepository.hentTrygdetid(behandlingId)
                    ?: throw Exception("Fant ikke gjeldende trygdetid for behandlingId=$behandlingId")

            val datoer = hentDatoerForBehandling(behandlingId, brukerTokenInfo)

            val sjekketGjeldendeTrygdetid =
                sjekkYrkesskadeForEndring(behandlingId, brukerTokenInfo, gjeldendeTrygdetid)

            val trygdetidGrunnlagBeregnet: TrygdetidGrunnlag =
                trygdetidGrunnlag.oppdaterBeregnetTrygdetid(
                    beregnetTrygdetid = beregnTrygdetidService.beregnTrygdetidGrunnlag(trygdetidGrunnlag),
                )

            val trygdetidMedOppdatertTrygdetidGrunnlag: Trygdetid =
                sjekketGjeldendeTrygdetid.leggTilEllerOppdaterTrygdetidGrunnlag(trygdetidGrunnlagBeregnet)

            val nyBeregnetTrygdetid: DetaljertBeregnetTrygdetid? =
                beregnTrygdetidService.beregnTrygdetid(
                    trygdetidGrunnlag = trygdetidMedOppdatertTrygdetidGrunnlag.trygdetidGrunnlag,
                    datoer.foedselsDato,
                    datoer.doedsDato,
                )

            when (nyBeregnetTrygdetid) {
                null -> trygdetidMedOppdatertTrygdetidGrunnlag.nullstillBeregnetTrygdetid()
                else -> trygdetidMedOppdatertTrygdetidGrunnlag.oppdaterBeregnetTrygdetid(nyBeregnetTrygdetid)
            }.also { nyTrygdetid ->
                trygdetidRepository.oppdaterTrygdetid(nyTrygdetid).also {
                    behandlingKlient.settBehandlingStatusTrygdetidOppdatert(behandlingId, brukerTokenInfo)
                }
            }
        }

    private data class DatoerForBehandling(
        val foedselsDato: LocalDate,
        val doedsDato: LocalDate,
    )

    private suspend fun hentDatoerForBehandling(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): DatoerForBehandling {
        val behandling = behandlingKlient.hentBehandling(behandlingId, brukerTokenInfo)

        val avdoed = grunnlagKlient.hentGrunnlag(behandling.sak, behandlingId, brukerTokenInfo).hentAvdoed()

        return DatoerForBehandling(
            foedselsDato =
                avdoed.hentFoedselsdato()?.verdi
                    ?: throw Exception("Fant ikke foedselsdato for avdoed for behandlingId=$behandlingId"),
            doedsDato =
                avdoed.hentDoedsdato()?.verdi
                    ?: throw Exception("Fant ikke doedsdato for avdoed for behandlingId=$behandlingId"),
        )
    }

    override suspend fun slettTrygdetidGrunnlag(
        behandlingId: UUID,
        trygdetidGrunnlagId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Trygdetid =
        tilstandssjekk(behandlingId, brukerTokenInfo) {
            val trygdetid =
                trygdetidRepository.hentTrygdetid(behandlingId)?.slettTrygdetidGrunnlag(trygdetidGrunnlagId)
                    ?: throw Exception("Fant ikke gjeldende trygdetid for behandlingId=$behandlingId")

            val datoer = hentDatoerForBehandling(behandlingId, brukerTokenInfo)

            when (
                val nyBeregnetTrygdetid =
                    beregnTrygdetidService.beregnTrygdetid(
                        trygdetid.trygdetidGrunnlag,
                        datoer.foedselsDato,
                        datoer.doedsDato,
                    )
            ) {
                null -> trygdetid.nullstillBeregnetTrygdetid()
                else -> trygdetid.oppdaterBeregnetTrygdetid(nyBeregnetTrygdetid)
            }.also { nyTrygdetid ->
                trygdetidRepository.oppdaterTrygdetid(nyTrygdetid).also {
                    behandlingKlient.settBehandlingStatusTrygdetidOppdatert(behandlingId, brukerTokenInfo)
                }
            }
        }

    override suspend fun kopierSisteTrygdetidberegning(
        behandlingId: UUID,
        forrigeBehandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Trygdetid {
        val behandling = behandlingKlient.hentBehandling(behandlingId, brukerTokenInfo)
        return kopierSisteTrygdetidberegning(behandling, forrigeBehandlingId, brukerTokenInfo)
    }

    private suspend fun kopierSisteTrygdetidberegning(
        behandling: DetaljertBehandling,
        forrigeBehandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Trygdetid {
        logger.info("Kopierer trygdetid for behandling ${behandling.id} fra behandling $forrigeBehandlingId")

        val forrigeTrygdetid =
            requireNotNull(hentTrygdetid(forrigeBehandlingId, brukerTokenInfo)) {
                "Fant ingen trygdetid for behandlingId=$forrigeBehandlingId"
            }

        return kopierSisteTrygdetidberegning(behandling, forrigeTrygdetid)
    }

    private fun kopierSisteTrygdetidberegning(
        behandling: DetaljertBehandling,
        forrigeTrygdetid: Trygdetid,
    ): Trygdetid {
        logger.info("Kopierer trygdetid for behandling ${behandling.id} fra trygdetid ${forrigeTrygdetid.id}")

        val kopiertTrygdetid =
            Trygdetid(
                sakId = behandling.sak,
                behandlingId = behandling.id,
                opplysninger = forrigeTrygdetid.opplysninger.map { it.copy(id = UUID.randomUUID()) },
                trygdetidGrunnlag = forrigeTrygdetid.trygdetidGrunnlag.map { it.copy(id = UUID.randomUUID()) },
                beregnetTrygdetid = forrigeTrygdetid.beregnetTrygdetid,
                ident = forrigeTrygdetid.ident,
            )

        return trygdetidRepository.opprettTrygdetid(kopiertTrygdetid)
    }

    private fun kildeFoedselsnummer(): RegelKilde =
        RegelKilde(
            "Beregnet basert på fødselsdato fra pdl",
            Tidspunkt.now(),
            "1",
        )

    private fun hentOpplysninger(avdoed: Grunnlagsdata<JsonNode>): List<Opplysningsgrunnlag> {
        val foedselsdato = avdoed.hentFoedselsdato()
        val opplysninger =
            listOf(
                Opplysningsgrunnlag.ny(
                    TrygdetidOpplysningType.FOEDSELSDATO,
                    foedselsdato?.kilde,
                    foedselsdato?.verdi,
                ),
                Opplysningsgrunnlag.ny(
                    TrygdetidOpplysningType.FYLT_16,
                    kildeFoedselsnummer(),
                    // Ifølge paragraf § 3-5 regnes trygdetid fra tidspunkt en person er fylt 16 år
                    foedselsdato?.verdi?.plusYears(16),
                ),
                Opplysningsgrunnlag.ny(
                    TrygdetidOpplysningType.FYLLER_66,
                    kildeFoedselsnummer(),
                    // Ifølge paragraf § 3-5 regnes trygdetid frem til tidspunkt en person er fyller 66 pår
                    foedselsdato?.verdi?.plusYears(66),
                ),
                avdoed.hentDoedsdato().let {
                    Opplysningsgrunnlag.ny(TrygdetidOpplysningType.DOEDSDATO, it?.kilde, it?.verdi)
                },
            )
        return opplysninger
    }

    private suspend fun <T> tilstandssjekk(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        block: suspend () -> T,
    ): T {
        val kanFastsetteTrygdetid = behandlingKlient.kanOppdatereTrygdetid(behandlingId, brukerTokenInfo)
        return if (kanFastsetteTrygdetid) {
            block()
        } else {
            throw Exception("Kan ikke opprette/endre trygdetid da behandlingen er i feil tilstand")
        }
    }

    override fun overstyrBeregnetTrygdetid(
        behandlingId: UUID,
        beregnetTrygdetid: DetaljertBeregnetTrygdetidResultat,
    ): Trygdetid {
        val trygdetid =
            trygdetidRepository.hentTrygdetid(behandlingId)
                ?: throw Exception("Fant ikke gjeldende trygdetid for behandlingId=$behandlingId")

        return trygdetid.oppdaterBeregnetTrygdetid(
            DetaljertBeregnetTrygdetid(
                resultat = beregnetTrygdetid,
                tidspunkt = Tidspunkt.now(),
                regelResultat = "".toJsonNode(),
            ),
        ).also { nyTrygdetid ->
            trygdetidRepository.oppdaterTrygdetid(nyTrygdetid, true)
        }
    }

    override fun overstyrNorskPoengaar(
        trygdetidId: UUID,
        behandlingsId: UUID,
        overstyrtNorskPoengaar: Int?,
    ): Trygdetid {
        // TODO - EY-2849 - må bruke ID og ikke bare behandlingsId for å hente her
        val trygdetid =
            trygdetidRepository.hentTrygdetid(behandlingsId)
                ?: throw Exception("Fant ikke gjeldende trygdetid for id=$trygdetidId og behandlingId=$behandlingsId")

        return trygdetidRepository.oppdaterTrygdetid(trygdetid.copy(overstyrtNorskPoengaar = overstyrtNorskPoengaar))
    }
}

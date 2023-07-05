package no.nav.etterlatte.trygdetid

import com.fasterxml.jackson.databind.JsonNode
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.Prosesstype
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsdata
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning.RegelKilde
import no.nav.etterlatte.libs.common.grunnlag.hentDoedsdato
import no.nav.etterlatte.libs.common.grunnlag.hentFoedselsdato
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.token.BrukerTokenInfo
import no.nav.etterlatte.trygdetid.klienter.BehandlingKlient
import no.nav.etterlatte.trygdetid.klienter.GrunnlagKlient
import org.slf4j.LoggerFactory
import java.util.*

class TrygdetidService(
    private val trygdetidRepository: TrygdetidRepository,
    private val behandlingKlient: BehandlingKlient,
    private val grunnlagKlient: GrunnlagKlient,
    private val beregnTrygdetidService: TrygdetidBeregningService
) {

    private val logger = LoggerFactory.getLogger(TrygdetidService::class.java)

    fun hentTrygdetid(behandlingsId: UUID): Trygdetid? = trygdetidRepository.hentTrygdetid(behandlingsId)

    suspend fun opprettTrygdetid(behandlingId: UUID, brukerTokenInfo: BrukerTokenInfo): Trygdetid = tilstandssjekk(
        behandlingId,
        brukerTokenInfo
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
                val forrigeBehandlingId = behandlingKlient.hentSisteIverksatteBehandling(
                    behandling.sak,
                    brukerTokenInfo
                )

                when (val forrigeTrygdetid = hentTrygdetid(forrigeBehandlingId)) {
                    null -> opprettTrygdetidForRevurdering(behandling, brukerTokenInfo)
                    else -> kopierSisteTrygdetidberegning(behandling, forrigeTrygdetid)
                }
            }

            else -> throw RuntimeException(
                "Støtter ikke trygdetid for behandlingType=${behandling.behandlingType}"
            )
        }
    }

    private suspend fun opprettTrygdetidForRevurdering(
        behandling: DetaljertBehandling,
        brukerTokenInfo: BrukerTokenInfo
    ) =
        if (behandling.revurderingsaarsak == RevurderingAarsak.REGULERING &&
            behandling.prosesstype == Prosesstype.AUTOMATISK
        ) {
            logger.info("Forrige trygdetid for ${behandling.id} finnes ikke - må reguleres manuelt")
            throw RuntimeException(
                "Forrige trygdetid for ${behandling.id} finnes ikke - må reguleres manuelt"
            )
        } else {
            logger.info("Oppretter trygdetid for behandling ${behandling.id} revurdering")
            opprettTrygdetid(behandling, brukerTokenInfo)
        }

    private suspend fun opprettTrygdetid(
        behandling: DetaljertBehandling,
        brukerTokenInfo: BrukerTokenInfo
    ): Trygdetid {
        val avdoed = grunnlagKlient.hentGrunnlag(behandling.sak, brukerTokenInfo).hentAvdoed()
        val trygdetid = Trygdetid(
            sakId = behandling.sak,
            behandlingId = behandling.id,
            opplysninger = hentOpplysninger(avdoed)
        )
        return trygdetidRepository.opprettTrygdetid(trygdetid)
    }

    suspend fun lagreTrygdetidGrunnlag(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        trygdetidGrunnlag: TrygdetidGrunnlag
    ): Trygdetid = tilstandssjekk(behandlingId, brukerTokenInfo) {
        val trygdetidGrunnlagBeregnet: TrygdetidGrunnlag = trygdetidGrunnlag.oppdaterBeregnetTrygdetid(
            beregnetTrygdetid = beregnTrygdetidService.beregnTrygdetidGrunnlag(trygdetidGrunnlag)
        )

        val gjeldendeTrygdetid: Trygdetid = trygdetidRepository.hentTrygdetid(behandlingId)
            ?: throw Exception("Fant ikke gjeldende trygdetid for behandlingId=$behandlingId")

        val trygdetidMedOppdatertTrygdetidGrunnlag: Trygdetid =
            gjeldendeTrygdetid.leggTilEllerOppdaterTrygdetidGrunnlag(trygdetidGrunnlagBeregnet)

        val nyBeregnetTrygdetid: BeregnetTrygdetid? = beregnTrygdetidService.beregnTrygdetid(
            trygdetidGrunnlag = trygdetidMedOppdatertTrygdetidGrunnlag.trygdetidGrunnlag
        )

        when (nyBeregnetTrygdetid) {
            null -> trygdetidMedOppdatertTrygdetidGrunnlag.nullstillBeregnetTrygdetid()
            else -> trygdetidMedOppdatertTrygdetidGrunnlag.oppdaterBeregnetTrygdetid(nyBeregnetTrygdetid)
        }.also { nyTrygdetid ->
            trygdetidRepository.oppdaterTrygdetid(nyTrygdetid).also {
                behandlingKlient.settBehandlingStatusVilkaarsvurdert(behandlingId, brukerTokenInfo)
            }
        }
    }

    suspend fun slettTrygdetidGrunnlag(
        behandlingId: UUID,
        trygdetidGrunnlagId: UUID,
        brukerTokenInfo: BrukerTokenInfo
    ): Trygdetid =
        tilstandssjekk(behandlingId, brukerTokenInfo) {
            val trygdetid = trygdetidRepository.hentTrygdetid(behandlingId)?.slettTrygdetidGrunnlag(trygdetidGrunnlagId)
                ?: throw Exception("Fant ikke gjeldende trygdetid for behandlingId=$behandlingId")

            when (val nyBeregnetTrygdetid = beregnTrygdetidService.beregnTrygdetid(trygdetid.trygdetidGrunnlag)) {
                null -> trygdetid.nullstillBeregnetTrygdetid()
                else -> trygdetid.oppdaterBeregnetTrygdetid(nyBeregnetTrygdetid)
            }.also { nyTrygdetid ->
                trygdetidRepository.oppdaterTrygdetid(nyTrygdetid).also {
                    behandlingKlient.settBehandlingStatusVilkaarsvurdert(behandlingId, brukerTokenInfo)
                }
            }
        }

    suspend fun kopierSisteTrygdetidberegning(
        behandlingId: UUID,
        forrigeBehandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo
    ): Trygdetid {
        val behandling = behandlingKlient.hentBehandling(behandlingId, brukerTokenInfo)
        return kopierSisteTrygdetidberegning(behandling, forrigeBehandlingId)
    }

    private fun kopierSisteTrygdetidberegning(
        behandling: DetaljertBehandling,
        forrigeBehandlingId: UUID
    ): Trygdetid {
        logger.info("Kopierer trygdetid for behandling ${behandling.id} fra behandling $forrigeBehandlingId")

        val forrigeTrygdetid = requireNotNull(hentTrygdetid(forrigeBehandlingId)) {
            "Fant ingen trygdetid for behandlingId=$forrigeBehandlingId"
        }

        return kopierSisteTrygdetidberegning(behandling, forrigeTrygdetid)
    }

    private fun kopierSisteTrygdetidberegning(
        behandling: DetaljertBehandling,
        forrigeTrygdetid: Trygdetid
    ): Trygdetid {
        logger.info("Kopierer trygdetid for behandling ${behandling.id} fra trygdetid ${forrigeTrygdetid.id}")

        val kopiertTrygdetid = Trygdetid(
            sakId = behandling.sak,
            behandlingId = behandling.id,
            opplysninger = forrigeTrygdetid.opplysninger.map { it.copy(id = UUID.randomUUID()) },
            trygdetidGrunnlag = forrigeTrygdetid.trygdetidGrunnlag.map { it.copy(id = UUID.randomUUID()) },
            beregnetTrygdetid = forrigeTrygdetid.beregnetTrygdetid
        )

        return trygdetidRepository.opprettTrygdetid(kopiertTrygdetid)
    }

    private fun kildeFoedselsnummer(): RegelKilde = RegelKilde(
        "Beregnet basert på fødselsdato fra pdl",
        Tidspunkt.now(),
        "1"
    )

    private fun hentOpplysninger(avdoed: Grunnlagsdata<JsonNode>): List<Opplysningsgrunnlag> {
        val foedselsdato = avdoed.hentFoedselsdato()
        val opplysninger = listOf(
            Opplysningsgrunnlag.ny(
                TrygdetidOpplysningType.FOEDSELSDATO,
                foedselsdato?.kilde,
                foedselsdato?.verdi
            ),
            Opplysningsgrunnlag.ny(
                TrygdetidOpplysningType.FYLT_16,
                kildeFoedselsnummer(),
                // Ifølge paragraf § 3-5 regnes trygdetid fra tidspunkt en person er fylt 16 år
                foedselsdato?.verdi?.plusYears(16)
            ),
            Opplysningsgrunnlag.ny(
                TrygdetidOpplysningType.FYLLER_66,
                kildeFoedselsnummer(),
                // Ifølge paragraf § 3-5 regnes trygdetid frem til tidspunkt en person er fyller 66 pår
                foedselsdato?.verdi?.plusYears(66)
            ),
            avdoed.hentDoedsdato().let {
                Opplysningsgrunnlag.ny(TrygdetidOpplysningType.DOEDSDATO, it?.kilde, it?.verdi)
            }
        )
        return opplysninger
    }

    private suspend fun tilstandssjekk(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        block: suspend () -> Trygdetid
    ): Trygdetid {
        val kanFastsetteTrygdetid = behandlingKlient.kanBeregnes(behandlingId, brukerTokenInfo)
        return if (kanFastsetteTrygdetid) {
            block()
        } else {
            throw Exception("Kan ikke opprette/endre trygdetid da behandlingen er i feil tilstand")
        }
    }
}
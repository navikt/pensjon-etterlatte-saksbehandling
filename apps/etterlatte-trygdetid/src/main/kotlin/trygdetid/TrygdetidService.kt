package no.nav.etterlatte.trygdetid

import com.fasterxml.jackson.databind.JsonNode
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsdata
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning.RegelKilde
import no.nav.etterlatte.libs.common.grunnlag.hentDoedsdato
import no.nav.etterlatte.libs.common.grunnlag.hentFoedselsdato
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.token.Bruker
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

    suspend fun opprettTrygdetid(behandlingId: UUID, bruker: Bruker): Trygdetid =
        tilstandssjekk(behandlingId, bruker) {
            trygdetidRepository.hentTrygdetid(behandlingId)?.let {
                throw IllegalArgumentException("Trygdetid finnes allerede for behandling $behandlingId")
            }
            val behandling = behandlingKlient.hentBehandling(behandlingId, bruker)
            when (behandling.behandlingType) {
                BehandlingType.FØRSTEGANGSBEHANDLING -> {
                    logger.info("Oppretter trygdetid for behandling $behandlingId")
                    val avdoed = grunnlagKlient.hentGrunnlag(behandling.sak, bruker).hentAvdoed()
                    val trygdetid = Trygdetid(
                        sakId = behandling.sak,
                        behandlingId = behandling.id,
                        opplysninger = hentOpplysninger(avdoed)
                    )
                    trygdetidRepository.opprettTrygdetid(trygdetid)
                }
                BehandlingType.REVURDERING -> {
                    logger.info("Kopierer trygdetid for behandling $behandlingId fra forrige behandling")
                    val forrigeBehandling = behandlingKlient.hentSisteIverksatteBehandling(behandling.sak, bruker)
                    kopierSisteTrygdetidberegning(behandling, forrigeBehandling.id)
                }
                else -> throw RuntimeException(
                    "Støtter ikke trygdetid for behandlingType=${behandling.behandlingType}"
                )
            }
        }

    suspend fun lagreTrygdetidGrunnlag(
        behandlingId: UUID,
        bruker: Bruker,
        trygdetidGrunnlag: TrygdetidGrunnlag
    ): Trygdetid =
        tilstandssjekk(behandlingId, bruker) {
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
                    behandlingKlient.settBehandlingStatusVilkaarsvurdert(behandlingId, bruker)
                }
            }
        }

    suspend fun slettTrygdetidGrunnlag(behandlingId: UUID, trygdetidGrunnlagId: UUID, bruker: Bruker): Trygdetid =
        tilstandssjekk(behandlingId, bruker) {
            val trygdetid = trygdetidRepository.hentTrygdetid(behandlingId)?.slettTrygdetidGrunnlag(trygdetidGrunnlagId)
                ?: throw Exception("Fant ikke gjeldende trygdetid for behandlingId=$behandlingId")

            when (val nyBeregnetTrygdetid = beregnTrygdetidService.beregnTrygdetid(trygdetid.trygdetidGrunnlag)) {
                null -> trygdetid.nullstillBeregnetTrygdetid()
                else -> trygdetid.oppdaterBeregnetTrygdetid(nyBeregnetTrygdetid)
            }.also { nyTrygdetid ->
                trygdetidRepository.oppdaterTrygdetid(nyTrygdetid).also {
                    behandlingKlient.settBehandlingStatusVilkaarsvurdert(behandlingId, bruker)
                }
            }
        }

    suspend fun kopierSisteTrygdetidberegning(
        behandlingId: UUID,
        forrigeBehandlingId: UUID,
        bruker: Bruker
    ): Trygdetid {
        val behandling = behandlingKlient.hentBehandling(behandlingId, bruker)
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
        bruker: Bruker,
        block: suspend () -> Trygdetid
    ): Trygdetid {
        val kanFastsetteTrygdetid = behandlingKlient.kanBeregnes(behandlingId, bruker)
        return if (kanFastsetteTrygdetid) {
            block()
        } else {
            throw Exception("Kan ikke opprette/endre trygdetid da behandlingen er i feil tilstand")
        }
    }
}
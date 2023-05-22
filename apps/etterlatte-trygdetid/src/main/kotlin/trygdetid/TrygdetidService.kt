package no.nav.etterlatte.trygdetid

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning.RegelKilde
import no.nav.etterlatte.libs.common.grunnlag.hentDoedsdato
import no.nav.etterlatte.libs.common.grunnlag.hentFoedselsdato
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.token.Bruker
import no.nav.etterlatte.trygdetid.klienter.BehandlingKlient
import no.nav.etterlatte.trygdetid.klienter.GrunnlagKlient
import java.util.*

class TrygdetidService(
    private val trygdetidRepository: TrygdetidRepository,
    private val behandlingKlient: BehandlingKlient,
    private val grunnlagKlient: GrunnlagKlient,
    private val beregnTrygdetidService: TrygdetidBeregningService
) {
    fun hentTrygdetid(behandlingsId: UUID): Trygdetid? = trygdetidRepository.hentTrygdetid(behandlingsId)

    suspend fun opprettTrygdetid(behandlingId: UUID, bruker: Bruker): Trygdetid =
        tilstandssjekk(behandlingId, bruker) {
            trygdetidRepository.hentTrygdetid(behandlingId)?.let {
                throw IllegalArgumentException("Trygdetid finnes allerede for behandling $behandlingId")
            }
            val behandling = behandlingKlient.hentBehandling(behandlingId, bruker)
            val avdoed = grunnlagKlient.hentGrunnlag(behandling.sak, bruker).hentAvdoed()

            val foedselsdato = avdoed.hentFoedselsdato()
            val opplysninger = listOf(
                Opplysningsgrunnlag.ny(TrygdetidOpplysningType.FOEDSELSDATO, foedselsdato?.kilde, foedselsdato?.verdi),
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
            trygdetidRepository.transaction { tx ->
                trygdetidRepository.opprettTrygdetid(behandling, opplysninger, tx)
            }
        }

    private fun kildeFoedselsnummer(): RegelKilde = RegelKilde(
        "Beregnet basert på fødselsdato fra pdl",
        Tidspunkt.now(),
        "1"
    )

    suspend fun lagreTrygdetidGrunnlag(
        behandlingId: UUID,
        bruker: Bruker,
        trygdetidGrunnlag: TrygdetidGrunnlag
    ): Trygdetid =
        tilstandssjekk(behandlingId, bruker) {
            val beregnetTrygdetidGrunnlag = beregnTrygdetidService.beregnTrygdetidGrunnlag(trygdetidGrunnlag)
            val trygdetidGrunnlagMedBeregning = trygdetidGrunnlag.copy(beregnetTrygdetid = beregnetTrygdetidGrunnlag)
            val eksisterendeTrygdetid = trygdetidRepository.hentEnkeltTrygdetidGrunnlag(trygdetidGrunnlag.id)

            trygdetidRepository.transaction { tx ->
                val trygdetid = if (eksisterendeTrygdetid != null) {
                    trygdetidRepository.oppdaterTrygdetidGrunnlag(behandlingId, trygdetidGrunnlagMedBeregning, tx)
                } else {
                    trygdetidRepository.opprettTrygdetidGrunnlag(behandlingId, trygdetidGrunnlagMedBeregning, tx)
                }

                val beregnetTrygdetid = beregnTrygdetidService.beregnTrygdetid(trygdetid.trygdetidGrunnlag)
                trygdetidRepository.oppdaterBeregnetTrygdetid(behandlingId, beregnetTrygdetid, tx).also {
                    runBlocking { behandlingKlient.settBehandlingStatusVilkaarsvurdert(behandlingId, bruker) }
                }
            }
        }

    suspend fun kopierSisteTrygdetidberegning(
        behandlingId: UUID,
        forrigeBehandlingId: UUID,
        bruker: Bruker
    ): Trygdetid {
        val forrigeTrygdetid = hentTrygdetid(forrigeBehandlingId)
        val forrigeTrygdetidGrunnlag =
            forrigeTrygdetid?.trygdetidGrunnlag ?: throw Exception("Mangler trygdetid for $forrigeBehandlingId")
        val beregnetTrygdetid =
            forrigeTrygdetid.beregnetTrygdetid ?: throw Exception("Mangler beregnet trygdetid for $forrigeBehandlingId")
        val regulering = behandlingKlient.hentBehandling(behandlingId, bruker)

        return trygdetidRepository.transaction { tx ->
            trygdetidRepository.opprettTrygdetid(regulering, forrigeTrygdetid.opplysninger, tx)
            forrigeTrygdetidGrunnlag.forEach { grunnlag ->
                trygdetidRepository.opprettTrygdetidGrunnlag(behandlingId, grunnlag, tx)
            }
            trygdetidRepository.oppdaterBeregnetTrygdetid(behandlingId, beregnetTrygdetid, tx)
        }
    }

    private suspend fun tilstandssjekk(behandlingId: UUID, bruker: Bruker, block: suspend () -> Trygdetid): Trygdetid {
        val kanFastsetteTrygdetid = behandlingKlient.kanBeregnes(behandlingId, bruker)
        return if (kanFastsetteTrygdetid) {
            block()
        } else {
            throw Exception("Kan ikke opprette/endre trygdetid da behandlingen er i feil tilstand")
        }
    }
}
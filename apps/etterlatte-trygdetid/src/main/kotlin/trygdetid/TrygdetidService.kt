package no.nav.etterlatte.trygdetid

import no.nav.etterlatte.libs.common.grunnlag.hentDoedsdato
import no.nav.etterlatte.libs.common.grunnlag.hentFoedselsdato
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
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
            val opplysninger = mapOf(
                Opplysningstype.FOEDSELSDATO to avdoed.hentFoedselsdato(),
                Opplysningstype.DOEDSDATO to avdoed.hentDoedsdato()
            )
            trygdetidRepository.opprettTrygdetid(behandling, opplysninger)
        }

    suspend fun lagreTrygdetidGrunnlag(
        behandlingId: UUID,
        bruker: Bruker,
        trygdetidGrunnlag: TrygdetidGrunnlag
    ): Trygdetid =
        tilstandssjekk(behandlingId, bruker) {
            // TODO hvis status er "forbi" trygdetid bør dette sette tilstand tilbake til trygdetid?
            val trygdetidGrunnlagMedBeregning = beregnTrygdetidService.beregnTrygdetidGrunnlag(trygdetidGrunnlag)
            val eksisterendeTrygdetid = trygdetidRepository.hentEnkeltTrygdetidGrunnlag(trygdetidGrunnlag.id)
            val trygdetid = if (eksisterendeTrygdetid != null) {
                trygdetidRepository.oppdaterTrygdetidGrunnlag(behandlingId, trygdetidGrunnlagMedBeregning)
            } else {
                trygdetidRepository.opprettTrygdetidGrunnlag(behandlingId, trygdetidGrunnlagMedBeregning)
            }

            val trygdetidMedBeregning = trygdetidRepository.oppdaterBeregnetTrygdetid(
                behandlingId = behandlingId,
                beregnetTrygdetid = beregnTrygdetidService.beregnTrygdetid(trygdetid.trygdetidGrunnlag)
            )

            behandlingKlient.settBehandlingStatusVilkaarsvurdert(behandlingId, bruker)
            trygdetidMedBeregning
        }

    suspend fun lagreBeregnetTrygdetid(
        behandlingId: UUID,
        bruker: Bruker,
        beregnetTrygdetid: BeregnetTrygdetid
    ): Trygdetid =
        tilstandssjekk(behandlingId, bruker) {
            // TODO hvis status er "forbi" trygdetid bør dette sette tilstand tilbake til trygdetid?
            val trygdetid = trygdetidRepository.oppdaterBeregnetTrygdetid(behandlingId, beregnetTrygdetid)
            behandlingKlient.settBehandlingStatusVilkaarsvurdert(behandlingId, bruker)
            trygdetid
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
package no.nav.etterlatte.trygdetid

import no.nav.etterlatte.libs.common.grunnlag.hentDoedsdato
import no.nav.etterlatte.libs.common.grunnlag.hentFoedselsdato
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.token.Bruker
import no.nav.etterlatte.trygdetid.klienter.BehandlingKlient
import no.nav.etterlatte.trygdetid.klienter.GrunnlagKlient
import java.util.*

class TrygdetidService(
    private val trygdetidRepository: TrygdetidRepository,
    private val behandlingKlient: BehandlingKlient,
    private val grunnlagKlient: GrunnlagKlient
) {
    fun hentTrygdetid(behandlingsId: UUID): Trygdetid? = trygdetidRepository.hentTrygdetid(behandlingsId)

    suspend fun opprettTrygdetid(behandlingId: UUID, sakId: Long, bruker: Bruker): Trygdetid =
        tilstandssjekk(behandlingId, bruker) {
            trygdetidRepository.hentTrygdetid(behandlingId)?.let {
                throw IllegalArgumentException("Trygdetid finnes allerede for behandling $behandlingId")
            }

            val avdoed = grunnlagKlient.hentGrunnlag(sakId, bruker).hentAvdoed()
            val opplysninger = mapOf(
                Opplysningstype.FOEDSELSDATO to avdoed.hentFoedselsdato()?.verdi?.toJson(),
                Opplysningstype.DOEDSDATO to avdoed.hentDoedsdato()?.verdi?.toJson()
            )
            trygdetidRepository.opprettTrygdetid(behandlingId, opplysninger)
        }

    suspend fun lagreTrygdetidGrunnlag(
        behandlingId: UUID,
        bruker: Bruker,
        trygdetidGrunnlag: TrygdetidGrunnlag
    ): Trygdetid =
        tilstandssjekk(behandlingId, bruker) {
            // TODO hvis status er "forbi" trygdetid bør dette sette tilstand tilbake til trygdetid?
            val eksisterendeTrygdetid = trygdetidRepository.hentEnkeltTrygdetidGrunnlag(trygdetidGrunnlag.id)
            if (eksisterendeTrygdetid != null) {
                trygdetidRepository.oppdaterTrygdetidGrunnlag(behandlingId, trygdetidGrunnlag)
            } else {
                trygdetidRepository.opprettTrygdetidGrunnlag(behandlingId, trygdetidGrunnlag)
            }
        }

    suspend fun lagreBeregnetTrygdetid(
        behandlingId: UUID,
        bruker: Bruker,
        beregnetTrygdetid: BeregnetTrygdetid
    ): Trygdetid =
        tilstandssjekk(behandlingId, bruker) {
            // TODO hvis status er "forbi" trygdetid bør dette sette tilstand tilbake til trygdetid?
            trygdetidRepository.oppdaterBeregnetTrygdetid(behandlingId, beregnetTrygdetid)
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
package no.nav.etterlatte.behandling

import java.util.*


data class BehandlingSammendragListe(val behandlinger: List<BehandlingSammendrag>)
data class BehandlingSammendrag(val id: UUID, val sak: Long, val status: String)
data class DetaljertBehandling(    val id: UUID,
                                   val sak: Long,
                                   val grunnlag: List<Opplysning>,
                                   val vilkårsprøving: Vilkårsprøving?,
                                   val beregning: Beregning?,
                                   val fastsatt: Boolean = false)

data class BehandlingsBehov(val sak: Long)


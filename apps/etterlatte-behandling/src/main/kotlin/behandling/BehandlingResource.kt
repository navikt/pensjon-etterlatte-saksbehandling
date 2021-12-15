package no.nav.etterlatte.behandling

import java.util.*


data class BehandlingSammendragListe(val behandlinger: List<BehandlingSammendrag>)
data class BehandlingSammendrag(val id: UUID, val sak: String, val status: String)
data class DetaljertBehandling(    val id: UUID,
                                   val sak: String,
                                   val grunnlag: List<Opplysning>,
                                   val vilkårsprøving: Vilkårsprøving?,
                                   val beregning: Beregning?,
                                   val fastsatt: Boolean = false)

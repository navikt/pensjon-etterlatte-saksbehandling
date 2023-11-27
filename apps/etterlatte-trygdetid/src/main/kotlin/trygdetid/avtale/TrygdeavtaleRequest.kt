package no.nav.etterlatte.trygdetid.avtale

import no.nav.etterlatte.libs.common.behandling.JaNei
import java.util.UUID

data class TrygdeavtaleRequest(
    val id: UUID?,
    val avtaleKode: String,
    val avtaleDatoKode: String?,
    val avtaleKriteriaKode: String?,
    val personKrets: JaNei?,
    val arbInntekt1G: JaNei?,
    val arbInntekt1GKommentar: String?,
    val beregArt50: JaNei?,
    val beregArt50Kommentar: String?,
    val nordiskTrygdeAvtale: JaNei?,
    val nordiskTrygdeAvtaleKommentar: String?,
)

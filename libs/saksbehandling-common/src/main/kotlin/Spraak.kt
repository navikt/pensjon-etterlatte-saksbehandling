package no.nav.etterlatte.brev.model

import com.fasterxml.jackson.annotation.JsonValue

enum class Spraak(
    @get:JsonValue val verdi: String,
) {
    NB("nb"),
    NN("nn"),
    EN("en"),
}

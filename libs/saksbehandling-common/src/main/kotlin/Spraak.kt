package no.nav.etterlatte.brev.model

import com.fasterxml.jackson.annotation.JsonValue

enum class Spraak(
    @get:JsonValue val verdi: String,
) {
    NB("NB"),
    NN("NN"),
    EN("EN"),
}

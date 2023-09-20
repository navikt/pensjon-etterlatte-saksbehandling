package no.nav.etterlatte.behandling.domain

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.etterlatte.institusjonsopphold.InstitusjonsoppholdHendelseBeriket
import no.nav.etterlatte.institusjonsopphold.InstitusjonsoppholdsType
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.pdl.PersonDTO
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.Utland
import no.nav.etterlatte.libs.common.person.VergemaalEllerFremtidsfullmakt
import java.time.LocalDate

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
sealed class SamsvarMellomKildeOgGrunnlag {
    abstract val samsvar: Boolean

    @JsonTypeName("Feilrolle")
    data class FeilRolle(
        val person: PersonDTO,
        val fraGrunnlag: Grunnlag?,
        override val samsvar: Boolean,
    ) : SamsvarMellomKildeOgGrunnlag()

    @JsonTypeName("VERGEMAAL_ELLER_FREMTIDSFULLMAKT")
    data class VergemaalEllerFremtidsfullmaktForhold(
        val fraPdl: List<VergemaalEllerFremtidsfullmakt>?,
        val fraGrunnlag: List<VergemaalEllerFremtidsfullmakt>?,
        override val samsvar: Boolean,
    ) : SamsvarMellomKildeOgGrunnlag()

    @JsonTypeName("DOEDSDATO")
    data class Doedsdatoforhold(
        val fraGrunnlag: LocalDate?,
        val fraPdl: LocalDate?,
        override val samsvar: Boolean,
    ) : SamsvarMellomKildeOgGrunnlag()

    @JsonTypeName("UTLAND")
    data class Utlandsforhold(
        val fraPdl: Utland?,
        val fraGrunnlag: Utland?,
        override val samsvar: Boolean,
    ) : SamsvarMellomKildeOgGrunnlag()

    @JsonTypeName("ANSVARLIGE_FORELDRE")
    data class AnsvarligeForeldre(
        val fraPdl: List<Folkeregisteridentifikator>?,
        val fraGrunnlag: List<Folkeregisteridentifikator>?,
        override val samsvar: Boolean,
    ) : SamsvarMellomKildeOgGrunnlag()

    @JsonTypeName("BARN")
    data class Barn(
        val fraPdl: List<Folkeregisteridentifikator>?,
        val fraGrunnlag: List<Folkeregisteridentifikator>?,
        override val samsvar: Boolean,
    ) : SamsvarMellomKildeOgGrunnlag()

    @JsonTypeName("SIVILSTAND")
    data class Sivilstand(
        val fraPdl: List<no.nav.etterlatte.libs.common.person.Sivilstand>?,
        val fraGrunnlag: List<no.nav.etterlatte.libs.common.person.Sivilstand>?,
        override val samsvar: Boolean,
    ) : SamsvarMellomKildeOgGrunnlag()

    @JsonTypeName("GRUNNBELOEP")
    data class Grunnbeloep(
        override val samsvar: Boolean,
    ) : SamsvarMellomKildeOgGrunnlag()

    @JsonTypeName("INSTITUSJONSOPPHOLD")
    data class INSTITUSJONSOPPHOLD(
        override val samsvar: Boolean,
        val oppholdstype: InstitusjonsoppholdsType,
        val oppholdBeriket: InstitusjonsoppholdHendelseBeriket?,
    ) : SamsvarMellomKildeOgGrunnlag()
}

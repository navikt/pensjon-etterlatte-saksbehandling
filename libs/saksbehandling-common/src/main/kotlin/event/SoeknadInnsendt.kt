package no.nav.etterlatte.libs.common.event

interface ISoeknadInnsendt {
    val skjemaInfoKey get() = "@skjema_info"
    val skjemaInfoTypeKey get() = "@skjema_info.type"
    val templateKey get() = "@template"
    val lagretSoeknadIdKey get() = "@lagret_soeknad_id"
    val hendelseGyldigTilKey get() = "@hendelse_gyldig_til"
    val adressebeskyttelseKey get() = "@adressebeskyttelse"
    val fnrSoekerKey get() = "@fnr_soeker"
    val dokarkivReturKey get() = "@dokarkivRetur"
}

object SoeknadInnsendt : ISoeknadInnsendt

enum class SoeknadInnsendtHendelseType(
    val eventname: String,
) : EventnameHendelseType {
    EVENT_NAME_INNSENDT("soeknad_innsendt"),
    EVENT_NAME_BEHANDLINGBEHOV("trenger_behandling"),
    ;

    override fun lagEventnameForType(): String = this.eventname
}

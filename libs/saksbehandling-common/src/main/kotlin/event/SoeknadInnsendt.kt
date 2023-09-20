package no.nav.etterlatte.libs.common.event

interface ISoeknadInnsendt {
    val skjemaInfoKey get() = "@skjema_info"
    val skjemaInfoTypeKey get() = "@skjema_info.type"
    val skjemaInfoVersjonKey get() = "@skjema_info.versjon"
    val templateKey get() = "@template"
    val lagretSoeknadIdKey get() = "@lagret_soeknad_id"
    val hendelseGyldigTilKey get() = "@hendelse_gyldig_til"
    val adressebeskyttelseKey get() = "@adressebeskyttelse"
    val fnrSoekerKey get() = "@fnr_soeker"
    val dokarkivReturKey get() = "@dokarkivRetur"
    val eventNameInnsendt get() = "soeknad_innsendt"
    val eventNameBehandlingBehov get() = "trenger_behandling"
}

object SoeknadInnsendt : ISoeknadInnsendt

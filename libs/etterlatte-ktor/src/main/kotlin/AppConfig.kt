package no.nav.etterlatte.libs.ktor

import no.nav.etterlatte.libs.common.EnvEnum

enum class AppConfig : EnvEnum {
    HTTP_PORT,
    ELECTOR_PATH,
    ;

    override fun name() = name
}

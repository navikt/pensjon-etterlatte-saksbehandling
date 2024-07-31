package no.nav.etterlatte.libs.ktor

import no.nav.etterlatte.libs.common.EnvEnum

enum class AppConfig : EnvEnum {
    DEV_MODE,
    ELECTOR_PATH,
    ;

    override fun key() = name
}

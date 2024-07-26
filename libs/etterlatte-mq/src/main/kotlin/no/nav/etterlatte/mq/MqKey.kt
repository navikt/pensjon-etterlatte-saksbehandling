package no.nav.etterlatte.mq

import no.nav.etterlatte.libs.common.EnvEnum

enum class MqKey : EnvEnum {
    MQ_HOSTNAME,
    MQ_PORT,
    MQ_MANAGER,
    MQ_CHANNEL,

    @Suppress("ktlint:standard:enum-entry-name-case")
    srvuser,

    @Suppress("ktlint:standard:enum-entry-name-case")
    srvpwd,
    ;

    override fun name() = name
}

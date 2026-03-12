package no.nav.etterlatte.config.modules

import no.nav.etterlatte.behandling.BehandlingsHendelserKafkaProducerImpl
import no.nav.etterlatte.behandling.klage.KlageHendelserServiceImpl
import no.nav.etterlatte.behandling.tilbakekreving.TilbakekrevingHendelserServiceImpl
import no.nav.etterlatte.grunnlagsendring.doedshendelse.DoedshendelserKafkaServiceImpl
import no.nav.etterlatte.kafka.KafkaProdusent

class KafkaModule(
    private val rapid: KafkaProdusent<String, String>,
) {
    val behandlingsHendelser by lazy { BehandlingsHendelserKafkaProducerImpl(rapid) }
    val klageHendelser by lazy { KlageHendelserServiceImpl(rapid) }
    val tilbakekrevingHendelserService by lazy { TilbakekrevingHendelserServiceImpl(rapid) }
    val doedshendelserProducer by lazy { DoedshendelserKafkaServiceImpl(rapid) }
}

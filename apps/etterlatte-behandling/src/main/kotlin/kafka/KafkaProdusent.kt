package no.nav.etterlatte.kafka

import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord

interface KafkaProdusent<K, V>{
    fun publiser(noekkel: K, verdi: V, headers: Map<String, ByteArray> = emptyMap()): Pair<Int, Long>
}

class KafkaProdusentImpl<K, V> (private val kafka: KafkaProducer<K, V>, private val topic: String): KafkaProdusent<K, V> {

    init{
        Runtime.getRuntime().addShutdownHook(Thread{kafka.close()})
    }

    override fun publiser(noekkel: K, verdi: V, headers: Map<String, ByteArray>): Pair<Int, Long>{
        return kafka.send(ProducerRecord(topic, noekkel, verdi).also {
            headers.forEach{ h->
                it.headers().add(h.key, h.value)
            }
        }).get().let {
            it.partition() to it.offset()
        }
    }



}
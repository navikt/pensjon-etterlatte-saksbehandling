package no.nav.etterlatte.kafka

import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer

interface KafkaProdusent<K, V> {
    fun publiser(noekkel: K, verdi: V, headers: Map<String, ByteArray>? = emptyMap()): Pair<Int, Long>
    fun close() = Unit
}

fun KafkaConfig.standardProducer(topic: String): KafkaProdusent<String, String> = KafkaProdusentImpl(
    KafkaProducer(producerConfig(), StringSerializer(), StringSerializer()),
    topic
)
fun KafkaConfig.rapidsAndRiversProducer(topic: String): KafkaProdusent<String, JsonMessage> = KafkaProdusentImpl(
    KafkaProducer(producerConfig(), StringSerializer(), JsonMessageSerializer()),
    topic
)

class KafkaProdusentImpl<K, V> (
    private val kafka: KafkaProducer<K, V>,
    private val topic: String
) : KafkaProdusent<K, V> {
    override fun publiser(noekkel: K, verdi: V, headers: Map<String, ByteArray>?): Pair<Int, Long> {
        return kafka.send(
            ProducerRecord(topic, noekkel, verdi).also {
                headers?.forEach { h ->
                    it.headers().add(h.key, h.value)
                }
            }
        ).get().let {
            it.partition() to it.offset()
        }
    }

    override fun close() {
        kafka.close()
    }
}

class TestProdusent<K, V> () : KafkaProdusent<K, V> {
    data class Record<K, V>(val noekkel: K, val verdi: V, val headers: Map<String, ByteArray>?)
    var closed = false; private set
    val publiserteMeldinger = mutableListOf<Record<K, V>>()
    override fun publiser(noekkel: K, verdi: V, headers: Map<String, ByteArray>?): Pair<Int, Long> {
        require(!closed)
        return publiserteMeldinger.let {
            it.add(Record(noekkel, verdi, headers))
            Pair(0, (it.size - 1).toLong())
        }
    }

    override fun close() {
        closed = true
    }
}
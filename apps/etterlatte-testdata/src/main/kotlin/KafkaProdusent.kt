import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord

class KafkaProdusent<K, V> (private val kafka: KafkaProducer<K, V>, private val topic: String) {

    init{
        Runtime.getRuntime().addShutdownHook(Thread{kafka.close()})
    }

    fun publiser(noekkel: K, verdi: V): Pair<Int, Long>{
        return kafka.send(ProducerRecord(topic, noekkel, verdi)).get().let {
            it.partition() to it.offset()
        }
    }



}
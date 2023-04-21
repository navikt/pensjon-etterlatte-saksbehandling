package institusjonsopphold

data class KafkaOppholdHendelse(
    val hendelseId: Long,
    var oppholdId: Long,
    var norskident: String,
    var type: Type,
    var kilde: Kilde
) {
    enum class Type {
        INNMELDING,
        OPPDATERING,
        UTMELDING,
        ANNULERING
    }

    enum class Kilde {
        APPBRK, KDI, IT, INST
    }
}
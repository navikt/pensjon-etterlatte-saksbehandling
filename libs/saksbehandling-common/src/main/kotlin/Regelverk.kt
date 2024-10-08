import java.time.LocalDate
import java.time.Month

enum class Regelverk {
    REGELVERK_TOM_DES_2023,
    REGELVERK_FOM_JAN_2024,
    ;

    companion object {
        private val ETTERLATTEREFORMEN_DATO: LocalDate = LocalDate.of(2024, Month.JANUARY, 1)

        fun fraDato(dato: LocalDate): Regelverk =
            if (dato >= ETTERLATTEREFORMEN_DATO) {
                REGELVERK_FOM_JAN_2024
            } else {
                REGELVERK_TOM_DES_2023
            }
    }
}

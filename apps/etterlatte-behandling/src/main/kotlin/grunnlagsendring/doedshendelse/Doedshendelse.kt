package no.nav.etterlatte.grunnlagsendring.doedshendelse

import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import java.time.LocalDate
import java.util.UUID

data class Doedshendelse internal constructor(
    val id: UUID = UUID.randomUUID(),
    val avdoedFnr: String,
    val avdoedDoedsdato: LocalDate,
    val beroertFnr: String,
    val relasjon: Relasjon,
    val opprettet: Tidspunkt,
    val endret: Tidspunkt,
    val status: Status,
    val utfall: Utfall? = null,
    val oppgaveId: UUID? = null,
    val brevId: Long? = null,
    val sakId: Long? = null,
) {
    companion object {
        fun nyHendelse(
            avdoedFnr: String,
            avdoedDoedsdato: LocalDate,
            beroertFnr: String,
            relasjon: Relasjon,
        ) = Doedshendelse(
            avdoedFnr = avdoedFnr,
            avdoedDoedsdato = avdoedDoedsdato,
            beroertFnr = beroertFnr,
            relasjon = relasjon,
            status = Status.NY,
            opprettet = Tidspunkt.now(),
            endret = Tidspunkt.now(),
        )
    }

    fun tilAvbrutt(sakId: Long? = null): Doedshendelse {
        return copy(
            sakId = sakId,
            status = Status.FERDIG,
            utfall = Utfall.AVBRUTT,
            endret = Tidspunkt.now(),
        )
    }
}

enum class Status {
    NY,
    OPPDATERT,
    FERDIG,
    FEILET,
}

enum class Utfall {
    BREV,
    OPPGAVE,
    AVBRUTT,
}

// Vi bør muligens skille på EPS, og støtte søsken også.
// Holder det enkelt til vi vet mer om behovet.
enum class Relasjon {
    BARN,
    EPS,
}

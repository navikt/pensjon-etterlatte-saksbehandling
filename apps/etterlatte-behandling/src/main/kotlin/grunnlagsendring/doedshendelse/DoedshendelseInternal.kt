package no.nav.etterlatte.grunnlagsendring.doedshendelse

import no.nav.etterlatte.grunnlagsendring.doedshendelse.kontrollpunkt.DoedshendelseKontrollpunkt
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.pdlhendelse.Endringstype
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import java.time.LocalDate
import java.util.UUID

data class DoedshendelseInternal internal constructor(
    val id: UUID = UUID.randomUUID(),
    val avdoedFnr: String,
    val avdoedDoedsdato: LocalDate,
    val beroertFnr: String,
    val relasjon: Relasjon,
    val opprettet: Tidspunkt,
    val endret: Tidspunkt,
    val status: Status,
    val endringstype: Endringstype? = null,
    val utfall: Utfall? = null,
    val oppgaveId: UUID? = null,
    val brevId: Long? = null,
    val sakId: no.nav.etterlatte.libs.common.sak.SakId? = null,
    val kontrollpunkter: List<DoedshendelseKontrollpunkt>? = null,
    val migrertMellomAttenOgTjue: Boolean = false,
) {
    companion object {
        fun nyHendelse(
            avdoedFnr: String,
            avdoedDoedsdato: LocalDate,
            beroertFnr: String,
            relasjon: Relasjon,
            endringstype: Endringstype,
            migrertMellomAttenOgTjue: Boolean = false,
        ) = DoedshendelseInternal(
            avdoedFnr = avdoedFnr,
            avdoedDoedsdato = avdoedDoedsdato,
            beroertFnr = beroertFnr,
            relasjon = relasjon,
            status = Status.NY,
            endringstype = endringstype,
            opprettet = Tidspunkt.now(),
            endret = Tidspunkt.now(),
            migrertMellomAttenOgTjue = migrertMellomAttenOgTjue,
        )
    }

    fun tilOppdatert(
        avdoedDoedsdato: LocalDate,
        endringstype: Endringstype,
    ): DoedshendelseInternal =
        copy(
            avdoedDoedsdato = avdoedDoedsdato,
            status = Status.OPPDATERT,
            endret = endret,
            endringstype = endringstype,
        )

    fun tilAvbrutt(
        sakId: no.nav.etterlatte.libs.common.sak.SakId? = null,
        oppgaveId: UUID? = null,
        kontrollpunkter: List<DoedshendelseKontrollpunkt>,
    ): DoedshendelseInternal =
        copy(
            sakId = sakId,
            oppgaveId = oppgaveId,
            kontrollpunkter = kontrollpunkter,
            status = Status.FERDIG,
            utfall = Utfall.AVBRUTT,
            endret = Tidspunkt.now(),
        )

    fun tilAnnulert(): DoedshendelseInternal =
        copy(
            status = Status.FERDIG,
            utfall = Utfall.AVBRUTT,
            endret = Tidspunkt.now(),
            endringstype = Endringstype.ANNULLERT,
            kontrollpunkter = listOf(DoedshendelseKontrollpunkt.DoedshendelseErAnnullert),
        )

    fun tilBehandlet(
        utfall: Utfall,
        sakId: no.nav.etterlatte.libs.common.sak.SakId?,
        kontrollpunkter: List<DoedshendelseKontrollpunkt>,
        oppgaveId: UUID? = null,
        brevId: Long? = null,
    ): DoedshendelseInternal =
        copy(
            status = Status.FERDIG,
            utfall = utfall,
            sakId = sakId,
            kontrollpunkter = kontrollpunkter,
            oppgaveId = oppgaveId,
            brevId = brevId,
            endret = Tidspunkt.now(),
        )

    fun sakTypeForEpsEllerBarn(): SakType =
        when (relasjon) {
            Relasjon.BARN -> SakType.BARNEPENSJON
            Relasjon.EKTEFELLE -> SakType.OMSTILLINGSSTOENAD
            Relasjon.AVDOED -> throw IllegalStateException("Saktype for relasjon er kun gyldig for BARN og EPS")
            Relasjon.SAMBOER -> SakType.OMSTILLINGSSTOENAD
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
    BREV_OG_OPPGAVE,
    AVBRUTT,
}

// Vi bør muligens skille på EPS, og støtte søsken også.
// Holder det enkelt til vi vet mer om behovet.
enum class Relasjon {
    BARN,
    EKTEFELLE,
    SAMBOER,
    AVDOED,
}

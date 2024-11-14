package no.nav.etterlatte.behandling.aktivitetsplikt

import no.nav.etterlatte.behandling.aktivitetsplikt.vurdering.AktivitetspliktAktivitetsgrad
import no.nav.etterlatte.behandling.aktivitetsplikt.vurdering.AktivitetspliktAktivitetsgradType
import no.nav.etterlatte.behandling.aktivitetsplikt.vurdering.AktivitetspliktUnntak
import no.nav.etterlatte.behandling.aktivitetsplikt.vurdering.AktivitetspliktUnntakType
import no.nav.etterlatte.behandling.sakId1
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import java.time.LocalDate
import java.util.UUID

fun unntak(
    id: UUID = UUID.randomUUID(),
    sakId: SakId = sakId1,
    behandlingId: UUID? = null,
    oppgaveId: UUID? = null,
    unntak: AktivitetspliktUnntakType = AktivitetspliktUnntakType.OMSORG_BARN_UNDER_ETT_AAR,
    fom: LocalDate? = LocalDate.now(),
    tom: LocalDate? = null,
    opprettet: Grunnlagsopplysning.Saksbehandler =
        Grunnlagsopplysning.Saksbehandler(
            ident = "Z123456",
            tidspunkt = Tidspunkt.now(),
        ),
    endret: Grunnlagsopplysning.Saksbehandler? =
        Grunnlagsopplysning.Saksbehandler(
            ident = "Z123456",
            tidspunkt = Tidspunkt.now(),
        ),
    beskrivelse: String = "",
): AktivitetspliktUnntak =
    AktivitetspliktUnntak(
        id = id,
        sakId = sakId,
        behandlingId = behandlingId,
        oppgaveId = oppgaveId,
        unntak = unntak,
        fom = fom,
        tom = tom,
        opprettet = opprettet,
        endret = endret,
        beskrivelse = beskrivelse,
    )

fun aktivitetsgrad(
    id: UUID = UUID.randomUUID(),
    sakId: SakId = sakId1,
    behandlingId: UUID? = null,
    oppgaveId: UUID? = null,
    aktivitetsgrad: AktivitetspliktAktivitetsgradType = AktivitetspliktAktivitetsgradType.AKTIVITET_100,
    fom: LocalDate = LocalDate.now(),
    tom: LocalDate = LocalDate.now(),
    opprettet: Grunnlagsopplysning.Saksbehandler =
        Grunnlagsopplysning.Saksbehandler(
            ident = "Z123456",
            tidspunkt = Tidspunkt.now(),
        ),
    endret: Grunnlagsopplysning.Saksbehandler =
        Grunnlagsopplysning.Saksbehandler(
            ident = "Z123456",
            tidspunkt = Tidspunkt.now(),
        ),
    beskrivelse: String = "",
): AktivitetspliktAktivitetsgrad =
    AktivitetspliktAktivitetsgrad(
        id = id,
        sakId = sakId,
        behandlingId = behandlingId,
        oppgaveId = oppgaveId,
        aktivitetsgrad = aktivitetsgrad,
        fom = fom,
        tom = tom,
        opprettet = opprettet,
        endret = endret,
        beskrivelse = beskrivelse,
    )

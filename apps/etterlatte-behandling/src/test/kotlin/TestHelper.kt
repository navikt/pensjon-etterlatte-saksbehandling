package no.nav.etterlatte

import no.nav.etterlatte.behandling.Foerstegangsbehandling
import no.nav.etterlatte.behandling.ManueltOpphoer
import no.nav.etterlatte.behandling.Revurdering
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.GrunnlagsendringStatus
import no.nav.etterlatte.libs.common.behandling.GrunnlagsendringsType
import no.nav.etterlatte.libs.common.behandling.Grunnlagsendringshendelse
import no.nav.etterlatte.libs.common.behandling.Grunnlagsinformasjon
import no.nav.etterlatte.libs.common.behandling.KommerBarnetTilgode
import no.nav.etterlatte.libs.common.behandling.ManueltOpphoerAarsak
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import no.nav.etterlatte.libs.common.behandling.Saksrolle
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsResultat
import no.nav.etterlatte.libs.common.pdlhendelse.Doedshendelse
import no.nav.etterlatte.libs.common.pdlhendelse.Endringstype
import no.nav.etterlatte.libs.common.pdlhendelse.ForelderBarnRelasjonHendelse
import no.nav.etterlatte.libs.common.pdlhendelse.UtflyttingsHendelse
import no.nav.etterlatte.libs.common.person.Adressebeskyttelse
import no.nav.etterlatte.libs.common.person.FamilieRelasjon
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.person.Utland
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.JaNeiVetIkke
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

fun foerstegangsbehandling(
    id: UUID = UUID.randomUUID(),
    sak: Long,
    behandlingOpprettet: LocalDateTime = LocalDateTime.now(),
    sistEndret: LocalDateTime = LocalDateTime.now(),
    status: BehandlingStatus = BehandlingStatus.OPPRETTET,
    soeknadMottattDato: LocalDateTime = LocalDateTime.now(),
    persongalleri: Persongalleri = persongalleri(),
    gyldighetsproeving: GyldighetsResultat? = null,
    virkningstidspunkt: Virkningstidspunkt? = null,
    kommerBarnetTilgode: KommerBarnetTilgode? = null
) = Foerstegangsbehandling(
    id = id,
    sak = sak,
    behandlingOpprettet = behandlingOpprettet,
    sistEndret = sistEndret,
    status = status,
    soeknadMottattDato = soeknadMottattDato,
    persongalleri = persongalleri,
    gyldighetsproeving = gyldighetsproeving,
    virkningstidspunkt = virkningstidspunkt,
    kommerBarnetTilgode = kommerBarnetTilgode
)

fun revurdering(
    id: UUID = UUID.randomUUID(),
    sak: Long,
    behandlingOpprettet: LocalDateTime = LocalDateTime.now(),
    sistEndret: LocalDateTime = LocalDateTime.now(),
    status: BehandlingStatus = BehandlingStatus.OPPRETTET,
    persongalleri: Persongalleri = persongalleri(),
    revurderingAarsak: RevurderingAarsak,
    kommerBarnetTilgode: KommerBarnetTilgode = kommerBarnetTilgode()
) = Revurdering(
    id = id,
    sak = sak,
    behandlingOpprettet = behandlingOpprettet,
    sistEndret = sistEndret,
    status = status,
    persongalleri = persongalleri,
    revurderingsaarsak = revurderingAarsak,
    kommerBarnetTilgode = kommerBarnetTilgode
)

fun manueltOpphoer(
    sak: Long = 1,
    behandlingId: UUID = UUID.randomUUID(),
    persongalleri: Persongalleri = persongalleri(),
    opphoerAarsaker: List<ManueltOpphoerAarsak> = listOf(
        ManueltOpphoerAarsak.SOESKEN_DOED,
        ManueltOpphoerAarsak.GJENLEVENDE_FORELDER_DOED
    ),
    fritekstAarsak: String? = "Umulig å revurdere i nytt saksbehandlingssystem"
) = ManueltOpphoer(
    id = behandlingId,
    sak = sak,
    behandlingOpprettet = LocalDateTime.now(),
    sistEndret = LocalDateTime.now(),
    status = BehandlingStatus.OPPRETTET,
    persongalleri = persongalleri,
    opphoerAarsaker = opphoerAarsaker,
    fritekstAarsak = fritekstAarsak
)

fun persongalleri(
    soeker: String = "Soeker",
    innsender: String = "Innsender",
    soesken: List<String> = listOf("Soester", "Bror"),
    avdoed: List<String> = listOf("Avdoed"),
    gjenlevende: List<String> = listOf("Gjenlevende")
) = Persongalleri(
    soeker = soeker,
    innsender = innsender,
    soesken = soesken,
    avdoed = avdoed,
    gjenlevende = gjenlevende
)

fun grunnlagsendringshendelse(
    id: UUID = UUID.randomUUID(),
    sakId: Long = 1,
    type: GrunnlagsendringsType = GrunnlagsendringsType.DOEDSFALL,
    opprettet: LocalDateTime = LocalDateTime.now(),
    data: Grunnlagsinformasjon,
    status: GrunnlagsendringStatus = GrunnlagsendringStatus.VENTER_PAA_JOBB,
    behandlingId: UUID? = null,
    hendelseGjelderRolle: Saksrolle = Saksrolle.SOEKER
) = Grunnlagsendringshendelse(
    id = id,
    sakId = sakId,
    type = type,
    opprettet = opprettet,
    data = data,
    status = status,
    behandlingId = behandlingId,
    hendelseGjelderRolle = hendelseGjelderRolle
)

fun grunnlagsinformasjonDoedshendelse(
    avdoedFnr: String = "12345678911",
    doedsdato: LocalDate = LocalDate.of(2022, 1, 1),
    endringstype: Endringstype = Endringstype.OPPRETTET
) =
    Grunnlagsinformasjon.Doedsfall(
        Doedshendelse(avdoedFnr = avdoedFnr, doedsdato = doedsdato, endringstype = endringstype)
    )

fun grunnlagsinformasjonUtflyttingshendelse(
    fnr: String = "12345678911",
    tilflyttingsLand: String = "Sverige",
    utflyttingsdato: LocalDate = LocalDate.of(2022, 8, 8)
) = Grunnlagsinformasjon.Utflytting(
    hendelse = UtflyttingsHendelse(
        fnr = fnr,
        tilflyttingsLand = tilflyttingsLand,
        tilflyttingsstedIUtlandet = null,
        utflyttingsdato = utflyttingsdato,
        endringstype = Endringstype.OPPRETTET
    )
)

fun grunnlagsinformasjonForelderBarnRelasjonHendelse(
    fnr: String = "12345678911",
    relatertPersonsIdent: String = "98765432198",
    relatertPersonsRolle: String = "MOR",
    minRolleForPerson: String = "BARN"
) = Grunnlagsinformasjon.ForelderBarnRelasjon(
    hendelse = ForelderBarnRelasjonHendelse(
        fnr = fnr,
        relatertPersonsIdent = relatertPersonsIdent,
        relatertPersonsRolle = relatertPersonsRolle,
        minRolleForPerson = minRolleForPerson,
        relatertPersonUtenFolkeregisteridentifikator = null,
        endringstype = Endringstype.OPPRETTET
    )

)

fun kommerBarnetTilgode(
    svar: JaNeiVetIkke = JaNeiVetIkke.JA,
    begrunnelse: String = "En begrunnelse",
    kilde: Grunnlagsopplysning.Saksbehandler = Grunnlagsopplysning.Saksbehandler("S01", Instant.now())
) = KommerBarnetTilgode(svar, begrunnelse, kilde)

fun mockPerson(
    doedsdato: LocalDate? = null,
    utland: Utland? = null
) = Person(
    fornavn = "Test",
    etternavn = "Testulfsen",
    foedselsnummer = Foedselsnummer.of("70078749472"),
    foedselsdato = LocalDate.parse("2020-06-10"),
    foedselsaar = 1985,
    foedeland = null,
    doedsdato = doedsdato,
    adressebeskyttelse = Adressebeskyttelse.UGRADERT,
    bostedsadresse = null,
    deltBostedsadresse = null,
    kontaktadresse = null,
    oppholdsadresse = null,
    sivilstatus = null,
    statsborgerskap = null,
    utland = utland,
    familieRelasjon = FamilieRelasjon(null, null, null),
    avdoedesBarn = null,
    vergemaalEllerFremtidsfullmakt = null
)
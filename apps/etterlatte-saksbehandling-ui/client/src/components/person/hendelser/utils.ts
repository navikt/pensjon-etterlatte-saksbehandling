import {
  GrunnlagendringshendelseSamsvarType,
  Grunnlagsendringshendelse,
  GrunnlagsendringsType,
  IBehandlingsammendrag,
  Saksrolle,
} from '~components/person/typer'
import { formaterKanskjeStringDatoMedFallback } from '~utils/formattering'
import React from 'react'
import { PersonMedNavn } from '~shared/types/grunnlag'
import { Revurderingaarsak } from '~shared/types/Revurderingaarsak'
import { SakType } from '~shared/types/sak'
import { IBehandlingStatus, IBehandlingsType } from '~shared/types/IDetaljertBehandling'
import { behandlingErIverksatt, enhetErSkrivbar, erFerdigBehandlet } from '~components/behandling/felles/utils'

export const grunnlagsendringsTittel: Record<GrunnlagendringshendelseSamsvarType, string> = {
  GRUNNBELOEP: 'Regulering feilet',
  DOEDSDATO: 'Dødsdato',
  UTLAND: 'Ut-/innflytting til Norge',
  BARN: 'Barn',
  ANSVARLIGE_FORELDRE: 'Ansvarlige foreldre',
  VERGEMAAL_ELLER_FREMTIDSFULLMAKT: 'Vergemål eller fremtidsfullmakt',
  SIVILSTAND: 'Sivilstand',
  INSTITUSJONSOPPHOLD: 'Institusjonsopphold',
  ADRESSE: 'Flytting med endret geografisktilknytning ',
}

export const grunnlagsendringsBeskrivelse: Record<GrunnlagendringshendelseSamsvarType, string> = {
  GRUNNBELOEP: 'Regulering av pensjonen kunne ikke behandles automatisk. Saken må derfor behandles manuelt',
  ANSVARLIGE_FORELDRE: 'andre ansvarlige foreldre i PDL',
  BARN: 'andre barn i PDL',
  DOEDSDATO: 'ny dødsdato i PDL',
  UTLAND: 'ny ut-/innflytting i PDL',
  VERGEMAAL_ELLER_FREMTIDSFULLMAKT: 'annet vergemål i PDL',
  SIVILSTAND: 'endring på sivilstand i PDL',
  INSTITUSJONSOPPHOLD: 'INSTITUSJONSOPPHOLD',
  ADRESSE: 'fått ny geografisk tilknytning men saken kunne ikke flyttes på grunn av åpen behandling',
}

export const grunnlagsendringsKilde = (type: GrunnlagendringshendelseSamsvarType): string => {
  switch (type) {
    case 'GRUNNBELOEP':
      return 'Gjenny'
    case 'INSTITUSJONSOPPHOLD':
      return 'Inst2'
    case 'DOEDSDATO':
    case 'UTLAND':
    case 'BARN':
    case 'ANSVARLIGE_FORELDRE':
    case 'SIVILSTAND':
    case 'VERGEMAAL_ELLER_FREMTIDSFULLMAKT':
    case 'ADRESSE':
      return 'Pdl'
  }
}

const grunnlagsEndringstyperTilRevurderingsAarsaker: Record<GrunnlagsendringsType, Array<string>> = {
  GRUNNBELOEP: [Revurderingaarsak.REGULERING],
  DOEDSFALL: [Revurderingaarsak.DOEDSFALL, Revurderingaarsak.SOESKENJUSTERING],
  UTFLYTTING: [Revurderingaarsak.UTLAND, Revurderingaarsak.EKSPORT],
  FORELDER_BARN_RELASJON: [Revurderingaarsak.ANSVARLIGE_FORELDRE, Revurderingaarsak.BARN, Revurderingaarsak.SIVILSTAND],
  VERGEMAAL_ELLER_FREMTIDSFULLMAKT: [Revurderingaarsak.VERGEMAAL_ELLER_FREMTIDSFULLMAKT],
  SIVILSTAND: [Revurderingaarsak.SIVILSTAND],
  INSTITUSJONSOPPHOLD: [Revurderingaarsak.SOESKENJUSTERING],
  ADRESSE: [Revurderingaarsak.ANNEN],
}

export const stoetterRevurderingAvHendelse = (
  hendelse: Grunnlagsendringshendelse,
  revurderinger: Array<Revurderingaarsak>
): boolean => {
  return revurderinger.some(
    (revurdering) =>
      grunnlagsEndringstyperTilRevurderingsAarsaker[hendelse.type] &&
      grunnlagsEndringstyperTilRevurderingsAarsaker[hendelse.type].includes(revurdering)
  )
}

export const formaterRolle = (sakType: SakType, rolle: Saksrolle) => {
  switch (rolle) {
    case 'AVDOED':
      return `Avdød ${sakType === SakType.BARNEPENSJON ? 'forelder' : 'ektefelle/partner/samboer'}`
    case 'GJENLEVENDE':
      return `Gjenlevende ${sakType === SakType.BARNEPENSJON ? 'forelder' : 'ektefelle/partner/samboer'}`
    case 'INNSENDER':
      return 'Innsender av søknaden'
    case 'SOEKER':
      return 'Søker'
    case 'SOESKEN':
      return `${sakType === SakType.BARNEPENSJON ? 'Søsken' : 'barn'} til søker`
    case 'UKJENT':
      return 'Ukjent person i saken'
  }
}

export const formaterLandList = (
  landliste: { tilflyttingsland?: string; dato?: string; fraflyttingsland?: string }[]
): string => {
  if (landliste.length === 0) {
    return 'Ingen'
  }
  return landliste
    .map(
      (land) =>
        `${land.tilflyttingsland || land.fraflyttingsland} - ${formaterKanskjeStringDatoMedFallback(
          'Ukjent dato',
          land.dato
        )}`
    )
    .join(', ')
}

export const formaterFoedselsnummerMedNavn = (
  fnrTilNavn: Record<string, PersonMedNavn>,
  foedselsnummer: string
): string => {
  const person = fnrTilNavn[foedselsnummer]
  if (person) {
    return `${genererNavn(person)} (${foedselsnummer})`
  }
  return `${foedselsnummer}`
}

const genererNavn = (personInfo: PersonMedNavn) => {
  return [personInfo.fornavn, personInfo.mellomnavn, personInfo.etternavn].join(' ')
}

export const FnrTilNavnMapContext = React.createContext<Record<string, PersonMedNavn>>({})

export const harAapenRevurdering = (behandlinger: IBehandlingsammendrag[]): boolean => {
  return (
    behandlinger
      .filter((behandling) => behandling.behandlingType === IBehandlingsType.REVURDERING)
      .filter((behandling) => !erFerdigBehandlet(behandling.status)).length > 0
  )
}

export const revurderingKanOpprettes = (
  behandlinger: IBehandlingsammendrag[],
  enhetId: string,
  enheter: string[]
): Boolean => {
  return (
    behandlinger.filter((behandling) => behandlingErIverksatt(behandling.status)).length > 0 &&
    enhetErSkrivbar(enhetId, enheter)
  )
}

export const omgjoeringAvslagKanOpprettes = (
  behandlinger: IBehandlingsammendrag[],
  enhet: string,
  enheter: Array<string>
): Boolean => {
  return (
    behandlinger.every(
      (behandling) =>
        behandling.behandlingType != IBehandlingsType.FØRSTEGANGSBEHANDLING ||
        [IBehandlingStatus.AVSLAG, IBehandlingStatus.AVBRUTT].includes(behandling.status)
    ) && enhetErSkrivbar(enhet, enheter)
  )
}

import {
  Grunnlagsendringshendelse,
  GrunnlagendringshendelseSamsvarType,
  GrunnlagsendringsType,
} from '~components/person/typer'
import { formaterKanskjeStringDatoMedFallback } from '~utils/formattering'
import React from 'react'
import { PersonMedNavn } from '~shared/types/grunnlag'
import { Revurderingsaarsak } from '~shared/types/Revurderingsaarsak'

export const grunnlagsendringsTittel: Record<GrunnlagendringshendelseSamsvarType, string> = {
  GRUNNBELOEP: 'Regulering feilet',
  DOEDSDATO: 'Dødsdato',
  UTLAND: 'Ut-/innflytting til Norge',
  BARN: 'Barn',
  ANSVARLIGE_FORELDRE: 'Ansvarlige foreldre',
  VERGEMAAL_ELLER_FREMTIDSFULLMAKT: 'Vergemål eller fremtidsfullmakt',
  INSTITUSJONSOPPHOLD: 'Institusjonsopphold',
}

export const grunnlagsendringsBeskrivelse: Record<GrunnlagendringshendelseSamsvarType, string> = {
  GRUNNBELOEP: 'Regulering av pensjonen kunne ikke behandles automatisk. Saken må derfor behandles manuelt',
  ANSVARLIGE_FORELDRE: 'andre ansvarlige foreldre i PDL',
  BARN: 'andre barn i PDL',
  DOEDSDATO: 'ny dødsdato i PDL',
  UTLAND: 'ny ut-/innflytting i PDL',
  VERGEMAAL_ELLER_FREMTIDSFULLMAKT: 'annet vergemål i PDL',
  INSTITUSJONSOPPHOLD: 'INSTITUSJONSOPPHOLD',
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
    case 'VERGEMAAL_ELLER_FREMTIDSFULLMAKT':
      return 'Pdl'
  }
}

const grunnlagsEndringstyperTilRevurderingsAarsaker: Record<GrunnlagsendringsType, Array<string>> = {
  GRUNNBELOEP: [Revurderingsaarsak.REGULERING],
  DOEDSFALL: [Revurderingsaarsak.DOEDSFALL, Revurderingsaarsak.SOESKENJUSTERING],
  UTFLYTTING: [Revurderingsaarsak.UTLAND],
  FORELDER_BARN_RELASJON: [Revurderingsaarsak.ANSVARLIGE_FORELDRE, Revurderingsaarsak.BARN],
  VERGEMAAL_ELLER_FREMTIDSFULLMAKT: [Revurderingsaarsak.VERGEMAAL_ELLER_FREMTIDSFULLMAKT],
  INSTITUSJONSOPPHOLD: [Revurderingsaarsak.SOESKENJUSTERING],
}

export const stoetterRevurderingAvHendelse = (
  hendelse: Grunnlagsendringshendelse,
  revurderinger: Array<Revurderingsaarsak>
): boolean => {
  return revurderinger.some(
    (revurdering) =>
      grunnlagsEndringstyperTilRevurderingsAarsaker[hendelse.type] &&
      grunnlagsEndringstyperTilRevurderingsAarsaker[hendelse.type].includes(revurdering)
  )
}

export const rolletekst: Record<Grunnlagsendringshendelse['hendelseGjelderRolle'], string> = {
  AVDOED: 'Avdød forelder',
  GJENLEVENDE: 'Gjenlevende forelder',
  INNSENDER: 'Innsender av søknaden',
  SOEKER: 'Søker',
  SOESKEN: 'Søsken til søker',
  UKJENT: 'Ukjent person i saken',
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

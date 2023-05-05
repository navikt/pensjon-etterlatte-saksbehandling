import { Grunnlagsendringshendelse, GrunnlagsendringsType } from '~components/person/typer'
import { formaterKanskjeStringDatoMedFallback } from '~utils/formattering'
import React from 'react'
import { PersonMedNavn } from '~shared/types/grunnlag'

export const grunnlagsendringsTittel: Record<GrunnlagsendringsType, string> = {
  GRUNNBELOEP: 'Regulering feilet',
  DOEDSDATO: 'Dødsdato',
  UTLAND: 'Ut-/innflytting til Norge',
  BARN: 'Barn',
  ANSVARLIGE_FORELDRE: 'Ansvarlige foreldre',
  VERGEMAAL_ELLER_FREMTIDSFULLMAKT: 'Vergemål eller fremtidsfullmakt',
  INSTITUSJONSOPPHOLD: 'Institusjonsopphold',
}

export const grunnlagsendringsBeskrivelse: Record<GrunnlagsendringsType, string> = {
  GRUNNBELOEP: 'Regulering av pensjonen kunne ikke behandles automatisk. Saken må derfor behandles manuelt',
  ANSVARLIGE_FORELDRE: 'andre ansvarlige foreldre i PDL',
  BARN: 'andre barn i PDL',
  DOEDSDATO: 'ny dødsdato i PDL',
  UTLAND: 'ny ut-/innflytting i PDL',
  VERGEMAAL_ELLER_FREMTIDSFULLMAKT: 'annet vergemål i PDL',
  INSTITUSJONSOPPHOLD: 'INSTITUSJONSOPPHOLD',
}

export const grunnlagsendringsKilde = (type: GrunnlagsendringsType): string => {
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

export const stoetterRevurderingAvHendelse = (
  hendelse: Grunnlagsendringshendelse,
  revurderinger: Array<string>
): boolean => {
  return revurderinger.includes(hendelse.type)
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
    return `${person.fornavn} ${person.etternavn} (${foedselsnummer})`
  }
  return `${foedselsnummer}`
}
export const FnrTilNavnMapContext = React.createContext<Record<string, PersonMedNavn>>({})

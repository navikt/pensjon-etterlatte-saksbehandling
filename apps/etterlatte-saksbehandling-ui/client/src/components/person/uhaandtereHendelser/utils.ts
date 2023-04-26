import { GrunnlagsendringsType } from '~components/person/typer'

export const grunnlagsendringsTittel: Record<GrunnlagsendringsType, string> = {
  GRUNNBELOEP: 'Regulering feilet',
  DOEDSDATO: 'Dødsdato',
  UTLAND: 'Utlendingsstatus',
  BARN: 'Barn',
  ANSVARLIGE_FORELDRE: 'Ansvarlige foreldre',
  VERGEMAAL_ELLER_FREMTIDSFULLMAKT: 'Vergemål eller fremtidsfullmakt',
  INSTITUSJONSOPPHOLD: 'Institusjonsopphold',
}

export const grunnlagsendringsBeskrivelse: Record<GrunnlagsendringsType, string> = {
  GRUNNBELOEP: 'Regulering av pensjonen kunne ikke behandles automatisk. Saken må derfor behandles manuelt',
  DOEDSDATO: 'Dødsdato',
  UTLAND: 'Utlendingsstatus',
  BARN: 'Barn',
  ANSVARLIGE_FORELDRE: 'Ansvarlige foreldre',
  VERGEMAAL_ELLER_FREMTIDSFULLMAKT: 'Vergemål eller fremtidsfullmakt',
  INSTITUSJONSOPPHOLD: 'INSTITUSJONSOPPHOLD',
}

export const grunnlagsendringsKilde = (type: GrunnlagsendringsType): string => {
  switch (type) {
    case 'GRUNNBELOEP':
      return 'Doffen'
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

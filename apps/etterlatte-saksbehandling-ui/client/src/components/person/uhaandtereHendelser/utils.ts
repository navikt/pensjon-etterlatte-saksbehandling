import { GrunnlagsendringsType } from '~components/person/typer'

export const grunnlagsendringsTittel: Record<GrunnlagsendringsType, string> = {
  REGULERING: 'Regulering feilet',
  DOEDSDATO: 'Dødsdato',
  UTLAND: 'Utlendingsstatus',
  BARN: 'Barn',
  ANSVARLIGE_FORELDRE: 'Ansvarlige foreldre',
  VERGEMAAL_ELLER_FREMTIDSFULLMAKT: 'Vergemål eller fremtidsfullmakt',
}

export const grunnlagsendringsBeskrivelse: Record<GrunnlagsendringsType, string> = {
  REGULERING: 'Regulering av pensjonen kunne ikke behandles automatisk. Saken må derfor behandles manuelt',
  DOEDSDATO: 'Dødsdato',
  UTLAND: 'Utlendingsstatus',
  BARN: 'Barn',
  ANSVARLIGE_FORELDRE: 'Ansvarlige foreldre',
  VERGEMAAL_ELLER_FREMTIDSFULLMAKT: 'Vergemål eller fremtidsfullmakt',
}

export const grunnlagsendringsKilde = (type: GrunnlagsendringsType): string => {
  switch (type) {
    case 'REGULERING':
      return 'Doffen'
    case 'DOEDSDATO':
    case 'UTLAND':
    case 'BARN':
    case 'ANSVARLIGE_FORELDRE':
    case 'VERGEMAAL_ELLER_FREMTIDSFULLMAKT':
      return 'Pdl'
  }
}

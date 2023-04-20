import { GrunnlagsendringsType, Saksrolle } from '~components/person/typer'

export const teksterForSaksrolle: Record<Saksrolle, string> = {
  AVDOED: 'Avdød',
  GJENLEVENDE: 'Gjenlevende',
  INNSENDER: 'Innsender',
  SOEKER: 'Søker',
  SOESKEN: 'Søsken',
  UKJENT: 'Ukjent',
}

export const teksterForGrunnlagshendelser: Record<GrunnlagsendringsType, string> = {
  ANSVARLIGE_FORELDRE: 'Ansvarlige foreldre',
  BARN: 'Barn',
  UTLAND: 'Ut-/innflytting',
  DOEDSDATO: 'Dødsfall',
  VERGEMAAL_ELLER_FREMTIDSFULLMAKT: 'Vergemål',
  GRUNNBELOEP: 'Regulering',
}

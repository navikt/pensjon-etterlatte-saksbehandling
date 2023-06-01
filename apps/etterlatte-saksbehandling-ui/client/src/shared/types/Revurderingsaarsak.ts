//Denne må matche det som er definert i backend RevurderingAarsak.kt
export enum Revurderingsaarsak {
  REGULERING = 'REGULERING',
  ANSVARLIGE_FORELDRE = 'ANSVARLIGE_FORELDRE',
  SOESKENJUSTERING = 'SOESKENJUSTERING',
  UTLAND = 'UTLAND',
  BARN = 'BARN',
  DOEDSFALL = 'DOEDSFALL',
  VERGEMAAL_ELLER_FREMTIDSFULLMAKT = 'VERGEMAAL_ELLER_FREMTIDSFULLMAKT',
}

export const tekstRevurderingsaarsak: Record<Revurderingsaarsak, string> = {
  REGULERING: 'Regulering',
  ANSVARLIGE_FORELDRE: 'Ansvarlige foreldre',
  SOESKENJUSTERING: 'Søskenjustering',
  UTLAND: 'Utland',
  BARN: 'Barn',
  DOEDSFALL: 'Dødsfall mottaker',
  VERGEMAAL_ELLER_FREMTIDSFULLMAKT: 'Verge / fullmakt',
} as const

export const erOpphoer = (revurderingsaarsak: Revurderingsaarsak) =>
  [Revurderingsaarsak.DOEDSFALL].includes(revurderingsaarsak)

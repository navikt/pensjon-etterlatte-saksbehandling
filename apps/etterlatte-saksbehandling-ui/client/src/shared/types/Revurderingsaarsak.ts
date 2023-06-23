//Denne må matche det som er definert i backend RevurderingAarsak.kt
export enum Revurderingsaarsak {
  REGULERING = 'REGULERING',
  ANSVARLIGE_FORELDRE = 'ANSVARLIGE_FORELDRE',
  SOESKENJUSTERING = 'SOESKENJUSTERING',
  UTLAND = 'UTLAND',
  BARN = 'BARN',
  DOEDSFALL = 'DOEDSFALL',
  VERGEMAAL_ELLER_FREMTIDSFULLMAKT = 'VERGEMAAL_ELLER_FREMTIDSFULLMAKT',
  INNTEKTSENDRING = 'INNTEKTSENDRING',
  OMGJOERING_AV_FARSKAP = 'OMGJOERING_AV_FARSKAP',
  ADOPSJON = 'ADOPSJON',
  SIVILSTAND = 'SIVILSTAND',
}

export const tekstRevurderingsaarsak: Record<Revurderingsaarsak, string> = {
  REGULERING: 'Regulering',
  ANSVARLIGE_FORELDRE: 'Ansvarlige foreldre',
  SOESKENJUSTERING: 'Søskenjustering',
  UTLAND: 'Utland',
  BARN: 'Barn',
  DOEDSFALL: 'Dødsfall mottaker',
  VERGEMAAL_ELLER_FREMTIDSFULLMAKT: 'Verge / fullmakt',
  INNTEKTSENDRING: 'Endring av inntekt',
  OMGJOERING_AV_FARSKAP: 'Omgjøring av farskap',
  ADOPSJON: 'Adopsjon',
  SIVILSTAND: 'Endring av sivilstand',
} as const

export const erOpphoer = (revurderingsaarsak: Revurderingsaarsak) =>
  [
    Revurderingsaarsak.DOEDSFALL,
    Revurderingsaarsak.ADOPSJON,
    Revurderingsaarsak.OMGJOERING_AV_FARSKAP,
    Revurderingsaarsak.SIVILSTAND,
  ].includes(revurderingsaarsak)

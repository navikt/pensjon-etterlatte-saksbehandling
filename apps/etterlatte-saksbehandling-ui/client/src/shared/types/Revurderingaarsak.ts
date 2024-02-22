//Denne må matche det som er definert i backend Revurderingaarsak.kt

export enum Revurderingaarsak {
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
  NY_SOEKNAD = 'NY_SOEKNAD',
  FENGSELSOPPHOLD = 'FENGSELSOPPHOLD',
  ANNEN = 'ANNEN',
  YRKESSKADE = 'YRKESSKADE',
  UT_AV_FENGSEL = 'UT_AV_FENGSEL',
  INSTITUSJONSOPPHOLD = 'INSTITUSJONSOPPHOLD',
  OMGJOERING_ETTER_KLAGE = 'OMGJOERING_ETTER_KLAGE',
  SLUTTBEHANDLING_UTLAND = 'SLUTTBEHANDLING_UTLAND',
  OPPHOER_UTEN_BREV = 'OPPHOER_UTEN_BREV',
  ALDERSOVERGANG = 'ALDERSOVERGANG',
}

export const tekstRevurderingsaarsak: Record<Revurderingaarsak, string> = {
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
  NY_SOEKNAD: 'Det har kommet inn en ny søknad',
  FENGSELSOPPHOLD: 'Fengselsopphold',
  ANNEN: 'Annen',
  YRKESSKADE: 'Yrkesskade',
  UT_AV_FENGSEL: 'Ut av fengsel',
  INSTITUSJONSOPPHOLD: 'Institusjonsopphold',
  OMGJOERING_ETTER_KLAGE: 'Omgjøring etter klage',
  SLUTTBEHANDLING_UTLAND: 'Sluttbehandling utland',
  OPPHOER_UTEN_BREV: 'Opphør uten å sende brev',
  ALDERSOVERGANG: 'Aldersovergang',
} as const

export const erOpphoer = (revurderingsaarsak: Revurderingaarsak) =>
  [
    Revurderingaarsak.DOEDSFALL,
    Revurderingaarsak.ADOPSJON,
    Revurderingaarsak.OMGJOERING_AV_FARSKAP,
    Revurderingaarsak.SIVILSTAND,
    Revurderingaarsak.OPPHOER_UTEN_BREV,
    Revurderingaarsak.ALDERSOVERGANG,
  ].includes(revurderingsaarsak)

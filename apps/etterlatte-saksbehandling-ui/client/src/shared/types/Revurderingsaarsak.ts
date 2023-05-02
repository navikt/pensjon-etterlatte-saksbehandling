export enum Revurderingsaarsak {
  SOESKENJUSTERING = 'SOESKENJUSTERING',
  REGULERING = 'REGULERING',
  DOEDSFALL = 'DOEDSFALL',
}

export const erOpphoer = (revurderingsaarsak: Revurderingsaarsak) =>
  [Revurderingsaarsak.DOEDSFALL].includes(revurderingsaarsak)

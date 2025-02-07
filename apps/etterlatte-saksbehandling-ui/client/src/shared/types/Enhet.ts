export const ENHETER = {
  VELGENHET: 'Velg enhet',
  E4815: 'Ålesund - 4815',
  E4808: 'Porsgrunn - 4808',
  E4817: 'Steinkjer - 4817',
  E4862: 'Ålesund utland - 4862',
  E0001: 'Utland - 0001',
  E4883: 'Egne ansatte - 4883',
  E2103: 'Vikafossen - 2103',
}

export type EnhetFilterKeys = keyof typeof ENHETER

export function filtrerEnhet(enhetsFilter: EnhetFilterKeys): string {
  return enhetsFilter === 'VELGENHET' ? 'VELGENHET' : enhetsFilter.substring(1)
}

export const tekstEnhet: Record<string, string> = {
  '4815': 'Ålesund - 4815',
  '4808': 'Porsgrunn - 4808',
  '4817': 'Steinkjer - 4817',
  '4862': 'Ålesund utland - 4862',
  '0001': 'Utland - 0001',
  '4883': 'Egne ansatte - 4883',
  '2103': 'Vikafossen - 2103',
}

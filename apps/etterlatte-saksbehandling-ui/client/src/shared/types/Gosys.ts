export enum GosysTema {
  PEN = 'PEN',
  EYO = 'EYO',
  EYB = 'EYB',
}

export const GOSYS_TEMA_FILTER: Record<GosysTema, string> = {
  PEN: 'Pensjon',
  EYO: 'Omstillingsstønad',
  EYB: 'Barnepensjon',
}

export const konverterStringTilGosysTema = (value: string): GosysTema => {
  switch (value) {
    case 'Pensjon':
      return GosysTema.PEN
    case 'Omstillingsstønad':
      return GosysTema.EYO
    case 'Barnepensjon':
      return GosysTema.EYB
    default:
      throw Error(`Ukjent gosys tema ${value}`)
  }
}

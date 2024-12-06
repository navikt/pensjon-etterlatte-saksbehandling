import { TilbakekrevingBeloep } from '~shared/types/Tilbakekreving'

// Det er kun ønskelig å vise linjer med klassetype YTEL per nå
export const klasseTypeYtelse = (beloep: TilbakekrevingBeloep) => beloep.klasseType === 'YTEL'

// Legger til orginal index som er nyttig dersom listen filtreres, men senere skal oppdatere verdier
export const leggPaaOrginalIndex = (beloep: TilbakekrevingBeloep, index: number) => ({
  ...beloep,
  originalIndex: index,
})

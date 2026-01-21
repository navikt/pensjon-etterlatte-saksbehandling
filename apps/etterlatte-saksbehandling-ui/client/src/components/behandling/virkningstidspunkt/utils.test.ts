import { describe, expect, it } from 'vitest'
import { hentMinimumsVirkningstidspunkt } from '~components/behandling/virkningstidspunkt/utils'
import { SakType } from '~shared/types/sak'

describe('minimumstidspunkt for virkningstidspunkt er', () => {
  it('maaned etter doedsdato', () => {
    const avdoedDoedsdato = new Date('2020-01-01')

    const minimumsVirkningstidspunkt = hentMinimumsVirkningstidspunkt(avdoedDoedsdato, SakType.BARNEPENSJON)

    const manedEtterDoedsDato = new Date('2020-02-01')
    expect(minimumsVirkningstidspunkt).toStrictEqual(manedEtterDoedsDato)
  })
})

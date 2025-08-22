import { describe, expect, it } from 'vitest'
import { hentMinimumsVirkningstidspunkt } from '~components/behandling/virkningstidspunkt/utils'
import { SakType } from '~shared/types/sak'

describe('minimumstidspunkt for virkningstidspunkt er', () => {
  it('maaned etter doedsdato hvis soknad er mottatt innenfor tre aar', () => {
    const avdoedDoedsdato = new Date('2020-01-01')
    const soeknadMottattDato = '2020-03-01'

    const minimumsVirkningstidspunkt = hentMinimumsVirkningstidspunkt(
      avdoedDoedsdato,
      new Date(soeknadMottattDato),
      SakType.BARNEPENSJON
    )

    const manedEtterDoedsDato = new Date('2020-02-01')
    expect(minimumsVirkningstidspunkt).toStrictEqual(manedEtterDoedsDato)
  })

  it('tre aar fra mottatt soknad hvis soknad er mottatt tre aar etter doedsdato', () => {
    const avdoedDoedsdato = new Date('2016-12-31')
    const soeknadMottattDato = '2020-01-01'

    const minimumsVirkningstidspunkt = hentMinimumsVirkningstidspunkt(
      avdoedDoedsdato,
      new Date(soeknadMottattDato),
      SakType.BARNEPENSJON
    )

    const treAarFoerSoknad = new Date('2017-01-01')
    expect(minimumsVirkningstidspunkt).toStrictEqual(treAarFoerSoknad)
  })
})

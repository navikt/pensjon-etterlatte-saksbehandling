// @vitest-environment jsdom

import React, { act } from 'react'
import { createRoot, Root } from 'react-dom/client'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { FastsettFaktiskInntekt } from './FastsettFaktiskInntekt'
import { IAvkortetYtelse } from '~shared/types/IAvkorting'
import { getDefaultOptions, setDefaultOptions } from 'date-fns'
import { nb } from 'date-fns/locale'

const mocks = vi.hoisted(() => {
  const avkortetYtelse: IAvkortetYtelse[] = []
  return {
    toggleAktiv: true,
    forbehandling: {
      innvilgetPeriode: { fom: '2025-01', tom: '2025-12' },
    },
    opplysninger: {
      tidligereAvkorting: { avkortetYtelse },
    },
  }
})

vi.mock('~store/reducers/EtteroppgjoerReducer', () => ({
  useEtteroppgjoerForbehandling: () => mocks,
}))
vi.mock('~useUnleash', () => ({
  FeatureToggle: { beregn_over_flere_perioder: 'beregn_over_flere_perioder' },
  useFeaturetoggle: () => mocks.toggleAktiv,
}))
vi.mock('./FaktiskInntektSkjema', () => ({
  FaktiskInntektSkjema: () => <span>Inntektsskjema</span>,
}))
vi.mock('./FaktiskInntektVisning', () => ({ FaktiskInntektVisning: () => null }))

const ytelse = (fom: string, tom: string): IAvkortetYtelse => ({
  id: fom,
  fom,
  tom,
  type: 'AARSOPPGJOER',
  ytelseFoerAvkorting: 7558,
  avkortingsbeloep: 10000,
  restanse: 0,
  ytelseEtterAvkorting: 0,
})

describe('fastsett faktisk inntekt med hull i aaret', () => {
  let root: Root
  let container: HTMLDivElement
  const opprinneligLocale = getDefaultOptions().locale

  beforeEach(() => {
    setDefaultOptions({ locale: nb })
    vi.stubGlobal('IS_REACT_ACT_ENVIRONMENT', true)
    mocks.toggleAktiv = true
    mocks.opplysninger.tidligereAvkorting.avkortetYtelse = [ytelse('2025-01', '2025-03'), ytelse('2025-07', '2025-12')]
    container = document.createElement('div')
    document.body.appendChild(container)
    root = createRoot(container)
  })

  afterEach(async () => {
    await act(async () => root.unmount())
    setDefaultOptions({ locale: opprinneligLocale })
    container.remove()
    vi.unstubAllGlobals()
  })

  const render = async () => {
    await act(async () =>
      root.render(<FastsettFaktiskInntekt erRedigerbar setFastsettFaktiskInntektSkjemaErrors={vi.fn()} />)
    )
  }

  it('viser begge perioder og skjema selv om ytelsen etter avkorting er null', async () => {
    await render()

    expect(container.textContent?.toLowerCase()).toContain('januar 2025 - mars 2025')
    expect(container.textContent?.toLowerCase()).toContain('juli 2025 - desember 2025')
    expect(container.textContent?.toLowerCase()).not.toContain('januar 2025 - desember 2025')
    expect(container.textContent).toContain('Inntektsskjema')
    expect(container.textContent).toContain('Inntekt i opphørsperioder mellom dem skal ikke tas med.')
  })

  it('blokkerer inntektsregistrering naar periodene mangler i grunnlaget', async () => {
    mocks.opplysninger.tidligereAvkorting.avkortetYtelse = []
    await render()

    expect(container.textContent).toContain('innvilgede perioder mangler i beregningsgrunnlaget')
    expect(container.textContent).not.toContain('Inntektsskjema')
  })

  it('blokkerer flerperiode-inntekt naar toggle er av', async () => {
    mocks.toggleAktiv = false
    await render()

    expect(container.textContent).toContain('Etteroppgjør med flere innvilgede perioder er ikke aktivert.')
    expect(container.textContent).not.toContain('Inntektsskjema')
  })

  it.each([true, false])(
    'beholder vanlig veiledning for en sammenhengende periode med toggle %s',
    async (toggleAktiv) => {
      mocks.toggleAktiv = toggleAktiv
      mocks.opplysninger.tidligereAvkorting.avkortetYtelse = [
        ytelse('2025-01', '2025-04'),
        ytelse('2025-05', '2025-12'),
      ]
      await render()

      expect(container.textContent).toContain('Inntektsskjema')
      expect(container.textContent?.toLowerCase()).toContain('januar 2025 - desember 2025')
      expect(container.textContent).toContain('Fastsett den faktiske inntekten for bruker i den innvilgede perioden.')
      expect(container.textContent).not.toContain('Inntekt i opphørsperioder mellom dem')
    }
  )

  it('viser historiske flerperiode-oppgjoer selv om toggle er av', async () => {
    mocks.toggleAktiv = false
    await act(async () =>
      root.render(<FastsettFaktiskInntekt erRedigerbar={false} setFastsettFaktiskInntektSkjemaErrors={vi.fn()} />)
    )

    expect(container.textContent?.toLowerCase()).toContain('januar 2025 - mars 2025')
    expect(container.textContent?.toLowerCase()).toContain('juli 2025 - desember 2025')
    expect(container.textContent).not.toContain('ikke aktivert')
  })
})

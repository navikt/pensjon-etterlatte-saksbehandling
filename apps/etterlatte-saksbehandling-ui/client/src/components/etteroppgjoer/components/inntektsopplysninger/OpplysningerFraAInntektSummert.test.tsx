import { ComponentProps } from 'react'
import { renderToStaticMarkup } from 'react-dom/server'
import { describe, expect, it, vi } from 'vitest'
import { Avkorting, SummerteInntekterAOrdningen } from '~shared/types/EtteroppgjoerForbehandling'
import { OpplysningerFraAInntektSummert } from './OpplysningerFraAInntektSummert'

vi.mock('./LenkeTilInntektOversikt', () => ({
  LenkeTilInntektOversikt: () => null,
}))

const inntektsmaaneder = Array.from({ length: 12 }, (_, indeks) => ({
  maaned: `2025-${String(indeks + 1).padStart(2, '0')}`,
  beloep: 0,
}))

const inntekter: SummerteInntekterAOrdningen = {
  afp: { filter: 'afp', inntekter: inntektsmaaneder },
  loenn: { filter: 'loenn', inntekter: inntektsmaaneder },
  oms: { filter: 'oms', inntekter: inntektsmaaneder },
  tidspunktBeregnet: '2026-01-01T12:00:00Z',
}

const avkorting: Avkorting = JSON.parse(`{
  "avkortingGrunnlag": [],
  "avkortetYtelse": [
    {
      "id": "april",
      "fom": "2025-04",
      "tom": "2025-04",
      "type": "AARSOPPGJOER",
      "ytelseFoerAvkorting": 10000,
      "avkortingsbeloep": 5000,
      "restanse": 0,
      "ytelseEtterAvkorting": 5000
    },
    {
      "id": "mai",
      "fom": "2025-05",
      "tom": null,
      "type": "AARSOPPGJOER",
      "ytelseFoerAvkorting": 10000,
      "avkortingsbeloep": 10000,
      "restanse": 0,
      "ytelseEtterAvkorting": 0
    }
  ]
}`)

function hentSvar(props: Partial<ComponentProps<typeof OpplysningerFraAInntektSummert>> = {}) {
  const markup = renderToStaticMarkup(
    <OpplysningerFraAInntektSummert
      inntekter={inntekter}
      avkorting={avkorting}
      innvilgetPeriode={{ fom: '2025-04', tom: '2025-08' }}
      {...props}
    />
  )
  return [...markup.matchAll(/>(Ja|Nei)<\/td>/g)].map((match) => match[1])
}

describe('OpplysningerFraAInntektSummert', () => {
  it('viser Nei før innvilgelse og etter opphør selv om avkortingen er åpen', () => {
    expect(hentSvar()).toEqual(['Nei', 'Nei', 'Nei', 'Ja', 'Ja', 'Ja', 'Ja', 'Ja', 'Nei', 'Nei', 'Nei', 'Nei'])
  })

  it('viser Ja ut året når den innvilgede perioden varer ut året', () => {
    expect(hentSvar({ innvilgetPeriode: { fom: '2025-04', tom: '2025-12' } })).toEqual([
      'Nei',
      'Nei',
      'Nei',
      'Ja',
      'Ja',
      'Ja',
      'Ja',
      'Ja',
      'Ja',
      'Ja',
      'Ja',
      'Ja',
    ])
  })

  it('viser Nei før innvilgelse selv om avkortingen starter tidligere', () => {
    expect(hentSvar({ innvilgetPeriode: { fom: '2025-05', tom: '2025-08' } })).toEqual([
      'Nei',
      'Nei',
      'Nei',
      'Nei',
      'Ja',
      'Ja',
      'Ja',
      'Ja',
      'Nei',
      'Nei',
      'Nei',
      'Nei',
    ])
  })

  it('respekterer avkortingens sluttdato innenfor den innvilgede perioden', () => {
    expect(
      hentSvar({
        avkorting: {
          ...avkorting,
          avkortetYtelse: avkorting.avkortetYtelse.map((ytelse) => ({ ...ytelse, tom: ytelse.tom ?? '2025-06' })),
        },
      })
    ).toEqual(['Nei', 'Nei', 'Nei', 'Ja', 'Ja', 'Ja', 'Nei', 'Nei', 'Nei', 'Nei', 'Nei', 'Nei'])
  })

  it.each([0, -1])('viser Nei når ytelsen før avkorting er %s', (beloep) => {
    expect(
      hentSvar({
        avkorting: {
          ...avkorting,
          avkortetYtelse: avkorting.avkortetYtelse.map((ytelse) => ({ ...ytelse, ytelseFoerAvkorting: beloep })),
        },
      })
    ).toEqual(Array(12).fill('Nei'))
  })

  it('viser Nei når det ikke finnes avkorting for måneden', () => {
    expect(hentSvar({ avkorting: { avkortingGrunnlag: [], avkortetYtelse: [] } })).toEqual(Array(12).fill('Nei'))
  })

  it('skjuler ytelsesraden når avkorting mangler', () => {
    const markup = renderToStaticMarkup(
      <OpplysningerFraAInntektSummert inntekter={inntekter} innvilgetPeriode={{ fom: '2025-04', tom: '2025-08' }} />
    )
    expect(markup).not.toContain('Har ytelse')
  })
})

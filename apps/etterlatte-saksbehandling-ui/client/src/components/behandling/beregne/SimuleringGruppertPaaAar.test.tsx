import { renderToStaticMarkup } from 'react-dom/server'
import { describe, expect, it } from 'vitest'
import type { SimulertBeregning, SimulertBeregningsperiode } from '~shared/types/Utbetaling'
import { NOK } from '~utils/formatering/formatering'
import { SimuleringGruppertPaaAar } from './SimuleringGruppertPaaAar'

describe('SimuleringGruppertPaaAar', () => {
  it('viser skattetrekk, andre trekk og riktig label for feilutbetaling', () => {
    const data: SimulertBeregning = {
      gjelderId: '17111357722',
      datoBeregnet: '2024-06-18',
      infomelding: null,
      beloep: 0,
      kommendeUtbetalinger: [],
      etterbetaling: [
        {
          fom: '2024-04-01',
          tom: '2024-04-30',
          gjelderId: '17111357722',
          forfall: '2024-06-18',
          utbetalesTilId: '17111357722',
          feilkonto: false,
          enhet: '4819',
          konto: '4134010',
          behandlingskode: '1',
          beloep: 20970,
          tilbakefoering: false,
          klassekode: 'BARNEPENSJON-OPTP',
          klassekodeBeskrivelse: 'Barnepensjon',
          klasseType: 'YTEL',
        },
        {
          fom: '2024-04-01',
          tom: '2024-04-30',
          gjelderId: '17111357722',
          forfall: '2024-06-18',
          utbetalesTilId: '17111357722',
          feilkonto: false,
          enhet: '4819',
          konto: '4134010',
          behandlingskode: '1',
          beloep: -9885,
          tilbakefoering: true,
          klassekode: 'BARNEPENSJON-OPTP',
          klassekodeBeskrivelse: 'Barnepensjon',
          klasseType: 'YTEL',
        },
        {
          fom: '2024-04-01',
          tom: '2024-04-30',
          gjelderId: '17111357722',
          forfall: '2024-06-18',
          utbetalesTilId: '17111357722',
          feilkonto: false,
          enhet: '4819',
          konto: '4134010',
          behandlingskode: '1',
          beloep: -9885,
          tilbakefoering: true,
          klassekode: 'BARNEPENSJON-OPTP',
          klassekodeBeskrivelse: 'Barnepensjon',
          klasseType: 'YTEL',
        },
        {
          fom: '2024-04-01',
          tom: '2024-04-30',
          gjelderId: '17111357722',
          forfall: '2024-06-18',
          utbetalesTilId: '17111357722',
          feilkonto: false,
          enhet: '4819',
          konto: '0630997',
          behandlingskode: '0',
          beloep: -200,
          tilbakefoering: false,
          klassekode: 'FSKTSKAT',
          klassekodeBeskrivelse: 'Forskuddsskatt',
          klasseType: 'SKAT',
        },
        {
          fom: '2024-04-01',
          tom: '2024-04-30',
          gjelderId: '17111357722',
          forfall: '2024-06-18',
          utbetalesTilId: '17111357722',
          feilkonto: false,
          enhet: '4819',
          konto: '4134010',
          behandlingskode: '1',
          beloep: 30,
          tilbakefoering: false,
          klassekode: 'TBTREKK',
          klassekodeBeskrivelse: 'Trekk',
          klasseType: 'TREK',
        },
      ],
      tilbakekreving: [
        {
          fom: '2024-04-01',
          tom: '2024-04-30',
          gjelderId: '17111357722',
          forfall: '2024-06-18',
          utbetalesTilId: '17111357722',
          feilkonto: false,
          enhet: '4819',
          konto: '0630997',
          behandlingskode: '0',
          beloep: 8896,
          tilbakefoering: false,
          klassekode: 'KL_KODE_FEIL_PEN',
          klassekodeBeskrivelse: 'Feilutbetaling pensjoner',
          klasseType: 'FEIL',
        },
      ],
      oppsummeringer: {
        perAar: [
          {
            aarstall: 2024,
            oppsummering: {
              etterbetaling: {
                brutto: 1200,
                skatt: -200,
                andreTrekk: 30,
                netto: 1030,
              },
              feilutbetaling: {
                brutto: 19770,
                beloepBrukerenSkulleHatt: 10874,
                netto: 8896,
              },
            },
          },
        ],
        forPerioden: {
          etterbetaling: {
            brutto: 1200,
            skatt: -200,
            andreTrekk: 30,
            netto: 1030,
          },
          feilutbetaling: {
            brutto: 19770,
            beloepBrukerenSkulleHatt: 10874,
            netto: 8896,
          },
        },
      },
    }

    const markup = renderToStaticMarkup(<SimuleringGruppertPaaAar data={data} />)

    expect(markup).toContain('Skattetrekk')
    expect(markup).toContain('Andre trekk')
    expect(markup).toContain('Beløp brukeren skulle hatt')
    expect(markup).not.toContain('Oppsummering av simuleringen er ikke tilgjengelig')
  })

  it('viser etterbetaling og brutto feilutbetaling separat per år og for hele perioden', () => {
    const etterbetaling2024 = { brutto: 451, skatt: 0, andreTrekk: 0, netto: 451 }
    const etterbetaling2025 = { brutto: 200, skatt: 0, andreTrekk: 0, netto: 200 }
    const feilutbetaling = { brutto: 9885, beloepBrukerenSkulleHatt: 989, netto: 8896 }
    const data: SimulertBeregning = {
      gjelderId: '17111357722',
      datoBeregnet: '2025-06-18',
      infomelding: null,
      beloep: 0,
      kommendeUtbetalinger: [],
      etterbetaling: [],
      tilbakekreving: [],
      oppsummeringer: {
        perAar: [
          { aarstall: 2025, oppsummering: { etterbetaling: etterbetaling2025, feilutbetaling: null } },
          { aarstall: 2024, oppsummering: { etterbetaling: etterbetaling2024, feilutbetaling } },
        ],
        forPerioden: {
          etterbetaling: { brutto: 651, skatt: 0, andreTrekk: 0, netto: 651 },
          feilutbetaling,
        },
      },
    }

    const markup = renderToStaticMarkup(<SimuleringGruppertPaaAar data={data} />)
    const rader = markup.match(/<tr\b[^>]*>[\s\S]*?<\/tr>/g) ?? []
    const rad = (label: string) => rader.find((rad) => rad.includes(`>${label}</td>`))

    for (const { suffix, bruttoEtterbetaling } of [
      { suffix: '', bruttoEtterbetaling: 451 },
      { suffix: ' for perioden', bruttoEtterbetaling: 651 },
    ]) {
      const etterbetalingsrader = rader.filter((rad) => rad.includes(`>Brutto etterbetaling${suffix}</td>`))
      expect(etterbetalingsrader.some((rad) => rad.includes(NOK(bruttoEtterbetaling)))).toBe(true)
      expect(rad(`Tidligere utbetalt brutto${suffix}`)).toContain(NOK(9885))
      expect(rad(`Beløp brukeren skulle hatt${suffix}`)).toContain(NOK(989))
      expect(rad(`Brutto feilutbetaling${suffix}`)).toContain(NOK(8896))
    }
    expect(markup).not.toContain('Netto feilutbetaling')
    expect(markup).not.toContain(NOK(8445))
  })

  it.each([{}, { oppsummeringer: null }])(
    'viser varsel og detaljer uten å lage nullbeløp når oppsummeringen mangler (%j)',
    (oppsummeringer) => {
      const periode: SimulertBeregningsperiode = {
        fom: '2024-04-01',
        tom: '2024-04-30',
        gjelderId: '17111357722',
        forfall: '2024-06-18',
        utbetalesTilId: '17111357722',
        feilkonto: false,
        enhet: '4819',
        konto: '4134010',
        behandlingskode: '1',
        beloep: 451,
        tilbakefoering: false,
        klassekode: 'BARNEPENSJON-OPTP',
        klassekodeBeskrivelse: 'Barnepensjon',
        klasseType: 'YTEL',
      }
      const data: SimulertBeregning = {
        gjelderId: '17111357722',
        datoBeregnet: '2024-06-18',
        infomelding: null,
        beloep: 0,
        kommendeUtbetalinger: [],
        etterbetaling: [periode],
        tilbakekreving: [
          {
            ...periode,
            beloep: 8896,
            konto: '0630997',
            behandlingskode: '0',
            klassekode: 'KL_KODE_FEIL_PEN',
            klassekodeBeskrivelse: 'Feilutbetaling pensjoner',
            klasseType: 'FEIL',
          },
        ],
        ...oppsummeringer,
      }

      const markup = renderToStaticMarkup(<SimuleringGruppertPaaAar data={data} />)

      expect(markup).toContain('Oppsummering av simuleringen er ikke tilgjengelig')
      expect(markup).toContain('>Etterbetaling<')
      expect(markup).toContain('>Tilbakekreving<')
      expect(markup).toContain(NOK(451))
      expect(markup).toContain(NOK(8896))
      expect(markup).not.toContain(NOK(0))
      expect(markup).not.toContain('Ingen perioder')
      expect(markup).not.toContain('Brutto etterbetaling')
      expect(markup).not.toContain('Brutto feilutbetaling')
    }
  )

  it('viser manglende oppsummering eksplisitt selv om det ikke finnes detaljer', () => {
    const data: SimulertBeregning = {
      gjelderId: '17111357722',
      datoBeregnet: '2024-06-18',
      infomelding: null,
      beloep: 0,
      kommendeUtbetalinger: [],
      etterbetaling: [],
      tilbakekreving: [],
    }

    const markup = renderToStaticMarkup(<SimuleringGruppertPaaAar data={data} />)

    expect(markup).toContain('Oppsummering av simuleringen er ikke tilgjengelig')
    expect(markup).not.toContain(NOK(0))
    expect(markup).not.toContain('Ingen perioder')
    expect(markup).not.toContain('Brutto etterbetaling')
    expect(markup).not.toContain('Brutto feilutbetaling')
  })

  it('skjuler andre trekk når beløpet er 0', () => {
    const utenAndreTrekk = { brutto: 1200, skatt: -200, andreTrekk: 0, netto: 1000 }
    const data: SimulertBeregning = {
      gjelderId: '17111357722',
      datoBeregnet: '2024-06-18',
      infomelding: null,
      beloep: 0,
      kommendeUtbetalinger: [],
      etterbetaling: [],
      tilbakekreving: [],
      oppsummeringer: {
        perAar: [{ aarstall: 2024, oppsummering: { etterbetaling: utenAndreTrekk, feilutbetaling: null } }],
        forPerioden: { etterbetaling: utenAndreTrekk, feilutbetaling: null },
      },
    }

    const markup = renderToStaticMarkup(<SimuleringGruppertPaaAar data={data} />)

    expect(markup).toContain('Skattetrekk')
    expect(markup).not.toContain('Andre trekk')
  })
})

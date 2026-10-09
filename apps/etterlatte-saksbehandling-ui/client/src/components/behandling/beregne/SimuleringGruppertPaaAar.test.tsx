import { renderToStaticMarkup } from 'react-dom/server'
import { describe, expect, it } from 'vitest'
import type { SimulertBeregning } from '~shared/types/Utbetaling'
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

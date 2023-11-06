import { describe, expect, it } from 'vitest'
import { IVilkaarsvurdering } from '~shared/api/vilkaarsvurdering'
import { vilkaarsvurderingErPaaNyttRegelverk } from '~components/behandling/vilkaarsvurdering/utils'

describe('Tester hjelpefunksjoner for vilkårsvurdering', () => {
  it('barnepensjon vilkaarsvurdering er ikke paa nytt regelverk', () => {
    const vilkaarsVurderingPaaGammeltRegelverk: IVilkaarsvurdering = {
      vilkaar: [
        {
          hovedvilkaar: {
            type: 'BP_FORMAAL',
            tittel: 'Lever barnet?',
            beskrivelse:
              'Formålet med barnepensjon er å sikre inntekt for barn når en av foreldrene eller begge er døde. Dette betyr at barnet må være i live for å ha rett på barnepensjon.',
            spoersmaal: 'Lever barnet det søkes barnepensjon for på virkningstidspunktet?',
            lovreferanse: {
              paragraf: '§ 18-1',
              lenke: 'https://lovdata.no/lov/1997-02-28-19/%C2%A718-3',
            },
          },
          unntaksvilkaar: [],
          grunnlag: [],
          id: '6f745fd1-1598-4b0a-96ea-e73d975c2a69',
        },
      ],
      virkningstidspunkt: '2023-11',
      isYrkesskade: false,
    }

    expect(vilkaarsvurderingErPaaNyttRegelverk(vilkaarsVurderingPaaGammeltRegelverk)).toBeFalsy()
  })

  it('barnepensjon vilkaarsvurdering er paa nytt regelverk', () => {
    const vilkaarsVurderingPaaNyttRegelverk: IVilkaarsvurdering = {
      vilkaar: [
        {
          hovedvilkaar: {
            type: 'BP_FORMAAL_2024',
            tittel: 'Lever barnet?',
            beskrivelse:
              'Formålet med barnepensjon er å sikre inntekt for barn når en av foreldrene eller begge er døde. Dette betyr at barnet må være i live for å ha rett på barnepensjon.',
            spoersmaal: 'Lever barnet det søkes barnepensjon for på virkningstidspunktet?',
            lovreferanse: {
              paragraf: '§ 18-1',
              lenke: 'https://lovdata.no/lov/1997-02-28-19/%C2%A718-3',
            },
          },
          unntaksvilkaar: [],
          grunnlag: [],
          id: '6f745fd1-1598-4b0a-96ea-e73d975c2a69',
        },
      ],
      virkningstidspunkt: '2023-11',
      isYrkesskade: false,
    }

    expect(vilkaarsvurderingErPaaNyttRegelverk(vilkaarsVurderingPaaNyttRegelverk)).toBeTruthy()
  })

  it('OMS vilkaarsvurdering er ikke paa nytt regelverk', () => {
    const omsVilkaarsvurdering: IVilkaarsvurdering = {
      vilkaar: [
        {
          hovedvilkaar: {
            type: 'OMS_FORMAAL',
            tittel: 'Lever barnet?',
            beskrivelse:
              'Formålet med barnepensjon er å sikre inntekt for barn når en av foreldrene eller begge er døde. Dette betyr at barnet må være i live for å ha rett på barnepensjon.',
            spoersmaal: 'Lever barnet det søkes barnepensjon for på virkningstidspunktet?',
            lovreferanse: {
              paragraf: '§ 18-1',
              lenke: 'https://lovdata.no/lov/1997-02-28-19/%C2%A718-3',
            },
          },
          unntaksvilkaar: [],
          grunnlag: [],
          id: '6f745fd1-1598-4b0a-96ea-e73d975c2a69',
        },
      ],
      virkningstidspunkt: '2023-11',
      isYrkesskade: false,
    }

    expect(vilkaarsvurderingErPaaNyttRegelverk(omsVilkaarsvurdering)).toBeFalsy()
  })
})

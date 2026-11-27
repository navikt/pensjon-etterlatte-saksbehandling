// @vitest-environment jsdom

import { act } from 'react'
import { createRoot, Root } from 'react-dom/client'
import { Provider } from 'react-redux'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import * as utbetalingApi from '~shared/api/utbetaling'
import { VilkaarsvurderingResultat } from '~shared/api/vilkaarsvurdering'
import { IBehandlingStatus, IBehandlingsType, Opprinnelse, Vedtaksloesning } from '~shared/types/IDetaljertBehandling'
import { IAvkorting } from '~shared/types/IAvkorting'
import { SakType } from '~shared/types/sak'
import { store } from '~store/Store'
import { IBehandlingReducer, oppdaterAvkorting, setBehandling } from '~store/reducers/BehandlingReducer'
import { useBehandling } from '~components/behandling/useBehandling'
import { SimulerUtbetaling } from './SimulerUtbetaling'

const avkorting: IAvkorting = {
  redigerbarForventetInntekt: undefined,
  redigerbarForventetInntektNesteAar: undefined,
  avkortingGrunnlag: [],
  avkortetYtelse: [],
  tidligereAvkortetYtelse: [],
}

const behandling: IBehandlingReducer = {
  id: 'behandling-id',
  sakId: 1,
  sakType: SakType.OMSTILLINGSSTOENAD,
  sakEnhetId: '4808',
  relatertBehandlingId: null,
  kommerBarnetTilgode: null,
  virkningstidspunkt: null,
  utlandstilknytning: null,
  boddEllerArbeidetUtlandet: null,
  status: IBehandlingStatus.AVKORTET,
  hendelser: [],
  behandlingType: IBehandlingsType.REVURDERING,
  revurderingsaarsak: null,
  revurderinginfo: null,
  begrunnelse: null,
  kilde: Vedtaksloesning.GJENNY,
  sendeBrev: true,
  viderefoertOpphoer: null,
  tidligereFamiliepleier: null,
  erSluttbehandling: false,
  opprinnelse: Opprinnelse.SAKSBEHANDLER,
}

function SimuleringFraStore() {
  const behandling = useBehandling()
  return behandling && <SimulerUtbetaling behandling={behandling} />
}

describe('SimulerUtbetaling', () => {
  let container: HTMLDivElement
  let root: Root

  beforeEach(() => {
    vi.stubGlobal('IS_REACT_ACT_ENVIRONMENT', true)
    vi.spyOn(utbetalingApi, 'simulerUtbetaling').mockResolvedValue({ ok: true, status: 200, data: null! })
    container = document.createElement('div')
    document.body.appendChild(container)
    root = createRoot(container)
  })

  afterEach(async () => {
    await act(async () => root.unmount())
    container.remove()
    vi.restoreAllMocks()
    vi.unstubAllGlobals()
  })

  async function render(endringer: Partial<IBehandlingReducer>) {
    store.dispatch(setBehandling({ ...behandling, ...endringer } as IBehandlingReducer))
    await act(async () => {
      root.render(
        <Provider store={store}>
          <SimuleringFraStore />
        </Provider>
      )
    })
  }

  it('venter med å simulere OMS til ny avkorting er hentet', async () => {
    await render({ avkorting: undefined })
    expect(utbetalingApi.simulerUtbetaling).not.toHaveBeenCalled()

    await act(async () => {
      store.dispatch(oppdaterAvkorting(avkorting))
    })

    expect(utbetalingApi.simulerUtbetaling).toHaveBeenCalledExactlyOnceWith(behandling.id)
  })

  it('simulerer BP uten avkorting', async () => {
    await render({ sakType: SakType.BARNEPENSJON, status: IBehandlingStatus.BEREGNET, avkorting: undefined })

    expect(utbetalingApi.simulerUtbetaling).toHaveBeenCalledExactlyOnceWith(behandling.id)
  })

  it('simulerer opphør i OMS uten avkorting', async () => {
    await render({
      status: IBehandlingStatus.VILKAARSVURDERT,
      avkorting: undefined,
      vilkaarsvurdering: {
        vilkaar: [],
        virkningstidspunkt: '2024-02-01',
        isYrkesskade: false,
        resultat: { utfall: VilkaarsvurderingResultat.IKKE_OPPFYLT, saksbehandler: 'Z123456', tidspunkt: new Date() },
      },
    })

    expect(utbetalingApi.simulerUtbetaling).toHaveBeenCalledExactlyOnceWith(behandling.id)
  })
})

// @vitest-environment jsdom

import { beforeEach, describe, expect, it } from 'vitest'
import { renderToStaticMarkup } from 'react-dom/server'
import { Provider } from 'react-redux'
import { MemoryRouter } from 'react-router'
import { store } from '~store/Store'
import {
  IBehandlingReducer,
  resetBehandling,
  setBehandling,
  updateVilkaarsvurdering,
} from '~store/reducers/BehandlingReducer'
import { IBehandlingStatus, IBehandlingsType, Opprinnelse, Vedtaksloesning } from '~shared/types/IDetaljertBehandling'
import { Revurderingaarsak } from '~shared/types/Revurderingaarsak'
import { SakType } from '~shared/types/sak'
import { KildeType } from '~shared/types/kilde'
import { VilkaarsvurderingResultat } from '~shared/api/vilkaarsvurdering'
import { useBehandlingRoutes } from './BehandlingRoutes'
import { useVedtaksResultat } from './useVedtaksResultat'

const behandling: IBehandlingReducer = {
  id: 'behandling-id',
  sakId: 1,
  sakType: SakType.OMSTILLINGSSTOENAD,
  sakEnhetId: '4808',
  relatertBehandlingId: null,
  kommerBarnetTilgode: null,
  virkningstidspunkt: {
    dato: '2027-01-01',
    kilde: { type: KildeType.saksbehandler, ident: 'Z123456', tidspunkt: '2026-11-16T12:00:00Z' },
    begrunnelse: 'Forlengelse',
    kravdato: null,
  },
  utlandstilknytning: null,
  boddEllerArbeidetUtlandet: null,
  status: IBehandlingStatus.OPPRETTET,
  hendelser: [],
  behandlingType: IBehandlingsType.REVURDERING,
  revurderingsaarsak: Revurderingaarsak.FORLENGET_OMS,
  revurderinginfo: null,
  begrunnelse: null,
  kilde: Vedtaksloesning.GJENNY,
  sendeBrev: true,
  viderefoertOpphoer: null,
  tidligereFamiliepleier: null,
  erSluttbehandling: false,
  opprinnelse: Opprinnelse.SAKSBEHANDLER,
}

function StegOgUtfall() {
  const { behandlingRoutes, routeErGyldig } = useBehandlingRoutes()
  const utfall = useVedtaksResultat()
  return (
    <>
      <output>{behandlingRoutes.map((route) => route.path).join(',')}</output>
      <output>{utfall}</output>
      <output>{String(routeErGyldig('vilkaarsvurdering'))}</output>
    </>
  )
}

const renderStegOgUtfall = () =>
  renderToStaticMarkup(
    <Provider store={store}>
      <MemoryRouter initialEntries={['/behandling/behandling-id/revurderingsoversikt']}>
        <StegOgUtfall />
      </MemoryRouter>
    </Provider>
  )

function settUtfall(utfall: VilkaarsvurderingResultat) {
  store.dispatch(
    updateVilkaarsvurdering({
      vilkaar: [],
      virkningstidspunkt: '2027-01-01',
      isYrkesskade: false,
      resultat: { utfall, saksbehandler: 'Z123456', tidspunkt: new Date('2026-11-16T12:00:00Z') },
    })
  )
}

describe('Revurderingsflyt', () => {
  beforeEach(() => {
    store.dispatch(resetBehandling())
    store.dispatch(setBehandling(behandling))
  })

  it('forlenget OMS har eget aktivitetssteg og tilgjengelig vilkårsvurdering før utfall er valgt', () => {
    const html = renderStegOgUtfall()

    expect(html).toContain(
      '<output>revurderingsoversikt,vilkaarsvurdering,forlenget-oms-aktivitet,trygdetid,beregningsgrunnlag,beregne,brev</output>'
    )
    expect(html).toContain('<output>true</output>')
  })

  it('oppfylte vilkår gir innvilgelse for forlenget OMS', () => {
    settUtfall(VilkaarsvurderingResultat.OPPFYLT)

    expect(renderStegOgUtfall()).toContain('<output>innvilget</output>')
  })

  it('avslag beholder aktivitetssiden for redigering, men fjerner beregning', () => {
    settUtfall(VilkaarsvurderingResultat.IKKE_OPPFYLT)
    const html = renderStegOgUtfall()

    expect(html).toContain('<output>revurderingsoversikt,vilkaarsvurdering,forlenget-oms-aktivitet,brev</output>')
    expect(html).toContain('<output>avslag</output>')
  })

  it('vanlig revurdering beholder aktivitetspliktsteg og endringsutfall', () => {
    store.dispatch(setBehandling({ ...behandling, revurderingsaarsak: Revurderingaarsak.ANNEN }))
    settUtfall(VilkaarsvurderingResultat.OPPFYLT)
    const html = renderStegOgUtfall()

    expect(html).toContain(
      '<output>revurderingsoversikt,vilkaarsvurdering,aktivitetsplikt,trygdetid,beregningsgrunnlag,beregne,brev</output>'
    )
    expect(html).toContain('<output>endring</output>')
  })

  it('etteroppgjør beholder sin egen flyt', () => {
    store.dispatch(setBehandling({ ...behandling, revurderingsaarsak: Revurderingaarsak.ETTEROPPGJOER }))

    expect(renderStegOgUtfall()).toContain('<output>etteroppgjoeroversikt,beregne,brev</output>')
  })
})

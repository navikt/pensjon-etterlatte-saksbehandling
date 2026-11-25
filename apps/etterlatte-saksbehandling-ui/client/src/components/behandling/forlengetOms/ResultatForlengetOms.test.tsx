// @vitest-environment jsdom

import { act, ReactNode } from 'react'
import { createRoot, Root } from 'react-dom/client'
import { Provider, useSelector } from 'react-redux'
import { MemoryRouter } from 'react-router'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { apiClient } from '~shared/api/apiClient'
import { ForlengetOmsAktivitet, LagretForlengetOmsVurdering } from '~shared/api/forlengetOms'
import { VilkaarsvurderingResultat } from '~shared/api/vilkaarsvurdering'
import { IBehandlingStatus, IBehandlingsType, Opprinnelse, Vedtaksloesning } from '~shared/types/IDetaljertBehandling'
import { KildeType } from '~shared/types/kilde'
import { Revurderingaarsak } from '~shared/types/Revurderingaarsak'
import { SakType } from '~shared/types/sak'
import { JaNei } from '~shared/types/ISvar'
import { store } from '~store/Store'
import { IBehandlingReducer, resetBehandling, setBehandling } from '~store/reducers/BehandlingReducer'
import { ResultatForlengetOms } from './ResultatForlengetOms'
import { ForlengetOmsAktivitet as ForlengetOmsAktivitetSide } from './ForlengetOmsAktivitet'

vi.mock('~components/behandling/handlinger/BehandlingHandlingKnapper', () => ({
  BehandlingHandlingKnapper: ({ children }: { children: ReactNode }) => <>{children}</>,
}))

vi.mock('~components/behandling/useInnloggetSaksbehandler', () => ({
  useInnloggetSaksbehandler: () => ({ skriveEnheter: ['4808'] }),
}))

vi.mock('~components/person/usePersonopplysninger', () => ({
  usePersonopplysningerOmsAvdoede: () => undefined,
}))

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
  vilkaarsvurdering: { vilkaar: [], virkningstidspunkt: '2027-01-01', isYrkesskade: false },
}

describe('Lagre vurdering av aktivitet', () => {
  let container: HTMLDivElement
  let root: Root

  beforeEach(() => {
    store.dispatch(resetBehandling())
    store.dispatch(setBehandling(behandling))
    container = document.createElement('div')
    document.body.appendChild(container)
    root = createRoot(container)
  })

  afterEach(async () => {
    await act(async () => root.unmount())
    container.remove()
    vi.restoreAllMocks()
  })

  async function renderResultat(aktivitet: ForlengetOmsAktivitet) {
    function Resultat() {
      const oppdatertBehandling = useSelector(() => store.getState().behandlingReducer.behandling)
      return (
        <ResultatForlengetOms
          behandling={oppdatertBehandling!}
          redigerbar
          aktivitet={aktivitet}
          resultatBegrunnelse={{ tekst: 'Begrunnelse', onChange: vi.fn() }}
          validerAktivitet={() => true}
          validerPeriode={(onSuccess) => onSuccess({ type: 'FORLENGET_OMS', fom: '2027-01', tom: '2027-06' })}
        />
      )
    }
    await act(async () => {
      root.render(
        <Provider store={store}>
          <MemoryRouter>
            <Resultat />
          </MemoryRouter>
        </Provider>
      )
    })
  }

  function response(aktivitet: ForlengetOmsAktivitet, utfall: 'INNVILGELSE' | 'AVSLAG'): LagretForlengetOmsVurdering {
    return {
      vurdering: { behandlingId: behandling.id, aktivitet, begrunnelse: 'Begrunnelse', utfall },
      periode: utfall === 'INNVILGELSE' ? { type: 'FORLENGET_OMS', fom: '2027-01', tom: '2027-06' } : null,
      vilkaarsvurdering: {
        vilkaar: [],
        virkningstidspunkt: '2027-01-01',
        isYrkesskade: false,
        resultat: {
          utfall: utfall === 'INNVILGELSE' ? VilkaarsvurderingResultat.OPPFYLT : VilkaarsvurderingResultat.IKKE_OPPFYLT,
          saksbehandler: 'Z123456',
          tidspunkt: new Date(),
          kommentar: 'Begrunnelse',
        },
      },
    }
  }

  async function velgOgLagre(utfall: VilkaarsvurderingResultat) {
    const radio = container.querySelector<HTMLInputElement>(`input[value="${utfall}"]`)!
    await act(async () => radio.click())
    const lagre = Array.from(container.querySelectorAll('button')).find((button) => button.textContent === 'Lagre')!
    await act(async () => lagre.click())
  }

  it('sender aktivitet periode og utfall i ett kall og oppdaterer behandling', async () => {
    const aktivitet: ForlengetOmsAktivitet = {
      type: 'ARBEIDSRETTET_TILTAK',
      tiltak: 'Tiltak',
      omfang: 'DELTID',
      aktivitetsplan: JaNei.NEI,
    }
    const post = vi.spyOn(apiClient, 'post').mockResolvedValue({
      ok: true,
      status: 200,
      data: response(aktivitet, 'INNVILGELSE'),
    })
    await renderResultat(aktivitet)
    await velgOgLagre(VilkaarsvurderingResultat.OPPFYLT)

    expect(post).toHaveBeenCalledExactlyOnceWith('/behandling/behandling-id/forlenget-oms', {
      aktivitet,
      begrunnelse: 'Begrunnelse',
      utfall: 'INNVILGELSE',
      periode: { type: 'FORLENGET_OMS', fom: '2027-01', tom: '2027-06' },
    })
    expect(store.getState().behandlingReducer.behandling?.status).toBe(IBehandlingStatus.VILKAARSVURDERT)
    expect(store.getState().behandlingReducer.behandling?.revurderinginfo?.revurderingInfo).toEqual({
      type: 'FORLENGET_OMS',
      fom: '2027-01',
      tom: '2027-06',
    })
    expect(container.textContent).toContain('Forlenget OMS innvilges')
    expect(container.textContent).toContain('Rediger vurdering')
  })

  it('lagrer avslag uten periode og viser feil uten aa laase ved mislykket lagring', async () => {
    const aktivitet: ForlengetOmsAktivitet = { type: 'NEI' }
    const post = vi.spyOn(apiClient, 'post').mockResolvedValue({ ok: false, status: 400, detail: 'Kunne ikke lagre' })
    await renderResultat(aktivitet)
    await velgOgLagre(VilkaarsvurderingResultat.IKKE_OPPFYLT)

    expect(post).toHaveBeenCalledExactlyOnceWith('/behandling/behandling-id/forlenget-oms', {
      aktivitet,
      begrunnelse: 'Begrunnelse',
      utfall: 'AVSLAG',
      periode: undefined,
    })
    expect(container.textContent).toContain('Kunne ikke lagre')
    expect(store.getState().behandlingReducer.behandling?.status).toBe(IBehandlingStatus.OPPRETTET)
  })

  it('fjerner en tidligere periode fra behandling ved lagring av avslag', async () => {
    const aktivitet: ForlengetOmsAktivitet = { type: 'NEI' }
    const behandlingMedPeriode: IBehandlingReducer = {
      ...behandling,
      revurderinginfo: {
        revurderingInfo: { type: 'FORLENGET_OMS', fom: '2027-01', tom: '2027-06' },
        begrunnelse: undefined,
      },
    }
    store.dispatch(setBehandling(behandlingMedPeriode))
    vi.spyOn(apiClient, 'post').mockResolvedValue({
      ok: true,
      status: 200,
      data: response(aktivitet, 'AVSLAG'),
    })
    await renderResultat(aktivitet)
    await velgOgLagre(VilkaarsvurderingResultat.IKKE_OPPFYLT)

    expect(store.getState().behandlingReducer.behandling?.revurderinginfo).toBeNull()
    expect(container.textContent).toContain('Forlenget OMS avslås')
  })

  it('henter lagrede svar og beholder dem naar vurderingen aapnes for redigering', async () => {
    const lagret = response({ type: 'NEI' }, 'AVSLAG')
    const lagretBehandling: IBehandlingReducer = {
      ...behandling,
      status: IBehandlingStatus.VILKAARSVURDERT,
      vilkaarsvurdering: lagret.vilkaarsvurdering,
    }
    store.dispatch(setBehandling(lagretBehandling))
    const get = vi.spyOn(apiClient, 'get').mockResolvedValue({ ok: true, status: 200, data: lagret.vurdering })
    const slett = vi.spyOn(apiClient, 'delete').mockResolvedValue({
      ok: true,
      status: 200,
      data: { ...lagret.vilkaarsvurdering, resultat: undefined },
    })
    function AktivitetSide() {
      const oppdatertBehandling = useSelector(() => store.getState().behandlingReducer.behandling)
      return <ForlengetOmsAktivitetSide behandling={oppdatertBehandling!} />
    }
    await act(async () => {
      root.render(
        <Provider store={store}>
          <MemoryRouter>
            <AktivitetSide />
          </MemoryRouter>
        </Provider>
      )
    })

    expect(get).toHaveBeenCalledExactlyOnceWith('/behandling/behandling-id/forlenget-oms')
    expect(container.querySelector<HTMLInputElement>('input[value="NEI"]')?.checked).toBe(true)
    const begrunnelse = container.querySelector('textarea')!
    expect(begrunnelse.value).toBe('Begrunnelse')
    expect(begrunnelse.readOnly).toBe(true)

    const rediger = Array.from(container.querySelectorAll('button')).find(
      (button) => button.textContent === 'Rediger vurdering'
    )!
    await act(async () => rediger.click())

    expect(slett).toHaveBeenCalledExactlyOnceWith('/vilkaarsvurdering/resultat/behandling-id')
    expect(begrunnelse.value).toBe('Begrunnelse')
    expect(begrunnelse.readOnly).toBe(false)
    expect(container.querySelector<HTMLInputElement>('input[value="IKKE_OPPFYLT"]')?.checked).toBe(true)
    expect(store.getState().behandlingReducer.behandling?.status).toBe(IBehandlingStatus.OPPRETTET)
  })
})

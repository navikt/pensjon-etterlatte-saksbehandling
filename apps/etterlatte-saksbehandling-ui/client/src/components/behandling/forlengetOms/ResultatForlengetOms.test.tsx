// @vitest-environment jsdom

import { act, ReactNode } from 'react'
import { createRoot, Root } from 'react-dom/client'
import { Provider, useSelector } from 'react-redux'
import { MemoryRouter } from 'react-router'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { apiClient, ApiError } from '~shared/api/apiClient'
import {
  AktivitetOgUtfall,
  ForlengetOmsAktivitet,
  ForlengetOmsUtfall,
  LagretForlengetOmsVurdering,
} from '~shared/api/forlengetOms'
import { VilkaarsvurderingResultat } from '~shared/api/vilkaarsvurdering'
import { IBehandlingStatus, IBehandlingsType, Opprinnelse, Vedtaksloesning } from '~shared/types/IDetaljertBehandling'
import { KildeType } from '~shared/types/kilde'
import { Revurderingaarsak } from '~shared/types/Revurderingaarsak'
import { SakType } from '~shared/types/sak'
import { JaNei } from '~shared/types/ISvar'
import { store } from '~store/Store'
import {
  IBehandlingReducer,
  oppdaterRevurderingInfo,
  resetBehandling,
  setBehandling,
  updateVilkaarsvurdering,
} from '~store/reducers/BehandlingReducer'
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
    vi.stubGlobal('IS_REACT_ACT_ENVIRONMENT', true)
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
    vi.unstubAllGlobals()
  })

  async function renderAktivitet(aktivitetOgUtfall: AktivitetOgUtfall | null) {
    const get = vi.spyOn(apiClient, 'get').mockResolvedValue({ ok: true, status: 200, data: aktivitetOgUtfall })
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
    return get
  }

  function response(aktivitet: ForlengetOmsAktivitet, utfall: 'INNVILGELSE' | 'AVSLAG'): LagretForlengetOmsVurdering {
    const data: LagretForlengetOmsVurdering = {
      vurdering: { behandlingId: behandling.id, aktivitet, begrunnelse: 'Begrunnelse', utfall },
      periode: utfall === 'INNVILGELSE' ? { type: 'FORLENGET_OMS', fom: '2027-01', tom: '2027-06' } : null,
      vilkaarsvurdering: {
        vilkaar: [],
        virkningstidspunkt: '2027-01-01',
        isYrkesskade: false,
        resultat: {
          utfall: utfall === 'INNVILGELSE' ? VilkaarsvurderingResultat.OPPFYLT : VilkaarsvurderingResultat.IKKE_OPPFYLT,
          saksbehandler: 'Z123456',
          tidspunkt: new Date('2026-11-16T12:00:00Z'),
          kommentar: 'Begrunnelse',
        },
      },
    }
    return JSON.parse(JSON.stringify(data))
  }

  async function velgOgLagre(utfall: ForlengetOmsUtfall) {
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
    store.dispatch(
      oppdaterRevurderingInfo({
        revurderingInfo: { type: 'FORLENGET_OMS', fom: '2027-01', tom: '2027-06' },
        begrunnelse: undefined,
      })
    )
    await renderAktivitet(response(aktivitet, 'INNVILGELSE').vurdering)
    await velgOgLagre('INNVILGELSE')

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
    for (const felt of container.querySelectorAll<HTMLInputElement>('input[type="text"]')) {
      expect(felt.readOnly).toBe(true)
    }
  })

  it('lagrer avslag uten periode og viser feil uten aa laase ved mislykket lagring', async () => {
    const aktivitet: ForlengetOmsAktivitet = { type: 'NEI' }
    const post = vi.spyOn(apiClient, 'post').mockResolvedValue({ ok: false, status: 400, detail: 'Kunne ikke lagre' })
    await renderAktivitet(response(aktivitet, 'AVSLAG').vurdering)
    await velgOgLagre('AVSLAG')

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
    await renderAktivitet(null)
    await act(async () => container.querySelector<HTMLInputElement>('input[value="NEI"]')!.click())
    const begrunnelse = container.querySelector('textarea')!
    await act(async () => {
      Object.getOwnPropertyDescriptor(HTMLTextAreaElement.prototype, 'value')!.set!.call(begrunnelse, 'Begrunnelse')
      begrunnelse.dispatchEvent(new Event('input', { bubbles: true }))
    })
    const neste = Array.from(container.querySelectorAll('button')).find((button) => button.textContent === 'Neste')!
    expect(neste.disabled).toBe(true)
    const lagre = Array.from(container.querySelectorAll('button')).find((button) => button.textContent === 'Lagre')!
    expect(lagre.disabled).toBe(false)

    await velgOgLagre('AVSLAG')

    expect(store.getState().behandlingReducer.behandling?.revurderinginfo).toBeNull()
    expect(container.textContent).toContain('Forlenget OMS avslås')
    expect(begrunnelse.readOnly).toBe(true)
    expect(neste.disabled).toBe(false)
  })

  it.each([
    { virkningstidspunkt: '2026-12-01', isGrunnlagUtdatert: false },
    { virkningstidspunkt: '2027-01-01', isGrunnlagUtdatert: true },
  ])('varsler og hindrer neste ved utdatert vurdering: %j', async ({ virkningstidspunkt, isGrunnlagUtdatert }) => {
    const lagret = response({ type: 'NEI' }, 'AVSLAG')
    store.dispatch(
      setBehandling({
        ...behandling,
        status: IBehandlingStatus.VILKAARSVURDERT,
        vilkaarsvurdering: { ...lagret.vilkaarsvurdering, virkningstidspunkt, isGrunnlagUtdatert },
      })
    )
    await renderAktivitet(lagret.vurdering)

    expect(container.textContent?.includes('Virkningstidspunktet er endret siden vilkårene ble vurdert sist.')).toBe(
      !isGrunnlagUtdatert
    )
    expect(
      container.textContent?.includes('OBS! Grunnlaget for vilkårsvurderingen har blitt oppdatert siden sist.')
    ).toBe(isGrunnlagUtdatert)
    const neste = Array.from(container.querySelectorAll('button')).find((button) => button.textContent === 'Neste')!
    expect(neste.disabled).toBe(true)
  })

  it('beholder lagrede svar laast til backend har aapnet vurderingen for redigering', async () => {
    const lagret = response({ type: 'NEI' }, 'AVSLAG')
    lagret.vilkaarsvurdering.resultat!.kommentar = 'Kommentar fra vilkår'
    const lagretBehandling: IBehandlingReducer = {
      ...behandling,
      status: IBehandlingStatus.VILKAARSVURDERT,
      vilkaarsvurdering: lagret.vilkaarsvurdering,
    }
    store.dispatch(setBehandling(lagretBehandling))
    let fullfoer!: (error: ApiError) => void
    const slett = vi
      .spyOn(apiClient, 'delete')
      .mockImplementationOnce(
        () =>
          new Promise<ApiError>((resolve) => {
            fullfoer = resolve
          })
      )
      .mockResolvedValue({
        ok: true,
        status: 200,
        data: { ...lagret.vilkaarsvurdering, resultat: undefined },
      })
    const get = await renderAktivitet(lagret.vurdering)

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
    expect(begrunnelse.readOnly).toBe(true)
    const neste = Array.from(container.querySelectorAll('button')).find((button) => button.textContent === 'Neste')!
    expect(neste.disabled).toBe(true)

    await act(async () => {
      fullfoer({ ok: false, status: 400, detail: 'Kunne ikke åpne vurderingen' })
    })

    expect(begrunnelse.readOnly).toBe(true)
    expect(store.getState().behandlingReducer.behandling?.vilkaarsvurdering?.resultat).toEqual(
      lagret.vilkaarsvurdering.resultat
    )
    expect(container.textContent).toContain('Kunne ikke åpne vurderingen')

    await act(async () => rediger.click())

    expect(slett).toHaveBeenCalledTimes(2)
    expect(begrunnelse.value).toBe('Begrunnelse')
    expect(begrunnelse.readOnly).toBe(false)
    expect(container.querySelector<HTMLInputElement>('input[value="AVSLAG"]')?.checked).toBe(true)
    expect(store.getState().behandlingReducer.behandling?.status).toBe(IBehandlingStatus.OPPRETTET)
  })

  it.each<ForlengetOmsAktivitet>([
    { type: 'NEI' },
    { type: 'UTDANNING', skole: 'Skole', studium: 'Studium', studererMinst50Prosent: JaNei.JA },
    { type: 'ARBEIDSRETTET_TILTAK', tiltak: 'Tiltak', omfang: 'DELTID', aktivitetsplan: JaNei.NEI },
  ])('laaser aktivitetssvar under lagring og aapner dem etter feil: $type', async (aktivitet) => {
    let fullfoer!: (error: ApiError) => void
    const post = vi.spyOn(apiClient, 'post').mockImplementationOnce(
      () =>
        new Promise<ApiError>((resolve) => {
          fullfoer = resolve
        })
    )
    await renderAktivitet(response(aktivitet, 'AVSLAG').vurdering)
    await velgOgLagre('AVSLAG')

    expect(post).toHaveBeenCalledTimes(1)
    const aktivitetsvalg = container.querySelector<HTMLInputElement>('input[value="NEI"]')!
    const begrunnelse = container.querySelector('textarea')!
    expect(aktivitetsvalg.disabled).toBe(true)
    expect(begrunnelse.readOnly).toBe(true)
    expect(container.querySelector<HTMLInputElement>('input[value="AVSLAG"]')?.disabled).toBe(true)
    for (const felt of container.querySelectorAll<HTMLInputElement>('input[type="text"]')) {
      expect(felt.readOnly).toBe(true)
    }

    await act(async () => {
      fullfoer({ ok: false, status: 400, detail: 'Kunne ikke lagre' })
    })

    expect(aktivitetsvalg.disabled).toBe(false)
    expect(begrunnelse.readOnly).toBe(false)
    expect(container.querySelector<HTMLInputElement>('input[value="AVSLAG"]')?.disabled).toBe(false)
    for (const felt of container.querySelectorAll<HTMLInputElement>('input[type="text"]')) {
      expect(felt.readOnly).toBe(false)
    }
    expect(container.textContent).toContain('Kunne ikke lagre')
    expect(store.getState().behandlingReducer.behandling?.status).toBe(IBehandlingStatus.OPPRETTET)
  })

  it('validerer periode ved innvilgelse og lagrer avslag uten periode', async () => {
    const aktivitet: ForlengetOmsAktivitet = {
      type: 'UTDANNING',
      skole: 'Skole',
      studium: 'Studium',
      studererMinst50Prosent: JaNei.JA,
    }
    const post = vi.spyOn(apiClient, 'post').mockResolvedValue({
      ok: true,
      status: 200,
      data: response(aktivitet, 'AVSLAG'),
    })
    await renderAktivitet(response(aktivitet, 'INNVILGELSE').vurdering)
    await velgOgLagre('INNVILGELSE')

    expect(post).not.toHaveBeenCalled()
    expect(container.textContent).toContain('Innvilgelse krever en fra og med- og til og med-måned.')
    expect(store.getState().behandlingReducer.behandling?.status).toBe(IBehandlingStatus.OPPRETTET)

    await velgOgLagre('AVSLAG')

    expect(post).toHaveBeenCalledExactlyOnceWith('/behandling/behandling-id/forlenget-oms', {
      aktivitet,
      begrunnelse: 'Begrunnelse',
      utfall: 'AVSLAG',
      periode: undefined,
    })
    expect(container.textContent).toContain('Forlenget OMS avslås')
  })

  it('lagrer endrede svar for arbeidsrettet tiltak fra skjemaet', async () => {
    const aktivitet: ForlengetOmsAktivitet = {
      type: 'ARBEIDSRETTET_TILTAK',
      tiltak: 'Nytt tiltak',
      omfang: 'HELTID',
      aktivitetsplan: JaNei.JA,
    }
    const post = vi.spyOn(apiClient, 'post').mockResolvedValue({
      ok: true,
      status: 200,
      data: response(aktivitet, 'AVSLAG'),
    })
    await renderAktivitet(
      response(
        { type: 'ARBEIDSRETTET_TILTAK', tiltak: 'Lagret tiltak', omfang: 'DELTID', aktivitetsplan: JaNei.NEI },
        'AVSLAG'
      ).vurdering
    )
    const tiltak = container.querySelector<HTMLInputElement>('input[type="text"]')!
    const begrunnelse = container.querySelector('textarea')!
    expect(tiltak.value).toBe('Lagret tiltak')
    expect(begrunnelse.value).toBe('Begrunnelse')
    expect(container.querySelector<HTMLInputElement>('input[value="DELTID"]')!.checked).toBe(true)
    expect(container.querySelectorAll<HTMLInputElement>('input[value="NEI"]')[1].checked).toBe(true)

    await act(async () => {
      Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value')!.set!.call(tiltak, aktivitet.tiltak)
      tiltak.dispatchEvent(new Event('input', { bubbles: true }))
      Object.getOwnPropertyDescriptor(HTMLTextAreaElement.prototype, 'value')!.set!.call(
        begrunnelse,
        'Oppdatert begrunnelse'
      )
      begrunnelse.dispatchEvent(new Event('input', { bubbles: true }))
    })
    await act(async () => container.querySelector<HTMLInputElement>('input[value="HELTID"]')!.click())
    await act(async () => container.querySelector<HTMLInputElement>('input[value="JA"]')!.click())
    await velgOgLagre('AVSLAG')

    expect(post).toHaveBeenCalledExactlyOnceWith('/behandling/behandling-id/forlenget-oms', {
      aktivitet,
      begrunnelse: 'Oppdatert begrunnelse',
      utfall: 'AVSLAG',
      periode: undefined,
    })
    expect(container.textContent).toContain('Fra dato')
    expect(container.textContent).toContain('Til dato')
  })

  it('lagrer endrede utdanningssvar fra skjemaet', async () => {
    const aktivitet: ForlengetOmsAktivitet = {
      type: 'UTDANNING',
      skole: 'Ny skole',
      studium: 'Nytt studium',
      studererMinst50Prosent: JaNei.NEI,
    }
    const post = vi.spyOn(apiClient, 'post').mockResolvedValue({
      ok: true,
      status: 200,
      data: response(aktivitet, 'AVSLAG'),
    })
    await renderAktivitet(
      response(
        { type: 'UTDANNING', skole: 'Lagret skole', studium: 'Lagret studium', studererMinst50Prosent: JaNei.JA },
        'AVSLAG'
      ).vurdering
    )
    const [skole, studium] = container.querySelectorAll<HTMLInputElement>('input[type="text"]')
    for (const [felt, verdi] of [
      [skole, aktivitet.skole],
      [studium, aktivitet.studium],
    ] as const) {
      await act(async () => {
        Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value')!.set!.call(felt, verdi)
        felt.dispatchEvent(new Event('input', { bubbles: true }))
      })
    }
    await act(async () => container.querySelectorAll<HTMLInputElement>('input[value="NEI"]')[1].click())
    const begrunnelse = container.querySelector('textarea')!
    await act(async () => {
      Object.getOwnPropertyDescriptor(HTMLTextAreaElement.prototype, 'value')!.set!.call(
        begrunnelse,
        'Oppdatert begrunnelse'
      )
      begrunnelse.dispatchEvent(new Event('input', { bubbles: true }))
    })
    await velgOgLagre('AVSLAG')

    expect(post).toHaveBeenCalledExactlyOnceWith('/behandling/behandling-id/forlenget-oms', {
      aktivitet,
      begrunnelse: 'Oppdatert begrunnelse',
      utfall: 'AVSLAG',
      periode: undefined,
    })
  })

  it('viser begrunnelse uten at aktivitetssvar er valgt', async () => {
    await renderAktivitet(null)
    const begrunnelse = container.querySelector('textarea')!
    expect(begrunnelse.readOnly).toBe(false)
    await act(async () => {
      Object.getOwnPropertyDescriptor(HTMLTextAreaElement.prototype, 'value')!.set!.call(
        begrunnelse,
        'Selvstendig begrunnelse'
      )
      begrunnelse.dispatchEvent(new Event('input', { bubbles: true }))
    })

    await act(async () => container.querySelector<HTMLInputElement>('input[value="UTDANNING"]')!.click())

    expect(container.querySelector<HTMLInputElement>('input[value="JA"]')!.checked).toBe(false)
    expect(container.querySelector('textarea')!.value).toBe('Selvstendig begrunnelse')
    expect(
      Array.from(container.querySelectorAll('label')).filter((label) => label.textContent === 'Begrunnelse')
    ).toHaveLength(1)
  })

  it('tillater valg av utfall foer aktivitet og viser manglende svar ved lagring', async () => {
    const post = vi.spyOn(apiClient, 'post')
    await renderAktivitet(null)
    const lagre = Array.from(container.querySelectorAll('button')).find((button) => button.textContent === 'Lagre')!
    expect(lagre.disabled).toBe(false)
    expect(container.querySelector<HTMLInputElement>('input[value="AVSLAG"]')!.disabled).toBe(false)

    await act(async () => lagre.click())

    expect(post).not.toHaveBeenCalled()
    expect(container.textContent).toContain('Velg om bruker tar utdanning eller deltar i arbeidsrettet tiltak.')
    expect(container.textContent).toContain('Velg om forlenget OMS innvilges eller avslås.')
    expect(container.textContent).toContain('Oppgi en begrunnelse.')

    await act(async () => container.querySelector<HTMLInputElement>('input[value="AVSLAG"]')!.click())
    expect(container.querySelector<HTMLInputElement>('input[value="AVSLAG"]')!.checked).toBe(true)
  })

  it('beholder begrunnelse og ulagrede aktivitetssvar ved aktivitetsbytte', async () => {
    const aktivitet: ForlengetOmsAktivitet = {
      type: 'UTDANNING',
      skole: 'Lagret skole',
      studium: 'Lagret studium',
      studererMinst50Prosent: JaNei.JA,
    }
    await renderAktivitet(response(aktivitet, 'AVSLAG').vurdering)
    const skole = container.querySelector<HTMLInputElement>('input[type="text"]')!
    const begrunnelse = container.querySelector('textarea')!
    await act(async () => {
      Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value')!.set!.call(skole, 'Ulagret skole')
      skole.dispatchEvent(new Event('input', { bubbles: true }))
      Object.getOwnPropertyDescriptor(HTMLTextAreaElement.prototype, 'value')!.set!.call(
        begrunnelse,
        'Ulagret begrunnelse'
      )
      begrunnelse.dispatchEvent(new Event('input', { bubbles: true }))
    })

    await act(async () => container.querySelector<HTMLInputElement>('input[value="ARBEIDSRETTET_TILTAK"]')!.click())
    expect(container.querySelector<HTMLInputElement>('input[type="text"]')!.value).toBe('')
    expect(container.querySelector('textarea')!.value).toBe('Ulagret begrunnelse')
    expect(container.querySelector<HTMLInputElement>('input[value="AVSLAG"]')!.checked).toBe(true)

    await act(async () => container.querySelector<HTMLInputElement>('input[value="UTDANNING"]')!.click())
    expect(container.querySelector<HTMLInputElement>('input[type="text"]')!.value).toBe('Ulagret skole')
    expect(container.querySelector('textarea')!.value).toBe('Ulagret begrunnelse')
  })

  it('henter nye svar og nullstiller utkast naar behandling endres', async () => {
    const get = await renderAktivitet(response({ type: 'NEI' }, 'AVSLAG').vurdering)
    get.mockResolvedValueOnce({ ok: true, status: 200, data: null })

    await act(async () => {
      store.dispatch(setBehandling({ ...behandling, id: 'ny-behandling-id' }))
    })

    expect(get).toHaveBeenLastCalledWith('/behandling/ny-behandling-id/forlenget-oms')
    expect(container.querySelector<HTMLInputElement>('input[value="NEI"]')!.checked).toBe(false)
    expect(container.querySelector<HTMLInputElement>('input[value="AVSLAG"]')!.checked).toBe(false)
    expect(container.querySelector('textarea')!.value).toBe('')
    const lagre = Array.from(container.querySelectorAll('button')).find((button) => button.textContent === 'Lagre')!
    expect(lagre.disabled).toBe(false)
  })

  it.each([
    ['2026-12', 'desember 2026'],
    ['2027-02', 'februar 2027'],
  ])('henter og lar saksbehandler endre fra-maaned uavhengig av virkningstidspunkt: %s', async (fom, visning) => {
    const aktivitet: ForlengetOmsAktivitet = {
      type: 'UTDANNING',
      skole: 'Skole',
      studium: 'Studium',
      studererMinst50Prosent: JaNei.JA,
    }
    store.dispatch(
      oppdaterRevurderingInfo({
        revurderingInfo: { type: 'FORLENGET_OMS', fom, tom: '2027-06' },
        begrunnelse: undefined,
      })
    )
    const lagret = response(aktivitet, 'INNVILGELSE')
    lagret.periode = { type: 'FORLENGET_OMS', fom: '2027-03', tom: '2027-06' }
    const post = vi.spyOn(apiClient, 'post').mockResolvedValue({ ok: true, status: 200, data: lagret })
    await renderAktivitet(lagret.vurdering)

    const fra = container.querySelector<HTMLInputElement>('#fom')!
    expect(fra.readOnly).toBe(false)
    expect(fra.value).toBe(visning)
    await act(async () => {
      Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value')!.set!.call(fra, 'mars 2027')
      fra.dispatchEvent(new Event('input', { bubbles: true }))
      fra.dispatchEvent(new FocusEvent('focusout', { bubbles: true }))
    })
    await velgOgLagre('INNVILGELSE')

    expect(post).toHaveBeenCalledExactlyOnceWith('/behandling/behandling-id/forlenget-oms', {
      aktivitet,
      begrunnelse: 'Begrunnelse',
      utfall: 'INNVILGELSE',
      periode: { type: 'FORLENGET_OMS', fom: '2027-03', tom: '2027-06' },
    })
    expect(store.getState().behandlingReducer.behandling?.virkningstidspunkt?.dato).toBe('2027-01-01')
  })

  it.each([
    { felt: 'fom', verdi: '', melding: 'Innvilgelse krever en fra og med- og til og med-måned.' },
    { felt: 'tom', verdi: '', melding: 'Innvilgelse krever en fra og med- og til og med-måned.' },
    { felt: 'tom', verdi: 'desember 2026', melding: 'Til og med-måneden må være lik eller etter fra og med-måneden.' },
  ])('validerer en endret periode ved lagring: $felt $verdi', async ({ felt, verdi, melding }) => {
    const aktivitet: ForlengetOmsAktivitet = {
      type: 'ARBEIDSRETTET_TILTAK',
      tiltak: 'Tiltak',
      omfang: 'HELTID',
      aktivitetsplan: JaNei.JA,
    }
    store.dispatch(
      oppdaterRevurderingInfo({
        revurderingInfo: { type: 'FORLENGET_OMS', fom: '2027-01', tom: '2027-06' },
        begrunnelse: undefined,
      })
    )
    const post = vi.spyOn(apiClient, 'post')
    await renderAktivitet(response(aktivitet, 'INNVILGELSE').vurdering)
    const maaned = container.querySelector<HTMLInputElement>(`#${felt}`)!
    await act(async () => {
      Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value')!.set!.call(maaned, verdi)
      maaned.dispatchEvent(new Event('input', { bubbles: true }))
      maaned.dispatchEvent(new FocusEvent('focusout', { bubbles: true }))
    })
    await velgOgLagre('INNVILGELSE')

    expect(post).not.toHaveBeenCalled()
    expect(container.textContent).toContain(melding)
    expect(store.getState().behandlingReducer.behandling?.status).toBe(IBehandlingStatus.OPPRETTET)
  })

  it('viser backendfeil ved manglende vilkaarsvurdering', async () => {
    const post = vi.spyOn(apiClient, 'post').mockResolvedValue({
      ok: false,
      status: 400,
      detail: 'Vilkårene må vurderes før vurderingen av aktivitet kan lagres.',
    })
    await renderAktivitet(null)
    await act(async () => container.querySelector<HTMLInputElement>('input[value="UTDANNING"]')!.click())
    await act(async () => container.querySelector<HTMLInputElement>('input[value="AVSLAG"]')!.click())
    const lagre = Array.from(container.querySelectorAll('button')).find((button) => button.textContent === 'Lagre')!
    expect(lagre.disabled).toBe(false)

    const [skole, studium] = container.querySelectorAll<HTMLInputElement>('input[type="text"]')
    for (const felt of [skole, studium]) {
      await act(async () => {
        Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value')!.set!.call(felt, 'Svar')
        felt.dispatchEvent(new Event('input', { bubbles: true }))
      })
      expect(lagre.disabled).toBe(false)
    }
    await act(async () => container.querySelector<HTMLInputElement>('input[value="JA"]')!.click())
    expect(lagre.disabled).toBe(false)

    const begrunnelse = container.querySelector('textarea')!
    for (const tekst of [' ', 'Begrunnelse']) {
      await act(async () => {
        Object.getOwnPropertyDescriptor(HTMLTextAreaElement.prototype, 'value')!.set!.call(begrunnelse, tekst)
        begrunnelse.dispatchEvent(new Event('input', { bubbles: true }))
      })
      expect(lagre.disabled).toBe(false)
    }
    await act(async () => {
      store.dispatch(updateVilkaarsvurdering(undefined))
    })
    expect(lagre.disabled).toBe(false)

    await velgOgLagre('AVSLAG')

    expect(post).toHaveBeenCalledExactlyOnceWith('/behandling/behandling-id/forlenget-oms', {
      aktivitet: { type: 'UTDANNING', skole: 'Svar', studium: 'Svar', studererMinst50Prosent: JaNei.JA },
      begrunnelse: 'Begrunnelse',
      utfall: 'AVSLAG',
      periode: undefined,
    })
    expect(container.textContent).toContain(
      'Kunne ikke lagre vurderingen av aktivitet: Vilkårene må vurderes før vurderingen av aktivitet kan lagres.'
    )
    const neste = Array.from(container.querySelectorAll('button')).find((button) => button.textContent === 'Neste')!
    expect(neste.disabled).toBe(true)
  })

  it.each([
    { type: 'UTDANNING', melding: 'Fyll ut skole, studium og om bruker studerer minst 50 prosent.' },
    { type: 'ARBEIDSRETTET_TILTAK', melding: 'Fyll ut tiltak, omfang og om bruker har en aktivitetsplan.' },
  ])('validerer ubesvarte aktivitetsspoersmaal ved lagring: $type', async ({ type, melding }) => {
    const post = vi.spyOn(apiClient, 'post')
    await renderAktivitet(null)
    await act(async () => container.querySelector<HTMLInputElement>(`input[value="${type}"]`)!.click())

    await velgOgLagre('AVSLAG')

    expect(post).not.toHaveBeenCalled()
    expect(container.textContent).toContain(melding)
  })

  it('validerer ufullstendige tiltakssvar ved lagring', async () => {
    const aktivitet: ForlengetOmsAktivitet = {
      type: 'ARBEIDSRETTET_TILTAK',
      tiltak: ' ',
      omfang: 'DELTID',
      aktivitetsplan: JaNei.NEI,
    }
    const post = vi.spyOn(apiClient, 'post')
    await renderAktivitet(response(aktivitet, 'AVSLAG').vurdering)
    const lagre = Array.from(container.querySelectorAll('button')).find((button) => button.textContent === 'Lagre')!
    expect(lagre.disabled).toBe(false)

    await velgOgLagre('AVSLAG')

    expect(post).not.toHaveBeenCalled()
    expect(container.textContent).toContain('Fyll ut tiltak, omfang og om bruker har en aktivitetsplan.')
    expect(lagre.disabled).toBe(false)
  })

  it.each<ForlengetOmsAktivitet>([
    { type: 'ARBEIDSRETTET_TILTAK', tiltak: 'Tiltak', omfang: 'DELTID', aktivitetsplan: JaNei.NEI },
    { type: 'NEI' },
  ])('viser valideringsfeil uten aa deaktivere lagre ved manglende begrunnelse: $type', async (aktivitet) => {
    const post = vi.spyOn(apiClient, 'post')
    await renderAktivitet({ ...response(aktivitet, 'AVSLAG').vurdering, begrunnelse: ' ' })
    const lagre = Array.from(container.querySelectorAll('button')).find((button) => button.textContent === 'Lagre')!
    expect(lagre.disabled).toBe(false)
    await velgOgLagre('AVSLAG')

    expect(post).not.toHaveBeenCalled()
    expect(container.textContent).toContain('Oppgi en begrunnelse.')
  })

  it.each<ForlengetOmsAktivitet>([
    { type: 'NEI' },
    {
      type: 'UTDANNING',
      skole: 'Skole',
      studium: 'Studium',
      studererMinst50Prosent: JaNei.NEI,
    },
  ])('krever begrunnelse og viser backendfeil ved ugyldig innvilgelse: $type', async (aktivitet) => {
    const lagret = response(aktivitet, 'AVSLAG')
    const post = vi
      .spyOn(apiClient, 'post')
      .mockResolvedValueOnce({ ok: false, status: 400, detail: 'Ugyldig innvilgelse' })
      .mockResolvedValue({ ok: true, status: 200, data: lagret })
    store.dispatch(
      oppdaterRevurderingInfo({
        revurderingInfo: { type: 'FORLENGET_OMS', fom: '2027-01', tom: '2027-06' },
        begrunnelse: undefined,
      })
    )
    await renderAktivitet({ ...lagret.vurdering, begrunnelse: '' })
    await velgOgLagre('AVSLAG')
    expect(post).not.toHaveBeenCalled()

    const begrunnelse = container.querySelector('textarea')!
    for (const tekst of [' ', 'Begrunnelse']) {
      await act(async () => {
        Object.getOwnPropertyDescriptor(HTMLTextAreaElement.prototype, 'value')!.set!.call(begrunnelse, tekst)
        begrunnelse.dispatchEvent(new Event('input', { bubbles: true }))
      })
      const lagre = Array.from(container.querySelectorAll('button')).find((button) => button.textContent === 'Lagre')!
      expect(lagre.disabled).toBe(false)
    }
    await velgOgLagre('INNVILGELSE')
    expect(post).toHaveBeenCalledExactlyOnceWith('/behandling/behandling-id/forlenget-oms', {
      aktivitet,
      begrunnelse: 'Begrunnelse',
      utfall: 'INNVILGELSE',
      periode: { type: 'FORLENGET_OMS', fom: '2027-01', tom: '2027-06' },
    })
    expect(container.textContent).toContain('Kunne ikke lagre vurderingen av aktivitet: Ugyldig innvilgelse')
    expect(store.getState().behandlingReducer.behandling?.status).toBe(IBehandlingStatus.OPPRETTET)
    await velgOgLagre('AVSLAG')
    expect(post).toHaveBeenNthCalledWith(2, '/behandling/behandling-id/forlenget-oms', {
      aktivitet,
      begrunnelse: 'Begrunnelse',
      utfall: 'AVSLAG',
      periode: undefined,
    })
  })
})

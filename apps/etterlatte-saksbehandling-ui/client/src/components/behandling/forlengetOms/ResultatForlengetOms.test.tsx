// @vitest-environment jsdom

import { act, ReactNode } from 'react'
import { createRoot, Root } from 'react-dom/client'
import { Provider, useSelector } from 'react-redux'
import { MemoryRouter } from 'react-router'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { apiClient, ApiError } from '~shared/api/apiClient'
import {
  ForlengetOmsAktivitet,
  ForlengetOmsUtfall,
  ForlengetOmsVurdering,
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

  async function renderAktivitet(vurdering: ForlengetOmsVurdering | null) {
    const get = vi.spyOn(apiClient, 'get').mockResolvedValue({ ok: true, status: 200, data: vurdering })
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
    expect(lagre.disabled).toBe(true)

    await velgOgLagre('AVSLAG')

    expect(store.getState().behandlingReducer.behandling?.revurderinginfo).toBeNull()
    expect(container.textContent).toContain('Forlenget OMS avslås')
    expect(begrunnelse.readOnly).toBe(true)
    expect(neste.disabled).toBe(false)
  })

  it('henter lagrede svar og beholder dem naar vurderingen aapnes for redigering', async () => {
    const lagret = response({ type: 'NEI' }, 'AVSLAG')
    lagret.vilkaarsvurdering.resultat!.kommentar = 'Kommentar fra vilkår'
    const lagretBehandling: IBehandlingReducer = {
      ...behandling,
      status: IBehandlingStatus.VILKAARSVURDERT,
      vilkaarsvurdering: lagret.vilkaarsvurdering,
    }
    store.dispatch(setBehandling(lagretBehandling))
    const slett = vi.spyOn(apiClient, 'delete').mockResolvedValue({
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

    await act(async () => {
      fullfoer({ ok: false, status: 400, detail: 'Kunne ikke lagre' })
    })

    expect(aktivitetsvalg.disabled).toBe(false)
    expect(begrunnelse.readOnly).toBe(false)
    expect(container.querySelector<HTMLInputElement>('input[value="AVSLAG"]')?.disabled).toBe(false)
    expect(container.textContent).toContain('Kunne ikke lagre')
    expect(store.getState().behandlingReducer.behandling?.status).toBe(IBehandlingStatus.OPPRETTET)
  })

  it('krever periode for innvilgelse men ikke for avslag', async () => {
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
    expect(container.textContent).toContain('Må fylles ut')

    await velgOgLagre('AVSLAG')

    expect(post).toHaveBeenCalledExactlyOnceWith('/behandling/behandling-id/forlenget-oms', {
      aktivitet,
      begrunnelse: 'Begrunnelse',
      utfall: 'AVSLAG',
      periode: undefined,
    })
    expect(container.textContent).toContain('Forlenget OMS avslås')
  })

  it('nullstiller ulagrede svar ved aktivitetsbytte og gjenoppretter lagrede svar', async () => {
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
    expect(container.querySelector('textarea')!.value).toBe('Begrunnelse')
    expect(container.querySelector<HTMLInputElement>('input[value="AVSLAG"]')!.checked).toBe(true)

    await act(async () => container.querySelector<HTMLInputElement>('input[value="UTDANNING"]')!.click())
    expect(container.querySelector<HTMLInputElement>('input[type="text"]')!.value).toBe('Lagret skole')
    expect(container.querySelector('textarea')!.value).toBe('Begrunnelse')
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
    expect(container.querySelector('textarea')).toBeNull()
    const lagre = Array.from(container.querySelectorAll('button')).find((button) => button.textContent === 'Lagre')!
    expect(lagre.disabled).toBe(true)
  })

  it('krever ny periode naar en lagret til-maaned slettes', async () => {
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
    const tom = container.querySelector<HTMLInputElement>('#tom')!
    await act(async () => {
      Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value')!.set!.call(tom, '')
      tom.dispatchEvent(new Event('input', { bubbles: true }))
    })
    await velgOgLagre('INNVILGELSE')

    expect(post).not.toHaveBeenCalled()
    expect(container.textContent).toContain('Må fylles ut')
  })

  it('aktiverer lagre foerst naar utdanningssvarene og vilkaarene er komplette', async () => {
    await renderAktivitet(null)
    await act(async () => container.querySelector<HTMLInputElement>('input[value="UTDANNING"]')!.click())
    await act(async () => container.querySelector<HTMLInputElement>('input[value="AVSLAG"]')!.click())
    const lagre = Array.from(container.querySelectorAll('button')).find((button) => button.textContent === 'Lagre')!
    expect(lagre.disabled).toBe(true)

    const [skole, studium] = container.querySelectorAll<HTMLInputElement>('input[type="text"]')
    for (const felt of [skole, studium]) {
      await act(async () => {
        Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value')!.set!.call(felt, 'Svar')
        felt.dispatchEvent(new Event('input', { bubbles: true }))
      })
      expect(lagre.disabled).toBe(true)
    }
    await act(async () => container.querySelector<HTMLInputElement>('input[value="JA"]')!.click())
    expect(lagre.disabled).toBe(true)

    const begrunnelse = container.querySelector('textarea')!
    for (const tekst of [' ', 'Begrunnelse']) {
      await act(async () => {
        Object.getOwnPropertyDescriptor(HTMLTextAreaElement.prototype, 'value')!.set!.call(begrunnelse, tekst)
        begrunnelse.dispatchEvent(new Event('input', { bubbles: true }))
      })
      expect(lagre.disabled).toBe(!tekst.trim())
    }
    await act(async () => {
      store.dispatch(updateVilkaarsvurdering(undefined))
    })
    expect(lagre.disabled).toBe(true)
  })

  it.each<ForlengetOmsAktivitet>([
    { type: 'ARBEIDSRETTET_TILTAK', tiltak: ' ', omfang: 'DELTID', aktivitetsplan: JaNei.NEI },
    { type: 'ARBEIDSRETTET_TILTAK', tiltak: 'Tiltak', omfang: 'DELTID', aktivitetsplan: JaNei.NEI },
    { type: 'NEI' },
  ])('deaktiverer lagre ved ufullstendige svar: $type', async (aktivitet) => {
    const begrunnelse = aktivitet.type === 'ARBEIDSRETTET_TILTAK' && !aktivitet.tiltak.trim() ? 'Begrunnelse' : ' '
    await renderAktivitet({ ...response(aktivitet, 'AVSLAG').vurdering, begrunnelse })
    const lagre = Array.from(container.querySelectorAll('button')).find((button) => button.textContent === 'Lagre')!
    expect(lagre.disabled).toBe(true)
  })

  it('krever begrunnelse og tillater bare avslag naar bruker studerer mindre enn 50 prosent', async () => {
    const aktivitet: ForlengetOmsAktivitet = {
      type: 'UTDANNING',
      skole: 'Skole',
      studium: 'Studium',
      studererMinst50Prosent: JaNei.NEI,
    }
    const lagret = response(aktivitet, 'AVSLAG')
    const post = vi.spyOn(apiClient, 'post').mockResolvedValue({
      ok: true,
      status: 200,
      data: lagret,
    })
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
      expect(lagre.disabled).toBe(!tekst.trim())
    }
    await velgOgLagre('INNVILGELSE')
    expect(post).not.toHaveBeenCalled()
    await velgOgLagre('AVSLAG')
    expect(post).toHaveBeenCalledExactlyOnceWith('/behandling/behandling-id/forlenget-oms', {
      aktivitet,
      begrunnelse: 'Begrunnelse',
      utfall: 'AVSLAG',
      periode: undefined,
    })
  })
})

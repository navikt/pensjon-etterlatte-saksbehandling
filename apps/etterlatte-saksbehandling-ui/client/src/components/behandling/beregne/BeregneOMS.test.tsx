// @vitest-environment jsdom

import React, { act } from 'react'
import { createRoot, Root } from 'react-dom/client'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { BeregneOMS } from './BeregneOMS'

const mocks = vi.hoisted(() => ({
  hentManglendeInntektsaar: vi.fn(),
  upsertVedtak: vi.fn(),
  next: vi.fn(),
  behandling: {
    id: 'behandling-id',
    status: 'AVKORTET',
    sendeBrev: true,
    vilkaarsvurdering: { resultat: { utfall: 'OPPFYLT' } },
  },
}))

vi.mock('~shared/api/avkorting', () => ({ hentManglendeInntektsaar: mocks.hentManglendeInntektsaar }))
vi.mock('~shared/api/vedtaksvurdering', () => ({ upsertVedtak: mocks.upsertVedtak, fattVedtak: vi.fn() }))
vi.mock('~shared/api/beregning', () => ({
  hentBeregning: vi.fn().mockResolvedValue({ ok: true, data: {}, status: 200 }),
}))
vi.mock('~components/behandling/useBehandling', () => ({ useBehandling: () => mocks.behandling }))
vi.mock('~store/Store', () => ({
  useAppDispatch: () => vi.fn(),
  useAppSelector: () => ({ brevutfall: 'INNVILGELSE' }),
}))
vi.mock('../BehandlingRoutes', async () => {
  const { createContext } = await import('react')
  return { BehandlingRouteContext: createContext({ next: mocks.next }) }
})
vi.mock('../felles/utils', () => ({
  erBehandlingRedigerbar: () => true,
  ogSeparertListe: (aar: number[]) => aar.join(', '),
}))
vi.mock('../useInnloggetSaksbehandler', () => ({
  useInnloggetSaksbehandler: () => ({ skriveEnheter: [] }),
}))
vi.mock('~components/behandling/handlinger/BehandlingHandlingKnapper', () => ({
  BehandlingHandlingKnapper: ({ children }: { children: React.ReactNode }) => <>{children}</>,
}))
vi.mock('~components/behandling/beregne/OmstillingsstoenadSammendrag', () => ({
  OmstillingsstoenadSammendrag: () => null,
}))
vi.mock('~components/behandling/avkorting/Avkorting', () => ({ Avkorting: () => null }))
vi.mock('~components/behandling/beregne/SimulerUtbetaling', () => ({ SimulerUtbetaling: () => null }))
vi.mock('~components/behandling/brevutfall/Brevutfall', () => ({ Brevutfall: () => null }))
vi.mock('~components/behandling/handlinger/SendTilAttesteringModal', () => ({
  SendTilAttesteringModal: () => <span>Send til attestering</span>,
}))
vi.mock('../handlinger/NesteOgTilbake', () => ({ NesteOgTilbake: () => null }))

describe('inntektskontroll foer vedtak i OMS', () => {
  let root: Root
  let container: HTMLDivElement

  beforeEach(() => {
    vi.stubGlobal('IS_REACT_ACT_ENVIRONMENT', true)
    vi.clearAllMocks()
    mocks.behandling.sendeBrev = true
    mocks.behandling.vilkaarsvurdering.resultat.utfall = 'OPPFYLT'
    mocks.upsertVedtak.mockResolvedValue({ ok: true, data: {}, status: 200 })
    container = document.createElement('div')
    document.body.appendChild(container)
    root = createRoot(container)
  })
  afterEach(async () => {
    await act(async () => root.unmount())
    container.remove()
    vi.unstubAllGlobals()
  })

  const render = async () => {
    await act(async () => root.render(<BeregneOMS />))
  }

  const klikkKnapp = async (navn: string) => {
    const knapp = Array.from(container.querySelectorAll('button')).find((knapp) => knapp.textContent === navn)
    expect(knapp).toBeDefined()
    await act(async () => knapp!.click())
  }

  it('stopper Neste naar inntekten for 2025 maa registreres paa nytt', async () => {
    mocks.hentManglendeInntektsaar.mockResolvedValue({ ok: true, data: [2025], status: 200 })
    await render()

    await klikkKnapp('Neste side')

    expect(container.textContent).toContain('Du må registrere forventet inntekt for 2025')
    expect(mocks.upsertVedtak).not.toHaveBeenCalled()
    expect(mocks.next).not.toHaveBeenCalled()

    mocks.hentManglendeInntektsaar.mockResolvedValue({ ok: true, data: [], status: 200 })
    await klikkKnapp('Neste side')

    expect(mocks.next).toHaveBeenCalledOnce()
    expect(mocks.upsertVedtak).toHaveBeenCalledWith('behandling-id')
    expect(mocks.hentManglendeInntektsaar).toHaveBeenCalledTimes(2)
  })

  it('stopper Fatt vedtak naar inntekten mangler og brev ikke skal sendes', async () => {
    mocks.behandling.sendeBrev = false
    mocks.hentManglendeInntektsaar.mockResolvedValue({ ok: true, data: [2025], status: 200 })
    await render()

    await klikkKnapp('Fatt vedtak')

    expect(container.textContent).toContain('Du må registrere forventet inntekt for 2025')
    expect(mocks.upsertVedtak).not.toHaveBeenCalled()
    expect(container.textContent).not.toContain('Send til attestering')
  })

  it('stopper naar kontrollen av inntektsaar feiler', async () => {
    mocks.hentManglendeInntektsaar.mockResolvedValue({ ok: false, detail: 'Feil ved henting', status: 500 })
    await render()

    await klikkKnapp('Neste side')

    expect(container.textContent).toContain('Kunne ikke kontrollere påkrevde inntektsår')
    expect(mocks.upsertVedtak).not.toHaveBeenCalled()
    expect(mocks.next).not.toHaveBeenCalled()
  })

  it('krever ikke ny inntekt ved rent opphoer', async () => {
    mocks.behandling.vilkaarsvurdering.resultat.utfall = 'IKKE_OPPFYLT'
    await render()

    await klikkKnapp('Neste side')

    expect(mocks.next).toHaveBeenCalledOnce()
    expect(mocks.hentManglendeInntektsaar).not.toHaveBeenCalled()
  })
})

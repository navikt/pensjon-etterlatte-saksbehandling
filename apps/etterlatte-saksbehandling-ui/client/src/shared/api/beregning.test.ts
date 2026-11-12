import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import {
  deaktiverOverstyrtBeregning,
  hentBeregning,
  hentBeregningsGrunnlag,
  hentGjeldendeGrunnbeloep,
  hentOverstyrBeregning,
  hentOverstyrBeregningGrunnlag,
  lagreBeregningsGrunnlag,
  lagreOverstyrBeregningGrunnlag,
  opprettEllerEndreBeregning,
  opprettOverstyrBeregning,
} from '~shared/api/beregning'
import {
  avkortingSkalHaToInntekter,
  hentAvkorting,
  hentManglendeInntektsaar,
  lagreAvkortingGrunnlag,
  lagreInntektListe,
} from '~shared/api/avkorting'
import {
  hentSanksjon,
  hentSanksjonerForSisteIverksatteBehandling,
  lagreSanksjon,
  slettSanksjon,
} from '~shared/api/sanksjon'
import { beregningConfig } from '~clientConfig'
import { OverstyrtBeregningKategori } from '~shared/types/OverstyrtBeregning'
import { ISanksjonLagre, SanksjonType } from '~shared/types/sanksjon'

vi.mock('~utils/logger', () => ({ logger: { generalWarning: vi.fn() } }))

const behandlingId = 'behandling-id'
const sanksjonId = 'sanksjon-id'
const inntekt = { inntektTom: 100_000, spesifikasjon: 'Forventet inntekt' }
const overstyring = { beskrivelse: 'Overstyring', kategori: OverstyrtBeregningKategori.UKJENT_AVDOED }
const sanksjon: ISanksjonLagre = {
  sakId: 123,
  type: SanksjonType.STANS,
  fom: '2026-01',
  beskrivelse: 'Stans',
}

const kall = [
  { navn: 'hentBeregning', kall: () => hentBeregning(behandlingId), sti: `/${behandlingId}`, metode: 'GET' },
  {
    navn: 'opprettEllerEndreBeregning',
    kall: () => opprettEllerEndreBeregning(behandlingId),
    sti: `/${behandlingId}`,
    metode: 'POST',
    body: {},
  },
  {
    navn: 'hentBeregningsGrunnlag',
    kall: () => hentBeregningsGrunnlag(behandlingId),
    sti: `/beregningsgrunnlag/${behandlingId}`,
    metode: 'GET',
  },
  {
    navn: 'lagreBeregningsGrunnlag',
    kall: () => lagreBeregningsGrunnlag({ behandlingId, grunnlag: undefined }),
    sti: `/beregningsgrunnlag/${behandlingId}`,
    metode: 'POST',
    body: {},
  },
  {
    navn: 'hentOverstyrBeregning',
    kall: () => hentOverstyrBeregning(behandlingId),
    sti: `/${behandlingId}/overstyrt`,
    metode: 'GET',
  },
  {
    navn: 'opprettOverstyrBeregning',
    kall: () => opprettOverstyrBeregning({ behandlingId, ...overstyring }),
    sti: `/${behandlingId}/overstyrt`,
    metode: 'POST',
    body: overstyring,
  },
  {
    navn: 'deaktiverOverstyrtBeregning',
    kall: () => deaktiverOverstyrtBeregning(behandlingId),
    sti: `/${behandlingId}/overstyrt`,
    metode: 'DELETE',
  },
  {
    navn: 'hentOverstyrBeregningGrunnlag',
    kall: () => hentOverstyrBeregningGrunnlag(behandlingId),
    sti: `/beregningsgrunnlag/${behandlingId}/overstyr`,
    metode: 'GET',
  },
  {
    navn: 'lagreOverstyrBeregningGrunnlag',
    kall: () => lagreOverstyrBeregningGrunnlag({ behandlingId, grunnlag: { perioder: [] } }),
    sti: `/beregningsgrunnlag/${behandlingId}/overstyr`,
    metode: 'POST',
    body: { perioder: [] },
  },
  { navn: 'hentGjeldendeGrunnbeloep', kall: hentGjeldendeGrunnbeloep, sti: '/grunnbeloep', metode: 'GET' },
  {
    navn: 'hentAvkorting',
    kall: () => hentAvkorting(behandlingId),
    sti: `/avkorting/${behandlingId}`,
    metode: 'GET',
  },
  {
    navn: 'avkortingSkalHaToInntekter',
    kall: () => avkortingSkalHaToInntekter(behandlingId),
    sti: `/avkorting/${behandlingId}/skalHaInntektNesteAar`,
    metode: 'GET',
  },
  {
    navn: 'lagreAvkortingGrunnlag',
    kall: () => lagreAvkortingGrunnlag({ behandlingId, avkortingGrunnlag: inntekt }),
    sti: `/avkorting/${behandlingId}`,
    metode: 'POST',
    body: inntekt,
  },
  {
    navn: 'lagreInntektListe',
    kall: () => lagreInntektListe({ behandlingId, inntekter: [inntekt] }),
    sti: `/avkorting/${behandlingId}/liste`,
    metode: 'POST',
    body: { inntekter: [inntekt] },
  },
  {
    navn: 'hentManglendeInntektsaar',
    kall: () => hentManglendeInntektsaar(behandlingId),
    sti: `/avkorting/${behandlingId}/manglende-inntektsaar`,
    metode: 'GET',
  },
  { navn: 'hentSanksjon', kall: () => hentSanksjon(behandlingId), sti: `/sanksjon/${behandlingId}`, metode: 'GET' },
  {
    navn: 'lagreSanksjon',
    kall: () => lagreSanksjon({ behandlingId, sanksjon }),
    sti: `/sanksjon/${behandlingId}`,
    metode: 'POST',
    body: sanksjon,
  },
  {
    navn: 'slettSanksjon',
    kall: () => slettSanksjon({ behandlingId, sanksjonId }),
    sti: `/sanksjon/${behandlingId}/${sanksjonId}`,
    metode: 'DELETE',
  },
]

const standardBrukBehandling = beregningConfig.brukBehandling

describe('ruting av beregningskall', () => {
  const fetchMock = vi.fn<typeof fetch>()

  beforeEach(() => {
    fetchMock.mockReset()
    fetchMock.mockImplementation(async () => Response.json({}))
    vi.stubGlobal('fetch', fetchMock)
    beregningConfig.brukBehandling = false
  })

  afterEach(() => {
    beregningConfig.brukBehandling = standardBrukBehandling
    vi.unstubAllGlobals()
  })

  it('bruker beregningsappen som standard', () => {
    expect(standardBrukBehandling).toBe(false)
  })

  describe.each([
    { brukBehandling: false, prefiks: '/api/beregning' },
    { brukBehandling: true, prefiks: '/api/beregning_v2' },
  ])('brukBehandling=$brukBehandling', ({ brukBehandling, prefiks }) => {
    it.each(kall)('$navn beholder sti, metode og body', async ({ kall, sti, metode, body }) => {
      beregningConfig.brukBehandling = brukBehandling
      const resultat = await kall()

      expect(resultat.ok).toBe(true)
      expect(fetchMock).toHaveBeenCalledExactlyOnceWith(`${prefiks}${sti}`, {
        method: metode,
        headers: { 'Content-Type': 'application/json' },
        body: body ? JSON.stringify(body) : undefined,
      })
    })

    it('endrer ikke kall som allerede går til behandling', async () => {
      beregningConfig.brukBehandling = brukBehandling
      await hentSanksjonerForSisteIverksatteBehandling(123)

      expect(fetchMock).toHaveBeenCalledWith(
        '/api/sak/123/behandlinger/sisteIverksatte/sanksjoner',
        expect.objectContaining({ method: 'GET' })
      )
    })
  })

  it('velger backend fra konfigurasjonen ved hvert kall', async () => {
    await hentBeregning(behandlingId)
    beregningConfig.brukBehandling = true
    await hentBeregning(behandlingId)
    beregningConfig.brukBehandling = false
    await hentBeregning(behandlingId)

    expect(fetchMock.mock.calls.map(([url]) => url)).toEqual([
      `/api/beregning/${behandlingId}`,
      `/api/beregning_v2/${behandlingId}`,
      `/api/beregning/${behandlingId}`,
    ])
  })
})

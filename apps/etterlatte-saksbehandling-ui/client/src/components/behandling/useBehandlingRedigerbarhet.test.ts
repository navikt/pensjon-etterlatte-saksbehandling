import { describe, expect, it } from 'vitest'
import { IBehandlingStatus, ManglerSkrivetilgangAarsak, Skrivetilgang } from '~shared/types/IDetaljertBehandling'
import { STATUS, vurderRedigerbarhet } from '~components/behandling/useBehandlingRedigerbarhet'
import { forklaringSkrivebeskyttet } from '~components/behandling/felles/SkrivebeskyttetVarsel'

const SAKENS_ENHET = '4817'
const ANNEN_ENHET = '4808'
const alleStatuser = Object.values(IBehandlingStatus)

/**
 * Frosset kopi av erBehandlingRedigerbar slik den var før fase A (utils.ts, kodeversjon 73fcf0615).
 * Brukes som fasit for at ny regel gir samme svar.
 */
const gammelErBehandlingRedigerbar = (status: IBehandlingStatus, enhetId: string, enheter: string[]): boolean =>
  enheter.includes(enhetId) &&
  [
    IBehandlingStatus.OPPRETTET,
    IBehandlingStatus.VILKAARSVURDERT,
    IBehandlingStatus.TRYGDETID_OPPDATERT,
    IBehandlingStatus.BEREGNET,
    IBehandlingStatus.AVKORTET,
    IBehandlingStatus.RETURNERT,
  ].includes(status)

type Rolle = 'SAKSBEHANDLER' | 'ATTESTANT' | 'LES' | 'INGEN'

/**
 * Syntetisk modell av backend: skriveEnheter = hentEnheterMedSkrivetilgang() og
 * skrivetilgang = vurderSkrivetilgang(). Backend-testen SkrivetilgangTest viser at
 * harSkrivetilgang er lik hentEnheterMedSkrivetilgang().contains(sakens enhet).
 */
const syntetiskBruker = (rolle: Rolle, brukersEnhet: string) => {
  const harSkriverolle = rolle === 'SAKSBEHANDLER' || rolle === 'ATTESTANT'
  const skriveEnheter = harSkriverolle ? [brukersEnhet] : []
  const skrivetilgang: Skrivetilgang = skriveEnheter.includes(SAKENS_ENHET)
    ? { harSkrivetilgang: true, aarsak: null }
    : {
        harSkrivetilgang: false,
        aarsak: harSkriverolle
          ? ManglerSkrivetilgangAarsak.ENHET_UTEN_SKRIVETILGANG
          : ManglerSkrivetilgangAarsak.MANGLER_ROLLE,
      }
  return { skriveEnheter, skrivetilgang }
}

describe('ny regel gir samme svar som erBehandlingRedigerbar (status × enhet × rolle)', () => {
  const roller: Rolle[] = ['SAKSBEHANDLER', 'ATTESTANT', 'LES', 'INGEN']
  const enheter = [SAKENS_ENHET, ANNEN_ENHET]

  alleStatuser.forEach((status) =>
    roller.forEach((rolle) =>
      enheter.forEach((brukersEnhet) => {
        it(`${status} / ${rolle} / enhet ${brukersEnhet}`, () => {
          const { skriveEnheter, skrivetilgang } = syntetiskBruker(rolle, brukersEnhet)

          const gammel = gammelErBehandlingRedigerbar(status, SAKENS_ENHET, skriveEnheter)
          const ny = vurderRedigerbarhet({ status, skrivetilgang }).redigerbar

          expect(ny).toBe(gammel)
        })
      })
    )
  )
})

describe('vurderRedigerbarhet gir riktig årsak', () => {
  const harSkrivetilgang: Skrivetilgang = { harSkrivetilgang: true, aarsak: null }

  it('redigerbar når status kan endres og saksbehandler har skrivetilgang', () => {
    expect(vurderRedigerbarhet({ status: IBehandlingStatus.OPPRETTET, skrivetilgang: harSkrivetilgang })).toEqual({
      redigerbar: true,
      aarsak: null,
    })
  })

  it('status har forrang når både status og tilgang hindrer endring', () => {
    expect(
      vurderRedigerbarhet({
        status: IBehandlingStatus.FATTET_VEDTAK,
        skrivetilgang: { harSkrivetilgang: false, aarsak: ManglerSkrivetilgangAarsak.MANGLER_ROLLE },
      })
    ).toEqual({ redigerbar: false, aarsak: STATUS })
  })

  it.each([ManglerSkrivetilgangAarsak.ENHET_UTEN_SKRIVETILGANG, ManglerSkrivetilgangAarsak.MANGLER_ROLLE])(
    'bruker årsaken fra backend: %s',
    (aarsak) => {
      expect(
        vurderRedigerbarhet({ status: IBehandlingStatus.BEREGNET, skrivetilgang: { harSkrivetilgang: false, aarsak } })
      ).toEqual({ redigerbar: false, aarsak })
    }
  )

  it('ikke redigerbar uten behandling', () => {
    expect(vurderRedigerbarhet(null)).toEqual({ redigerbar: false, aarsak: null })
  })
})

describe('forklaringSkrivebeskyttet', () => {
  const behandling = (status: IBehandlingStatus) => ({ status, sakEnhetId: SAKENS_ENHET })

  it('forklarer at behandlingen er sendt til attestering', () => {
    expect(forklaringSkrivebeskyttet(STATUS, behandling(IBehandlingStatus.FATTET_VEDTAK))).toBe(
      'Behandlingen er sendt til attestering.'
    )
  })

  it('forklarer at behandlingen er avbrutt', () => {
    expect(forklaringSkrivebeskyttet(STATUS, behandling(IBehandlingStatus.AVBRUTT))).toBe('Behandlingen er avbrutt.')
  })

  it('forklarer at behandlingen er ferdig behandlet', () => {
    expect(forklaringSkrivebeskyttet(STATUS, behandling(IBehandlingStatus.IVERKSATT))).toBe(
      'Behandlingen er ferdig behandlet.'
    )
  })

  it('forklarer manglende skrivetilgang til sakens enhet', () => {
    expect(
      forklaringSkrivebeskyttet(
        ManglerSkrivetilgangAarsak.ENHET_UTEN_SKRIVETILGANG,
        behandling(IBehandlingStatus.OPPRETTET)
      )
    ).toBe('Saken tilhører enhet Steinkjer - 4817, som du ikke har skrivetilgang til.')
  })

  it('forklarer at saksbehandler bare har lesetilgang', () => {
    expect(
      forklaringSkrivebeskyttet(ManglerSkrivetilgangAarsak.MANGLER_ROLLE, behandling(IBehandlingStatus.OPPRETTET))
    ).toContain('Du har bare lesetilgang i Gjenny')
  })
})

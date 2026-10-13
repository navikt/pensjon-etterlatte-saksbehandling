import { describe, expect, it } from 'vitest'
import { enhetErSkrivbar, statusErRedigerbar } from '~components/behandling/felles/utils'
import { IBehandlingStatus } from '~shared/types/IDetaljertBehandling'
import paritet from './paritet/behandlingStatusKanEndres.json'

const alleStatuser = Object.values(IBehandlingStatus)
const redigerbareStatuser = [
  IBehandlingStatus.OPPRETTET,
  IBehandlingStatus.VILKAARSVURDERT,
  IBehandlingStatus.TRYGDETID_OPPDATERT,
  IBehandlingStatus.BEREGNET,
  IBehandlingStatus.AVKORTET,
  IBehandlingStatus.RETURNERT,
]

const SAKENS_ENHET = '4817'
const ANNEN_ENHET = '4808'

describe('paritet med backend (BehandlingStatus.kanEndres)', () => {
  it('frontend kjenner de samme statusene som backend', () => {
    expect([...alleStatuser].sort()).toEqual([...paritet.alleStatuser].sort())
  })

  it('statusErRedigerbar gir samme statuser som BehandlingStatus.kanEndres()', () => {
    expect(alleStatuser.filter(statusErRedigerbar).sort()).toEqual([...paritet.kanEndres].sort())
  })
})

describe('statusErRedigerbar', () => {
  it.each(alleStatuser)('%s', (status) => {
    expect(statusErRedigerbar(status)).toBe(redigerbareStatuser.includes(status))
  })
})

describe('enhetErSkrivbar', () => {
  it('er skrivbar når sakens enhet er blant skriveenhetene', () => {
    expect(enhetErSkrivbar(SAKENS_ENHET, [ANNEN_ENHET, SAKENS_ENHET])).toBe(true)
  })

  it('er ikke skrivbar når sakens enhet ikke er blant skriveenhetene', () => {
    expect(enhetErSkrivbar(SAKENS_ENHET, [ANNEN_ENHET])).toBe(false)
  })

  it('er ikke skrivbar uten skriveenheter (f.eks. bare lesetilgang)', () => {
    expect(enhetErSkrivbar(SAKENS_ENHET, [])).toBe(false)
  })
})

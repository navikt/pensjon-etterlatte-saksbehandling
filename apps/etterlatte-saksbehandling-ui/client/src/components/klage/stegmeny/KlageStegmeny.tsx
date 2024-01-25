import { StegMenyWrapper } from '~components/behandling/StegMeny/stegmeny'
import React from 'react'
import { KlageNavLenke } from '~components/klage/stegmeny/KlageNavLenke'
import { useKlage } from '~components/klage/useKlage'
import { Klage, KlageStatus } from '~shared/types/Klage'
import { JaNei } from '~shared/types/ISvar'

export function kanVurdereUtfall(klage: Klage | null): boolean {
  const klageStatus = klage?.status

  switch (klageStatus) {
    case KlageStatus.UTFALL_VURDERT:
    case KlageStatus.FORMKRAV_OPPFYLT:
      return true
    case KlageStatus.FERDIGSTILT:
      return klage?.formkrav?.formkrav.erFormkraveneOppfylt === JaNei.JA
  }
  return false
}

export function kanSeOppsummering(klage: Klage | null): boolean {
  const klageStatus = klage?.status
  switch (klageStatus) {
    case KlageStatus.FERDIGSTILT:
    case KlageStatus.UTFALL_VURDERT:
    case KlageStatus.FORMKRAV_IKKE_OPPFYLT:
      return true
  }
  return false
}

export function kanSeBrev(klage: Klage | null): boolean {
  const utfall = klage?.utfall?.utfall
  switch (utfall) {
    case 'DELVIS_OMGJOERING':
    case 'STADFESTE_VEDTAK':
      return true
  }
  return false
}

export function KlageStegmeny() {
  const klage = useKlage()

  return (
    <StegMenyWrapper>
      <KlageNavLenke path="formkrav" description="Vurder formkrav" enabled={true} />
      <KlageNavLenke path="vurdering" description="Vurder klagen" enabled={kanVurdereUtfall(klage)} />
      <KlageNavLenke path="brev" description="Brev" enabled={kanSeBrev(klage)} />
      <KlageNavLenke path="oppsummering" description="Oppsummering" enabled={kanSeOppsummering(klage)} />
    </StegMenyWrapper>
  )
}

export function nesteSteg(klage: Klage, aktivSide: 'formkrav' | 'vurdering' | 'brev' | 'oppsummering'): string {
  if (aktivSide === 'formkrav') {
    return `/klage/${klage.id}/vurdering`
  }
  if (aktivSide === 'vurdering') {
    return kanSeBrev(klage) ? `/klage/${klage.id}/brev` : `/klage/${klage.id}/oppsummering`
  }
  if (aktivSide === 'brev') {
    return `/klage/${klage.id}/oppsummering`
  }
  return `/klage/${klage.id}`
}

export function forrigeSteg(klage: Klage, aktivSide: 'formkrav' | 'vurdering' | 'brev' | 'oppsummering'): string {
  if (aktivSide === 'vurdering') {
    return `/klage/${klage.id}/formkrav`
  }
  if (aktivSide === 'brev') {
    return `/klage/${klage.id}/vurdering`
  }
  if (aktivSide === 'oppsummering') {
    return kanSeBrev(klage) ? `/klage/${klage.id}/brev` : `/klage/${klage.id}/vurdering`
  }
  return `/klage/${klage.id}`
}

import { StegMenyBox } from '~components/behandling/stegmeny/StegMeny'
import React from 'react'
import { KlageNavLenke } from '~components/klage/stegmeny/KlageNavLenke'
import { useKlage } from '~components/klage/useKlage'
import { Klage, KlageStatus, Utfall } from '~shared/types/Klage'
import { JaNei } from '~shared/types/ISvar'
import { HStack } from '@navikt/ds-react'

export function kanVurdereUtfallUtenKontaktMedKlager(klage: Klage | null): boolean {
  return (
    klage?.formkrav?.formkrav.erFormkraveneOppfylt === JaNei.JA ||
    klage?.formkrav?.formkrav.erKlagenFramsattInnenFrist === JaNei.NEI
  )
}

export function kanVurdereUtfall(klage: Klage | null): boolean {
  return kanVurdereUtfallUtenKontaktMedKlager(klage) || !!klage?.formkrav?.klagerHarIkkeSvartVurdering
}

export function kanSeOppsummering(klage: Klage | null): boolean {
  const klageStatus = klage?.status
  switch (klageStatus) {
    case KlageStatus.FERDIGSTILT:
    case KlageStatus.UTFALL_VURDERT:
    case KlageStatus.FORMKRAV_OPPFYLT:
    case KlageStatus.FATTET_VEDTAK:
    case KlageStatus.RETURNERT:
      return true
    case KlageStatus.OPPRETTET:
    case KlageStatus.AVBRUTT:
    case KlageStatus.FORMKRAV_IKKE_OPPFYLT:
    case undefined:
      return false
  }
}

export function klageKanSeBrev(klage: Klage | null): boolean {
  const utfall = klage?.utfall?.utfall
  if (!utfall) return false

  return kanSeBrev(utfall as Utfall)
}

export function kanSeBrev(valgtUtfall: Utfall | null) {
  switch (valgtUtfall) {
    case 'DELVIS_OMGJOERING':
    case 'STADFESTE_VEDTAK':
    case 'AVVIST':
      return true
  }
  return false
}

export function KlageStegmeny() {
  const klage = useKlage()

  return (
    <StegMenyBox>
      <HStack gap="space-6" align="center">
        <KlageNavLenke path="formkrav" description="Vurder formkrav" enabled={true} />
        <KlageNavLenke path="vurdering" description="Vurder klagen" enabled={kanVurdereUtfall(klage)} />
        <KlageNavLenke path="brev" description="Brev" enabled={klageKanSeBrev(klage)} />
        <KlageNavLenke
          path="oppsummering"
          description="Oppsummering"
          enabled={kanSeOppsummering(klage)}
          erSisteRoute={true}
        />
      </HStack>
    </StegMenyBox>
  )
}

export function nesteSteg(klage: Klage, aktivSide: 'formkrav' | 'vurdering' | 'brev' | 'oppsummering'): string {
  if (aktivSide === 'formkrav') {
    return `/klage/${klage.id}/vurdering`
  }
  if (aktivSide === 'vurdering') {
    return klageKanSeBrev(klage) ? `/klage/${klage.id}/brev` : `/klage/${klage.id}/oppsummering`
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
    return klageKanSeBrev(klage) ? `/klage/${klage.id}/brev` : `/klage/${klage.id}/vurdering`
  }
  return `/klage/${klage.id}`
}

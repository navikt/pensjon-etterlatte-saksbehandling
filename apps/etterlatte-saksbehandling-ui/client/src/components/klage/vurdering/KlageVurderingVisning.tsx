import { useKlage } from '~components/klage/useKlage'
import { Content } from '~shared/styled'
import React from 'react'

export function KlageVurderingVisning() {
  const klage = useKlage()

  if (!klage) return

  return <Content></Content>
}

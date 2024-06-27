import { useBehandling } from '~components/behandling/useBehandling'
import { useTilbakekreving } from '~components/tilbakekreving/useTilbakekreving'
import React from 'react'
import { AttesterYtelse } from '~components/behandling/attestering/handinger/attesterYtelse'
import { AttesterTilbakekreving } from '~components/behandling/attestering/handinger/attesterTilbakekreving'
import { useKlage } from '~components/klage/useKlage'
import { AttesterKlage } from '~components/behandling/attestering/handinger/attesterKlage'

export const AttesterVedtak = ({ kommentar }: { kommentar: string }) => {
  const behandling = useBehandling()
  const tilbakekreving = useTilbakekreving()
  const klage = useKlage()

  if (behandling) {
    return <AttesterYtelse behandling={behandling} kommentar={kommentar} />
  } else if (tilbakekreving) {
    return <AttesterTilbakekreving tilbakekreving={tilbakekreving} kommentar={kommentar} />
  } else if (klage) {
    return <AttesterKlage klage={klage} kommentar={kommentar} />
  } else {
    throw Error('Mangler behandling')
  }
}

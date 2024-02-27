import { useBehandling } from '~components/behandling/useBehandling'
import { useTilbakekreving } from '~components/tilbakekreving/useTilbakekreving'
import React from 'react'
import { UnderkjennYtelse } from '~components/behandling/attestering/handinger/underkjennYtelse'
import { UnderkjennTilbakekreving } from '~components/behandling/attestering/handinger/underkjennTilbakekreving'
import { useKlage } from '~components/klage/useKlage'
import { UnderkjennKlage } from '~components/behandling/attestering/handinger/underkjennKlage'

export const UnderkjennVedtak = ({ kommentar, valgtBegrunnelse }: { kommentar: string; valgtBegrunnelse: string }) => {
  const behandling = useBehandling()
  const tilbakekreving = useTilbakekreving()
  const klage = useKlage()

  if (behandling) {
    return <UnderkjennYtelse behandlingId={behandling.id} kommentar={kommentar} valgtBegrunnelse={valgtBegrunnelse} />
  } else if (tilbakekreving) {
    return (
      <UnderkjennTilbakekreving
        tilbakekreving={tilbakekreving}
        kommentar={kommentar}
        valgtBegrunnelse={valgtBegrunnelse}
      />
    )
  } else if (klage) {
    return <UnderkjennKlage klage={klage} kommentar={kommentar} valgtBegrunnelse={valgtBegrunnelse} />
  } else {
    throw Error('Mangler behandling')
  }
}

import { useBehandling } from '~components/behandling/useBehandling'
import { useTilbakekreving } from '~components/tilbakekreving/useTilbakekreving'
import React from 'react'
import { UnderkjennYtelse } from '~components/behandling/attestering/handinger/underkjennYtelse'
import { UnderkjennTilbakekreving } from '~components/behandling/attestering/handinger/underkjennTilbakekreving'

export const UnderkjennVedtak = ({ kommentar, valgtBegrunnelse }: { kommentar: string; valgtBegrunnelse: string }) => {
  const behandling = useBehandling()
  const tilbakekreving = useTilbakekreving()

  if (behandling) {
    return <UnderkjennYtelse behandling={behandling} kommentar={kommentar} valgtBegrunnelse={valgtBegrunnelse} />
  } else if (tilbakekreving) {
    return (
      <UnderkjennTilbakekreving
        tilbakekreving={tilbakekreving}
        kommentar={kommentar}
        valgtBegrunnelse={valgtBegrunnelse}
      />
    )
  } else {
    throw Error('Mangler behandling')
  }
}

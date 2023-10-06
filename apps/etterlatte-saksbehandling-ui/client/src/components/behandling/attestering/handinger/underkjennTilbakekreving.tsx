import { Button } from '@navikt/ds-react'
import { useState } from 'react'
import { GeneriskModal } from '~shared/modal/modal'
import { useNavigate } from 'react-router'
import { isPending, useApiCall } from '~shared/hooks/useApiCall'
import { Tilbakekreving } from '~shared/types/Tilbakekreving'
import { underkjennVedtak } from '~shared/api/tilbakekreving'

type Props = {
  tilbakekreving: Tilbakekreving
  kommentar: string
  valgtBegrunnelse: string
}

export const UnderkjennTilbakekreving: React.FC<Props> = ({ tilbakekreving, kommentar, valgtBegrunnelse }) => {
  const [modalisOpen, setModalisOpen] = useState(false)
  const navigate = useNavigate()

  const [underkjennStatus, apiUnderkjennVedtak] = useApiCall(underkjennVedtak)

  const underkjenn = () => {
    if (!tilbakekreving.id) throw new Error('Mangler tilbakekrevignsid')
    apiUnderkjennVedtak({ tilbakekrevingId: tilbakekreving.id, kommentar, valgtBegrunnelse }, () => {
      navigate(`/person/${tilbakekreving.sak.ident}`)
    })
  }

  return (
    <>
      <Button variant="primary" onClick={() => setModalisOpen(true)}>
        Bekreft og send i retur
      </Button>

      <GeneriskModal
        tittel="Er du sikker på at vil underkjenne vedtak og sende i retur til saksbehandler?"
        tekstKnappJa="Ja, send i retur"
        tekstKnappNei=" Nei, gå tilbake"
        onYesClick={underkjenn}
        setModalisOpen={setModalisOpen}
        open={modalisOpen}
        loading={isPending(underkjennStatus)}
      />
    </>
  )
}

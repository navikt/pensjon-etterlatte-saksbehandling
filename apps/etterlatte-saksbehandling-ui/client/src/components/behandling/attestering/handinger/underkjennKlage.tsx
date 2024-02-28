import { Button } from '@navikt/ds-react'
import { useState } from 'react'
import { GeneriskModal } from '~shared/modal/modal'
import { useNavigate } from 'react-router'
import { useApiCall } from '~shared/hooks/useApiCall'
import { Klage } from '~shared/types/Klage'
import { underkjennVedtak } from '~shared/api/tilbakekreving'

import { isPending } from '~shared/api/apiUtils'

type Props = {
  klage: Klage
  kommentar: string
  valgtBegrunnelse: string
}

export const UnderkjennKlage = ({ klage, kommentar, valgtBegrunnelse }: Props) => {
  const [modalisOpen, setModalisOpen] = useState(false)
  const navigate = useNavigate()

  const [underkjennStatus, apiUnderkjennVedtak] = useApiCall(underkjennVedtak)

  const underkjenn = () => {
    apiUnderkjennVedtak({ behandlingsId: klage.id, kommentar, valgtBegrunnelse }, () => {
      navigate(`/person/${klage.sak.ident}`)
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

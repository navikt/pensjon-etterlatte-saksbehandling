import { Button } from '@navikt/ds-react'
import { useState } from 'react'
import { GeneriskModal } from '~shared/modal/modal'
import { useNavigate } from 'react-router'
import { useApiCall } from '~shared/hooks/useApiCall'
import { underkjennVedtak } from '~shared/api/vedtaksvurdering'

import { isPending } from '~shared/api/apiUtils'
import { hentSakId } from '~components/person/sakOgBehandling/sakUtils'

type Props = {
  behandlingId: string
  kommentar: string
  valgtBegrunnelse: string
}

export const UnderkjennYtelse = ({ behandlingId, kommentar, valgtBegrunnelse }: Props) => {
  const [modalisOpen, setModalisOpen] = useState(false)
  const navigate = useNavigate()
  const [underkjennStatus, apiUnderkjennVedtak] = useApiCall(underkjennVedtak)
  const sakId = hentSakId()

  const underkjenn = () => {
    apiUnderkjennVedtak({ behandlingId, kommentar, valgtBegrunnelse }, () => {
      navigate(`/person/${sakId}`)
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

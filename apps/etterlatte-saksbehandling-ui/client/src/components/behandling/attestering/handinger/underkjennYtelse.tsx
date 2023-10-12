import { Button } from '@navikt/ds-react'
import { useState } from 'react'
import { GeneriskModal } from '~shared/modal/modal'
import { hentBehandling } from '~shared/api/behandling'
import { useNavigate } from 'react-router'
import { isPending, useApiCall } from '~shared/hooks/useApiCall'
import { underkjennVedtak } from '~shared/api/vedtaksvurdering'

type Props = {
  behandlingId: string
  kommentar: string
  valgtBegrunnelse: string
}

export const UnderkjennYtelse: React.FC<Props> = ({ behandlingId, kommentar, valgtBegrunnelse }) => {
  const [modalisOpen, setModalisOpen] = useState(false)
  const navigate = useNavigate()

  const [underkjennStatus, apiUnderkjennVedtak] = useApiCall(underkjennVedtak)
  const [behandlingStatus, apiHentBehandling] = useApiCall(hentBehandling)

  const underkjenn = () => {
    apiUnderkjennVedtak({ behandlingId, kommentar, valgtBegrunnelse }, () => {
      apiHentBehandling(behandlingId, (behandling) => {
        navigate(`/person/${behandling.søker?.foedselsnummer}`)
      })
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
        loading={isPending(underkjennStatus) || isPending(behandlingStatus)}
      />
    </>
  )
}

import { Button } from '@navikt/ds-react'
import { useState } from 'react'
import { GeneriskModal } from '~shared/modal/modal'
import { hentBehandling, underkjennVedtak } from '~shared/api/behandling'
import { useNavigate } from 'react-router'
import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { isPending, useApiCall } from '~shared/hooks/useApiCall'

type Props = {
  behandling: IDetaljertBehandling
  kommentar: string
  valgtBegrunnelse: string
}

export const UnderkjennYtelse: React.FC<Props> = ({ behandling, kommentar, valgtBegrunnelse }) => {
  const [modalisOpen, setModalisOpen] = useState(false)
  const navigate = useNavigate()

  const [underkjennStatus, apiUnderkjennVedtak] = useApiCall(underkjennVedtak)
  const [behandlingStatus, apiHentBehandling] = useApiCall(hentBehandling)

  const underkjenn = () => {
    if (!behandling.id) throw new Error('Mangler behandlingsid')

    apiUnderkjennVedtak({ behandlingId: behandling.id, kommentar, valgtBegrunnelse }, () => {
      apiHentBehandling(behandling.id, (behandling) => {
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

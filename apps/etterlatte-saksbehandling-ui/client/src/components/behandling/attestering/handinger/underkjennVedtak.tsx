import { Button } from '@navikt/ds-react'
import { useState } from 'react'
import { ButtonWrapper } from '../styled'
import { GeneriskModal } from '~shared/modal/modal'
import { hentBehandling, underkjennVedtak } from '~shared/api/behandling'

type Props = {
  behandlingId: string
  kommentar: string
  valgtBegrunnelse: string
}

export const UnderkjennVedtak: React.FC<Props> = ({ behandlingId, kommentar, valgtBegrunnelse }) => {
  const [modalisOpen, setModalisOpen] = useState(false)

  const underkjenn = () => {
    if (!behandlingId) throw new Error('Mangler behandlingsid')

    underkjennVedtak(behandlingId, kommentar, valgtBegrunnelse).then((response) => {
      if (response.status === 'ok') {
        hentBehandling(behandlingId).then((response) => {
          if (response.status === 'ok') {
            window.location.reload()
          }
        })
      }
    })
  }

  return (
    <>
      <ButtonWrapper>
        <Button variant="primary" size="medium" className="button" onClick={() => setModalisOpen(true)}>
          Bekreft og send i retur
        </Button>
      </ButtonWrapper>
      <GeneriskModal
        tekst="Er du sikker på at vil underkjenne vedtak og sende i retur til saksbehandler?"
        tekstKnappJa="Ja, send i retur"
        tekstKnappNei=" Nei, gå tilbake"
        onYesClick={underkjenn}
        setModalisOpen={setModalisOpen}
        open={modalisOpen}
      />
    </>
  )
}

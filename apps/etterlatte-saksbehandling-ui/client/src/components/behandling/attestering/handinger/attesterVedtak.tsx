import { Button } from '@navikt/ds-react'
import { useState } from 'react'
import { attesterVedtak, hentBehandling } from '~shared/api/behandling'
import { ButtonWrapper } from '../styled'
import { GeneriskModal } from '../modal'

export const AttesterVedtak = ({ behandlingId }: { behandlingId?: string }) => {
  const [modalisOpen, setModalisOpen] = useState(false)

  const attester = () => {
    if (!behandlingId) throw new Error('Mangler behandlingsid')
    attesterVedtak(behandlingId).then((response) => {
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
          Iverksett vedtak
        </Button>
      </ButtonWrapper>
      <GeneriskModal
        tekst="Er du sikker på at vil iverksette vedtaket?"
        tekstKnappJa="Ja, iverksett vedtak"
        tekstKnappNei=" Nei, gå tilbake"
        onYesClick={attester}
        setModalisOpen={setModalisOpen}
        open={modalisOpen}
      />
    </>
  )
}

import { Button } from '@navikt/ds-react'
import { useState } from 'react'
import { attesterVedtak } from '../../../../shared/api/behandling'
import { ButtonWrapper } from '../styled'
import { GeneriskModal } from '../modal'

export const AttesterVedtak = ({ behandlingId }: { behandlingId?: string }) => {
  const [modalisOpen, setModalisOpen] = useState(false)

  const attester = () => {
    if (!behandlingId) throw new Error('Mangler behandlingsid')
    attesterVedtak(behandlingId).then((response) => {
      if (response.status === 200) {
        window.location.reload()
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
      {modalisOpen && (
        <GeneriskModal
          tekst="Er du sikker på at vil iverksette vedtaket?"
          tekstKnappJa="Ja, iverksett vedtak"
          tekstKnappNei=" Nei, gå tilbake"
          funksjon={attester}
          setModalisOpen={setModalisOpen}
        />
      )}
    </>
  )
}

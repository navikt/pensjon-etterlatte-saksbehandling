import { Button } from '@navikt/ds-react'
import { useState } from 'react'
import { ButtonWrapper } from '../styled'
import { GeneriskModal } from '../modal'
import { attesterVedtak } from '../../../../shared/api/behandling'

export const UnderkjennVedtak = ({ behandlingId }: { behandlingId?: string }) => {
  const [modalisOpen, setModalisOpen] = useState(false)

  const underkjennVedtak = () => {
    if (!behandlingId) throw new Error('Mangler behandlingsid')
    const result = attesterVedtak(behandlingId).then((response) => {
      console.log(response)
      if (response.status === 200) {
        window.location.reload()
      }
    })
    console.log(result)
  }

  return (
    <>
      <ButtonWrapper>
        <Button variant="primary" size="medium" className="button" onClick={() => setModalisOpen(true)}>
          Bekreft og send i retur
        </Button>
      </ButtonWrapper>
      {modalisOpen && (
        <GeneriskModal
          tekst="Er du sikker på at vil underkjenne vedtak og sende i retur til saksbehandler?"
          tekstKnappJa="Ja, send i retur"
          tekstKnappNei=" Nei, gå tilbake"
          funksjon={underkjennVedtak}
          setModalisOpen={setModalisOpen}
        />
      )}
    </>
  )
}

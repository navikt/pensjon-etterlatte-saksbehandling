import { Button } from '@navikt/ds-react'
import { useState } from 'react'
import { ButtonWrapper } from '../styled'
import { GeneriskModal } from '../modal'

export const SendIRetur: React.FC = () => {
  const [modalisOpen, setModalisOpen] = useState(false)

  const send = () => {
    console.log('send')
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
          funksjon={send}
          setModalisOpen={setModalisOpen}
        />
      )}
    </>
  )
}

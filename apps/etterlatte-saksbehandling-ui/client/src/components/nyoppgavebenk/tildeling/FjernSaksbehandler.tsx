import { isFailure, isInitial, isPending, isSuccess, useApiCall } from '~shared/hooks/useApiCall'
import { fjernSaksbehandlerApi } from '~shared/api/oppgaverny'
import { Alert, Button, Label, Loader } from '@navikt/ds-react'
import { PencilIcon } from '@navikt/aksel-icons'
import React, { useEffect, useState } from 'react'
import { GeneriskModal } from '~shared/modal/modal'
import styled from 'styled-components'
import { RedigerSaksbehandlerProps } from '~components/nyoppgavebenk/tildeling/RedigerSaksbehandler'

const SaksbehandlerWrapper = styled(Label)`
  padding: 12px 20px;
  margin-right: 0.5rem;
`

export const FjernSaksbehandler = (props: RedigerSaksbehandlerProps) => {
  const [modalIsOpen, setModalIsOpen] = useState(false)
  const { saksbehandler, oppgaveId, sakId, oppdaterTildeling, erRedigerbar, versjon, type } = props
  const [fjernSaksbehandlerSvar, fjernSaksbehandler] = useApiCall(fjernSaksbehandlerApi)

  useEffect(() => {
    if (isSuccess(fjernSaksbehandlerSvar)) {
      setTimeout(() => {
        oppdaterTildeling(oppgaveId, null)
      }, 3000)
    }
  }, [fjernSaksbehandlerSvar])

  return (
    <>
      {isInitial(fjernSaksbehandlerSvar) && (
        <>
          {erRedigerbar ? (
            <Button
              icon={<PencilIcon />}
              iconPosition="right"
              variant="tertiary"
              size="small"
              onClick={() => setModalIsOpen(true)}
            >
              {saksbehandler}
            </Button>
          ) : (
            <SaksbehandlerWrapper>{saksbehandler}</SaksbehandlerWrapper>
          )}

          <GeneriskModal
            tittel="Endre saksbehandler"
            beskrivelse="Ønsker du å fjerne deg som saksbehandler?"
            tekstKnappJa="Ja, fjern meg som saksbehandler"
            tekstKnappNei="Nei"
            onYesClick={() => fjernSaksbehandler({ oppgaveId: oppgaveId, sakId: sakId, type: type, versjon: versjon })}
            setModalisOpen={setModalIsOpen}
            open={modalIsOpen}
          />
        </>
      )}

      {isPending(fjernSaksbehandlerSvar) && <Loader size="small" title="Fjerner saksbehandler" />}
      {isFailure(fjernSaksbehandlerSvar) && (
        <Alert variant="error" size="small">
          Feil ved fjerning av saksbehandling
        </Alert>
      )}
      {isSuccess(fjernSaksbehandlerSvar) && (
        <Alert variant="success" size="small">
          Du er fjernet som saksbehandler
        </Alert>
      )}
    </>
  )
}

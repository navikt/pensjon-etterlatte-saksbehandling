import { useAppSelector } from '~store/Store'
import { isFailure, isInitial, isPending, isSuccess, useApiCall } from '~shared/hooks/useApiCall'
import { fjernSaksbehandlerApi, byttSaksbehandlerApi } from '~shared/api/oppgaverny'
import { Alert, Button, Loader } from '@navikt/ds-react'
import { ApiErrorAlert } from '~ErrorBoundary'
import { PencilIcon } from '@navikt/aksel-icons'
import React, { useState } from 'react'
import { GeneriskModal } from '~shared/modal/modal'
import styled from 'styled-components'

const SaksbehandlerWrapper = styled.span`
  margin-right: 0.5rem;
`

export const RedigerSaksbehandler = (props: { saksbehandler: string; oppgaveId: string; sakId: number }) => {
  const [modalIsOpen, setModalIsOpen] = useState(false)
  const { saksbehandler, oppgaveId, sakId } = props
  const user = useAppSelector((state) => state.saksbehandlerReducer.saksbehandler)
  const [fjernSaksbehandlerSvar, fjernSaksbehandler] = useApiCall(fjernSaksbehandlerApi)
  const [byttSaksbehandlerSvar, byttSaksbehandler] = useApiCall(byttSaksbehandlerApi)

  const brukerErSaksbehandler = user.ident === saksbehandler

  return (
    <>
      {brukerErSaksbehandler ? (
        <>
          {isInitial(fjernSaksbehandlerSvar) && (
            <>
              <SaksbehandlerWrapper>{saksbehandler}</SaksbehandlerWrapper>
              <Button icon={<PencilIcon />} size="small" variant="secondary" onClick={() => setModalIsOpen(true)}>
                Endre
              </Button>
              <GeneriskModal
                tittel="Endre saksbehandler"
                beskrivelse="Ønsker du å fjerne deg som saksbehandler?"
                tekstKnappJa="Ja, fjern meg som saksbehandler"
                tekstKnappNei="Nei"
                onYesClick={() => fjernSaksbehandler({ oppgaveId: oppgaveId, sakId: sakId })}
                setModalisOpen={setModalIsOpen}
                open={modalIsOpen}
              />
            </>
          )}

          {isPending(fjernSaksbehandlerSvar) && <Loader size="small" title="Fjerner saksbehandler" />}
          {isFailure(fjernSaksbehandlerSvar) && (
            <ApiErrorAlert>Kunne ikke fjerne saksbehandler fra oppgaven</ApiErrorAlert>
          )}
          {isSuccess(fjernSaksbehandlerSvar) && 'Du er fjernet som saksbehandler'}
        </>
      ) : (
        <>
          {isInitial(byttSaksbehandlerSvar) && (
            <>
              <SaksbehandlerWrapper>{saksbehandler}</SaksbehandlerWrapper>
              <Button icon={<PencilIcon />} size="small" variant="secondary" onClick={() => setModalIsOpen(true)}>
                Endre
              </Button>
              <GeneriskModal
                tittel="Endre saksbehandler"
                beskrivelse={`Ønsker du å fjerne ${saksbehandler} og sette deg som saksbehandler?`}
                tekstKnappJa="Ja, sett meg som saksbehandler"
                tekstKnappNei="Nei"
                onYesClick={() =>
                  byttSaksbehandler({
                    nysaksbehandler: { oppgaveId: oppgaveId, saksbehandler: user.ident },
                    sakId: sakId,
                  })
                }
                setModalisOpen={setModalIsOpen}
                open={modalIsOpen}
              />
            </>
          )}

          {isPending(byttSaksbehandlerSvar) && <Loader size="small" title="Bytter saksbehandler" />}
          {isFailure(byttSaksbehandlerSvar) && (
            <ApiErrorAlert>Kunne ikke bytte saksbehandler fra oppgaven</ApiErrorAlert>
          )}
          {isSuccess(byttSaksbehandlerSvar) && (
            <Alert variant="success">Saksbehandler er endret og oppgaven ble lagt på din oppgaveliste</Alert>
          )}
        </>
      )}
    </>
  )
}

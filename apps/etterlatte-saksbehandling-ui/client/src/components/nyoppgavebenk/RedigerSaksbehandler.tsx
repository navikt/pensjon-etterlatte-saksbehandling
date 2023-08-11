import { useAppSelector } from '~store/Store'
import { isFailure, isInitial, isPending, isSuccess, useApiCall } from '~shared/hooks/useApiCall'
import { fjernSaksbehandlerApi, byttSaksbehandlerApi, Oppgavestatus, erOppgaveRedigerbar } from '~shared/api/oppgaverny'
import { Alert, Button, Loader, Label } from '@navikt/ds-react'
import { ApiErrorAlert } from '~ErrorBoundary'
import { PencilIcon } from '@navikt/aksel-icons'
import React, { useEffect, useState } from 'react'
import { GeneriskModal } from '~shared/modal/modal'
import styled from 'styled-components'

const SaksbehandlerWrapper = styled(Label)`
  padding: 12px 20px;
  margin-right: 0.5rem;
`

export const RedigerSaksbehandler = (props: {
  saksbehandler: string
  oppgaveId: string
  sakId: number
  status: Oppgavestatus
  type: string
  hentOppgaver: () => void
}) => {
  const [modalIsOpen, setModalIsOpen] = useState(false)
  const { saksbehandler, oppgaveId, sakId, status, type, hentOppgaver } = props
  const user = useAppSelector((state) => state.saksbehandlerReducer.saksbehandler)
  const [fjernSaksbehandlerSvar, fjernSaksbehandler] = useApiCall(fjernSaksbehandlerApi)
  const [byttSaksbehandlerSvar, byttSaksbehandler] = useApiCall(byttSaksbehandlerApi)
  const erRedigerbar = erOppgaveRedigerbar(status, type)

  const brukerErSaksbehandler = user.ident === saksbehandler

  useEffect(() => {
    if (isSuccess(fjernSaksbehandlerSvar) || isSuccess(byttSaksbehandlerSvar)) {
      hentOppgaver()
    }
  }, [fjernSaksbehandlerSvar, byttSaksbehandlerSvar])

  return (
    <>
      {brukerErSaksbehandler ? (
        <>
          {isInitial(fjernSaksbehandlerSvar) && (
            <>
              {erRedigerbar ? (
                <Button
                  icon={<PencilIcon />}
                  iconPosition="right"
                  variant="tertiary"
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
              {erRedigerbar ? (
                <Button
                  icon={<PencilIcon />}
                  iconPosition="right"
                  variant="tertiary"
                  onClick={() => setModalIsOpen(true)}
                >
                  {saksbehandler}
                </Button>
              ) : (
                <SaksbehandlerWrapper>{saksbehandler}</SaksbehandlerWrapper>
              )}
              <GeneriskModal
                tittel="Endre saksbehandler"
                beskrivelse={`Vil du overta oppgaven til ${saksbehandler}?`}
                tekstKnappJa="Ja, overta oppgaven"
                tekstKnappNei="Nei"
                onYesClick={() =>
                  byttSaksbehandler({
                    oppgaveId: oppgaveId,
                    nysaksbehandler: { saksbehandler: user.ident },
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

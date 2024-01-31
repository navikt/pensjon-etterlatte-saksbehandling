import { useApiCall } from '~shared/hooks/useApiCall'
import { fjernSaksbehandlerApi } from '~shared/api/oppgaver'
import { Alert, Button, Label, Loader } from '@navikt/ds-react'
import { PencilIcon } from '@navikt/aksel-icons'
import React, { useEffect, useState } from 'react'
import { GeneriskModal } from '~shared/modal/modal'
import styled from 'styled-components'
import { RedigerSaksbehandlerProps } from '~components/oppgavebenk/tildeling/RedigerSaksbehandler'

import { isSuccess, mapAllApiResult } from '~shared/api/apiUtils'
import { ApiErrorAlert } from '~ErrorBoundary'

const SaksbehandlerWrapper = styled(Label)`
  padding: 12px 20px;
  margin-right: 0.5rem;
`

export const FjernSaksbehandler = (props: RedigerSaksbehandlerProps) => {
  const [modalIsOpen, setModalIsOpen] = useState(false)
  const { saksbehandler, saksbehandlerNavn, oppgaveId, sakId, oppdaterTildeling, erRedigerbar, versjon, type } = props
  const [fjernSaksbehandlerSvar, fjernSaksbehandler] = useApiCall(fjernSaksbehandlerApi)

  useEffect(() => {
    if (isSuccess(fjernSaksbehandlerSvar)) {
      oppdaterTildeling(oppgaveId, null, fjernSaksbehandlerSvar.data.versjon)
    }
  }, [fjernSaksbehandlerSvar])

  return (
    <>
      {mapAllApiResult(
        fjernSaksbehandlerSvar,
        <Loader size="small" title="Fjerner saksbehandler" />,
        <>
          {erRedigerbar ? (
            <Button
              icon={<PencilIcon />}
              iconPosition="right"
              variant="tertiary"
              size="small"
              onClick={() => setModalIsOpen(true)}
            >
              {saksbehandlerNavn ? saksbehandlerNavn : saksbehandler}
            </Button>
          ) : (
            <SaksbehandlerWrapper>{saksbehandlerNavn ? saksbehandlerNavn : saksbehandler}</SaksbehandlerWrapper>
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
        </>,
        () => (
          <ApiErrorAlert size="small">Feil ved fjerning av saksbehandling</ApiErrorAlert>
        ),
        () => (
          <Alert variant="success" size="small">
            Du er fjernet som saksbehandler
          </Alert>
        )
      )}
    </>
  )
}

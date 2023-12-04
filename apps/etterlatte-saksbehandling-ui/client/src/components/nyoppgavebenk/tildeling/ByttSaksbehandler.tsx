import { useAppSelector } from '~store/Store'
import { useApiCall } from '~shared/hooks/useApiCall'
import { byttSaksbehandlerApi } from '~shared/api/oppgaver'
import { Alert, Button, Label, Loader } from '@navikt/ds-react'
import { PencilIcon } from '@navikt/aksel-icons'
import React, { useEffect, useState } from 'react'
import { GeneriskModal } from '~shared/modal/modal'
import styled from 'styled-components'
import { RedigerSaksbehandlerProps } from '~components/nyoppgavebenk/tildeling/RedigerSaksbehandler'

import { isSuccess, mapAllApiResult } from '~shared/api/apiUtils'
import { ApiErrorAlert } from '~ErrorBoundary'

const SaksbehandlerWrapper = styled(Label)`
  padding: 12px 20px;
  margin-right: 0.5rem;
`

export const ByttSaksbehandler = (props: RedigerSaksbehandlerProps) => {
  const [modalIsOpen, setModalIsOpen] = useState(false)
  const { saksbehandler, oppgaveId, oppdaterTildeling, erRedigerbar, versjon, type } = props
  const innloggetSaksbehandler = useAppSelector((state) => state.saksbehandlerReducer.innloggetSaksbehandler)
  const [byttSaksbehandlerSvar, byttSaksbehandler] = useApiCall(byttSaksbehandlerApi)

  useEffect(() => {
    if (isSuccess(byttSaksbehandlerSvar)) {
      oppdaterTildeling(oppgaveId, innloggetSaksbehandler.ident)
    }
  }, [byttSaksbehandlerSvar])

  return (
    <>
      {mapAllApiResult(
        byttSaksbehandlerSvar,
        <Loader size="small" title="Bytter saksbehandler" />,
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
            beskrivelse={`Vil du overta oppgaven til ${saksbehandler}?`}
            tekstKnappJa="Ja, overta oppgaven"
            tekstKnappNei="Nei"
            onYesClick={() =>
              byttSaksbehandler({
                oppgaveId: oppgaveId,
                type: type,
                nysaksbehandler: { saksbehandler: innloggetSaksbehandler.ident, versjon: versjon },
              })
            }
            setModalisOpen={setModalIsOpen}
            open={modalIsOpen}
          />
        </>,
        () => (
          <ApiErrorAlert size="small">Kunne ikke bytte saksbehandler fra oppgaven</ApiErrorAlert>
        ),
        () => (
          <Alert variant="success" size="small">
            Saksbehandler er endret og oppgaven ble lagt på din oppgaveliste
          </Alert>
        )
      )}
    </>
  )
}

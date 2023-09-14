import { useAppSelector } from '~store/Store'
import { isFailure, isInitial, isPending, isSuccess, useApiCall } from '~shared/hooks/useApiCall'
import { byttSaksbehandlerApi } from '~shared/api/oppgaverny'
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

export const ByttSaksbehandler = (props: RedigerSaksbehandlerProps) => {
  const [modalIsOpen, setModalIsOpen] = useState(false)
  const { saksbehandler, oppgaveId, oppdaterTildeling, erRedigerbar, versjon, type } = props
  const user = useAppSelector((state) => state.saksbehandlerReducer.saksbehandler)
  const [byttSaksbehandlerSvar, byttSaksbehandler] = useApiCall(byttSaksbehandlerApi)

  useEffect(() => {
    if (isSuccess(byttSaksbehandlerSvar)) {
      setTimeout(() => {
        oppdaterTildeling(oppgaveId, user.ident)
      }, 3000)
    }
  }, [byttSaksbehandlerSvar])

  return (
    <>
      {isInitial(byttSaksbehandlerSvar) && (
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
                nysaksbehandler: { saksbehandler: user.ident, versjon: versjon },
              })
            }
            setModalisOpen={setModalIsOpen}
            open={modalIsOpen}
          />
        </>
      )}

      {isPending(byttSaksbehandlerSvar) && <Loader size="small" title="Bytter saksbehandler" />}
      {isFailure(byttSaksbehandlerSvar) && (
        <Alert variant="error" size="small">
          Kunne ikke bytte saksbehandler fra oppgaven
        </Alert>
      )}
      {isSuccess(byttSaksbehandlerSvar) && (
        <Alert variant="success" size="small">
          Saksbehandler er endret og oppgaven ble lagt på din oppgaveliste
        </Alert>
      )}
    </>
  )
}

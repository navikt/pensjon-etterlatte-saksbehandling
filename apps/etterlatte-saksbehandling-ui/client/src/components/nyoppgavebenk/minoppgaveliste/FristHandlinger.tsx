import React, { useState } from 'react'
import { isFailure, isPending, isSuccess, useApiCall } from '~shared/hooks/useApiCall'
import { redigerFristApi } from '~shared/api/oppgaverny'
import { Alert, Button, Heading, Modal, MonthPicker } from '@navikt/ds-react'
import { ApiErrorAlert } from '~ErrorBoundary'
import { formaterStringDato } from '~utils/formattering'
import { PencilIcon } from '@navikt/aksel-icons'
import styled from 'styled-components'
import { FristWrapper } from '~components/nyoppgavebenk/Oppgavelista'
import { isBefore } from 'date-fns'

const Buttonwrapper = styled.div`
  margin: 4rem 1rem 1rem 1rem;
  button:first-child {
    margin-right: 1rem;
  }
`

export const FristHandlinger = (props: { frist: string; oppgaveId: string; sakId: number }) => {
  const { frist, oppgaveId, sakId } = props
  const [open, setOpen] = useState(false)
  const [nyFrist, setnyFrist] = useState<Date>(new Date())
  const [redigerfristSvar, redigerFrist, resetredigerFristApi] = useApiCall(redigerFristApi)

  const generateFromDate = () => {
    const naa = new Date()
    naa.setMonth(naa.getMonth() - 1)
    return naa
  }
  const generateToDate = () => {
    const naa = new Date()
    naa.setMonth(naa.getMonth() + 24)
    return naa
  }

  const redigerDatoMedtimerForAAslippetidssoneproblemUTC = (valgDato: Date) => {
    if (valgDato) {
      const datoSomSisteDagIMaaneden = new Date(valgDato.getFullYear(), valgDato.getMonth() + 1, 0)
      datoSomSisteDagIMaaneden.setHours(17)
      setnyFrist(datoSomSisteDagIMaaneden)
      resetredigerFristApi()
    }
  }

  return (
    <>
      {frist ? (
        <>
          <Modal open={open} onClose={() => setOpen((x) => !x)} closeButton={false} aria-labelledby="modal-heading">
            <Modal.Content>
              <Heading spacing level="2" size="medium" id="modal-heading">
                Velg ny frist
              </Heading>
              <MonthPicker.Standalone
                onMonthSelect={redigerDatoMedtimerForAAslippetidssoneproblemUTC}
                selected={nyFrist}
                dropdownCaption
                fromDate={generateFromDate()}
                toDate={generateToDate()}
              />
              {isSuccess(redigerfristSvar) && <Alert variant="success">Frist er endret</Alert>}
              {isFailure(redigerfristSvar) && <ApiErrorAlert>Kunne ikke lagre ny frist</ApiErrorAlert>}
              {!nyFrist && <Alert variant="warning">Du må velge en måned</Alert>}
              <Buttonwrapper>
                <Button
                  loading={isPending(redigerfristSvar)}
                  disabled={!nyFrist}
                  onClick={() => {
                    redigerFrist({ redigerFristRequest: { oppgaveId: oppgaveId, frist: nyFrist }, sakId: sakId })
                  }}
                >
                  Lagre ny frist
                </Button>
                <Button variant="secondary" onClick={() => setOpen(!open)}>
                  Avbryt
                </Button>
              </Buttonwrapper>
            </Modal.Content>
          </Modal>
          <FristWrapper fristHarPassert={isBefore(new Date(frist), new Date())}>
            {formaterStringDato(frist)}
          </FristWrapper>
          <Button
            size="small"
            variant="secondary"
            icon={<PencilIcon title="a11y-title" fontSize="1.5rem" />}
            onClick={() => setOpen(!open)}
          >
            Rediger frist
          </Button>
        </>
      ) : (
        'Ingen frist'
      )}
    </>
  )
}

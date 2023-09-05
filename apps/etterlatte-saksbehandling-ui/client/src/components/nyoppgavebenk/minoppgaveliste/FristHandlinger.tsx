import React, { useEffect, useState } from 'react'
import { isFailure, isPending, isSuccess, useApiCall } from '~shared/hooks/useApiCall'
import { Oppgavetype, redigerFristApi } from '~shared/api/oppgaverny'
import { Alert, Button, Heading, Modal, DatePicker, Label } from '@navikt/ds-react'
import { ApiErrorAlert } from '~ErrorBoundary'
import { formaterStringDato } from '~utils/formattering'
import { PencilIcon } from '@navikt/aksel-icons'
import styled from 'styled-components'
import { add, isBefore } from 'date-fns'

const Buttonwrapper = styled.div`
  margin: 4rem 1rem 1rem 1rem;
  button:first-child {
    margin-right: 1rem;
  }
`

const FristWrapper = styled.span<{ fristHarPassert: boolean; utenKnapp?: boolean }>`
  color: ${(p) => p.fristHarPassert && 'var(--a-text-danger)'};
  padding: ${(p) => p.utenKnapp && '12px 20px'};
`

export const FristHandlinger = (props: {
  orginalFrist: string
  oppgaveId: string
  oppgaveVersjon: string | null
  type: Oppgavetype
  hentOppgaver: () => void
  erRedigerbar: boolean
}) => {
  const { orginalFrist, oppgaveId, hentOppgaver, erRedigerbar, oppgaveVersjon, type } = props
  const [open, setOpen] = useState(false)
  const [frist, setFrist] = useState<string>()
  const [nyFrist, setnyFrist] = useState<Date>(new Date())
  const [redigerfristSvar, redigerFrist, resetredigerFristApi] = useApiCall(redigerFristApi)

  const generateFromDate = () => {
    // Legger til 1 dag siden ny frist må være fremover i tid
    const naa = add(new Date(), { days: 1 })
    return naa
  }
  const generateToDate = () => {
    const naa = new Date()
    naa.setMonth(naa.getMonth() + 24)
    return naa
  }

  const redigerDatoMedtimerForAAslippetidssoneproblemUTC = (valgtDato: Date | undefined) => {
    if (valgtDato) {
      valgtDato.setHours(17)
      setnyFrist(valgtDato)
      resetredigerFristApi()
    }
  }
  useEffect(() => {
    if (isSuccess(redigerfristSvar)) {
      setFrist(nyFrist.toISOString())
    }
  }, [redigerfristSvar])

  useEffect(() => {
    if (orginalFrist) setFrist(orginalFrist)
  }, [orginalFrist])

  return (
    <>
      {frist ? (
        <>
          <Modal open={open} onClose={() => setOpen((x) => !x)} aria-labelledby="modal-heading">
            <Modal.Body>
              <Modal.Header closeButton={false}>
                <Heading spacing level="2" size="medium" id="modal-heading">
                  Velg ny frist
                </Heading>
              </Modal.Header>
              <DatePicker.Standalone
                onSelect={redigerDatoMedtimerForAAslippetidssoneproblemUTC}
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
                    redigerFrist({ oppgaveId, type, redigerFristRequest: { frist: nyFrist, versjon: oppgaveVersjon } })
                  }}
                >
                  Lagre ny frist
                </Button>

                {isSuccess(redigerfristSvar) ? (
                  <Button
                    variant="secondary"
                    onClick={() => {
                      hentOppgaver()
                      setOpen(false)
                    }}
                  >
                    Lukk
                  </Button>
                ) : (
                  <Button variant="secondary" onClick={() => setOpen(false)}>
                    Avbryt
                  </Button>
                )}
              </Buttonwrapper>
            </Modal.Body>
          </Modal>

          {erRedigerbar ? (
            <Button
              variant="tertiary"
              iconPosition="right"
              icon={<PencilIcon title="a11y-title" fontSize="1.5rem" />}
              onClick={() => setOpen(!open)}
            >
              <FristWrapper fristHarPassert={isBefore(new Date(frist), new Date())}>
                {formaterStringDato(frist)}
              </FristWrapper>
            </Button>
          ) : (
            <FristWrapper fristHarPassert={isBefore(new Date(frist), new Date())} utenKnapp>
              <Label>{formaterStringDato(frist)}</Label>
            </FristWrapper>
          )}
        </>
      ) : (
        'Ingen frist'
      )}
    </>
  )
}

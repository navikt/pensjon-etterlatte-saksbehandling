import React, { useEffect, useState } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { Oppgavetype, redigerFristApi } from '~shared/api/oppgaver'
import { Alert, Button, DatePicker, Heading, Label, Modal } from '@navikt/ds-react'
import { formaterStringDato } from '~utils/formattering'
import { PencilIcon } from '@navikt/aksel-icons'
import styled from 'styled-components'
import { add, isBefore } from 'date-fns'
import { FlexRow } from '~shared/styled'

import { isPending, isSuccess } from '~shared/api/apiUtils'
import { isFailureHandler } from '~shared/api/IsFailureHandler'

const FristWrapper = styled.span<{ fristHarPassert: boolean; utenKnapp?: boolean }>`
  color: ${(p) => p.fristHarPassert && 'var(--a-text-danger)'};
  padding: ${(p) => p.utenKnapp && '12px 20px'};
`

export const FristHandlinger = (props: {
  orginalFrist: string
  oppgaveId: string
  oppgaveVersjon: number | null
  type: Oppgavetype
  oppdaterFrist: (id: string, nyfrist: string, versjon: number | null) => void
  erRedigerbar: boolean
}) => {
  const { orginalFrist, oppgaveId, oppdaterFrist, erRedigerbar, oppgaveVersjon, type } = props
  const [open, setOpen] = useState(false)
  const [frist, setFrist] = useState<string>()
  const [nyFrist, setnyFrist] = useState<Date>(new Date())
  const [redigerfristSvar, redigerFrist, resetredigerFristApi] = useApiCall(redigerFristApi)

  const generateFromDate = () => {
    // Legger til 1 dag siden ny frist må være fremover i tid
    return add(new Date(), { days: 1 })
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
          <Modal open={open} onClose={() => setOpen(false)} aria-labelledby="modal-heading">
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
              {isSuccess(redigerfristSvar) && (
                <Alert style={{ marginBottom: '1rem' }} variant="success">
                  Frist er endret
                </Alert>
              )}
              {isFailureHandler({
                apiResult: redigerfristSvar,
                errorMessage: 'Kunne ikke lagre ny frist',
              })}
              {!nyFrist && <Alert variant="warning">Du må velge en måned</Alert>}

              <FlexRow justify="right">
                {isSuccess(redigerfristSvar) ? (
                  <Button
                    variant="secondary"
                    onClick={() => {
                      oppdaterFrist(oppgaveId, nyFrist.toISOString(), oppgaveVersjon)
                      setOpen(false)
                    }}
                  >
                    Lukk
                  </Button>
                ) : (
                  <Button variant="secondary" onClick={() => setOpen(false)} disabled={isPending(redigerfristSvar)}>
                    Avbryt
                  </Button>
                )}

                <Button
                  loading={isPending(redigerfristSvar)}
                  disabled={!nyFrist}
                  onClick={() => {
                    redigerFrist({ oppgaveId, type, redigerFristRequest: { frist: nyFrist, versjon: oppgaveVersjon } })
                  }}
                >
                  Lagre ny frist
                </Button>
              </FlexRow>
            </Modal.Body>
          </Modal>

          {erRedigerbar ? (
            <Button
              size="small"
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

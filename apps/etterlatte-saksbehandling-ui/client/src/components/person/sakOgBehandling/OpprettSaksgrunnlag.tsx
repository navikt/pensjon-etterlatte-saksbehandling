import React, { useEffect, useState } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { Alert, Button, Heading, HStack, Modal, VStack } from '@navikt/ds-react'
import { PlusCircleIcon } from '@navikt/aksel-icons'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { isPending, isSuccess, mapResult, mapSuccess } from '~shared/api/apiUtils'
import { hentSakForPerson } from '~shared/api/sak'
import { ISakMedUtlandstilknytning } from '~shared/types/sak'
import { formaterSakstype } from '~utils/formatering/formatering'
import { getGrunnlagFinnes } from '~shared/api/grunnlag'
import { ApiErrorAlert } from '~ErrorBoundary'
import Spinner from '~shared/Spinner'

export const OpprettSaksgrunnlag = ({ sak }: { sak: ISakMedUtlandstilknytning }) => {
  const [isOpen, setIsOpen] = useState(false)
  const [sakResult, hentEllerOpprettSak] = useApiCall(hentSakForPerson)
  const [grunnlagFinnesResult, sjekkOmGrunnlagFinnes] = useApiCall(getGrunnlagFinnes)

  useEffect(() => {
    if (isOpen) sjekkOmGrunnlagFinnes(sak.id)
  }, [isOpen])

  const opprett = () => {
    hentEllerOpprettSak({ fnr: sak.ident, type: sak.sakType, opprettHvisIkkeFinnes: true }, () => {
      setTimeout(() => window.location.reload(), 3000)
    })
  }

  const avbryt = () => setIsOpen(false)

  return (
    <>
      <div>
        <Button variant="primary" onClick={() => setIsOpen(true)} icon={<PlusCircleIcon aria-hidden />}>
          Opprett saksgrunnlag
        </Button>
      </div>

      <Modal open={isOpen} onClose={avbryt} width="medium" aria-label="Opprett sak">
        {mapResult(grunnlagFinnesResult, {
          pending: <Spinner label="Sjekker om grunnlag kan opprettes..." />,
          error: (error) => <ApiErrorAlert>{error.detail}</ApiErrorAlert>,
          success: (grunnlagFinnes) =>
            grunnlagFinnes ? (
              <Modal.Body>
                <VStack gap="space-4">
                  <Alert variant="warning">Grunnlag finnes allerede for denne saken!</Alert>

                  <HStack gap="space-4" justify="end">
                    <Button variant="secondary" onClick={avbryt}>
                      Lukk
                    </Button>
                  </HStack>
                </VStack>
              </Modal.Body>
            ) : (
              <>
                <Modal.Body>
                  <Heading size="large" spacing>
                    Gjenopprett sak og grunnlag
                  </Heading>

                  <Alert variant="info">Det finnes ingen grunnlag på denne saken. Du kan opprette nytt grunnlag.</Alert>

                  {isFailureHandler({
                    apiResult: sakResult,
                    errorMessage: `Kunne ikke opprette grunnlag for sak`,
                  })}

                  {mapSuccess(sakResult, (sak) => (
                    <Alert variant="success">
                      Opprettet {formaterSakstype(sak.sakType)} med id {sak.id}! Laster siden på nytt...
                    </Alert>
                  ))}
                </Modal.Body>

                <Modal.Footer>
                  <HStack gap="space-4" justify="end">
                    <Button
                      variant="secondary"
                      type="button"
                      disabled={isPending(sakResult) || isSuccess(sakResult)}
                      onClick={avbryt}
                    >
                      Avbryt
                    </Button>
                    <Button
                      variant="primary"
                      type="submit"
                      loading={isPending(sakResult) || isSuccess(sakResult)}
                      onClick={opprett}
                    >
                      Opprett saksgrunnlag
                    </Button>
                  </HStack>
                </Modal.Footer>
              </>
            ),
        })}
      </Modal>
    </>
  )
}

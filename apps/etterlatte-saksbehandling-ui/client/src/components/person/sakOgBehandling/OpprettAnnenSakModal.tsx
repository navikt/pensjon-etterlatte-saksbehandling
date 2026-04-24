import React, { useState } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { Alert, Button, Heading, HStack, Modal, VStack } from '@navikt/ds-react'
import { PlusCircleIcon } from '@navikt/aksel-icons'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { isPending, isSuccess, mapSuccess } from '~shared/api/apiUtils'
import { hentSakForPerson } from '~shared/api/sak'
import { ISakMedUtlandstilknytning, SakType } from '~shared/types/sak'
import { formaterSakstype } from '~utils/formatering/formatering'

const annenSakType = (sakType: SakType): SakType =>
  sakType === SakType.BARNEPENSJON ? SakType.OMSTILLINGSSTOENAD : SakType.BARNEPENSJON

interface Props {
  sak: ISakMedUtlandstilknytning
}

export const OpprettAnnenSakModal = ({ sak }: Props) => {
  const [isOpen, setIsOpen] = useState(false)
  const [sakResult, hentEllerOpprettSak] = useApiCall(hentSakForPerson)

  const nyType = annenSakType(sak.sakType)

  const opprett = () => {
    hentEllerOpprettSak({ fnr: sak.ident, type: nyType, opprettHvisIkkeFinnes: true }, () => {
      setTimeout(() => window.location.reload(), 3000)
    })
  }

  const avbryt = () => setIsOpen(false)

  return (
    <>
      <Button variant="secondary" size="small" onClick={() => setIsOpen(true)} icon={<PlusCircleIcon aria-hidden />}>
        Opprett annen sak
      </Button>
      <Modal open={isOpen} onClose={avbryt} width="medium" aria-label="Opprett annen sak">
        <Modal.Body>
          <VStack gap="space-16">
            <Heading size="large" spacing>
              Opprett annen sak
            </Heading>

            <Alert variant="warning">
              Dette kan ikke angres og skal kun gjøres ved behov. Bruk denne funksjonen med varsomhet.
            </Alert>

            <Alert variant="info">
              Du er i ferd med å opprette en ny sak av typen <strong>{formaterSakstype(nyType).toLowerCase()}</strong>{' '}
              for denne brukeren.
            </Alert>

            {isFailureHandler({
              apiResult: sakResult,
              errorMessage: `Kunne ikke opprette ${formaterSakstype(nyType).toLowerCase()} for bruker`,
            })}

            {mapSuccess(sakResult, (opprettetSak) => (
              <Alert variant="success">
                Opprettet {formaterSakstype(opprettetSak.sakType)} med id {opprettetSak.id}! Laster siden på nytt...
              </Alert>
            ))}
          </VStack>
        </Modal.Body>

        <Modal.Footer>
          <HStack gap="space-16" justify="end">
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
              Opprett annen sak
            </Button>
          </HStack>
        </Modal.Footer>
      </Modal>
    </>
  )
}

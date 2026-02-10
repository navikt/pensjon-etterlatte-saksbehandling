import React, { useState } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { Alert, Button, Heading, HStack, Modal, Radio, RadioGroup } from '@navikt/ds-react'
import { PlusCircleIcon } from '@navikt/aksel-icons'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { isPending, isSuccess, mapSuccess } from '~shared/api/apiUtils'
import { hentSakForPerson } from '~shared/api/sak'
import { SakType } from '~shared/types/sak'
import { formaterSakstype } from '~utils/formatering/formatering'

export const OpprettSakModal = ({ fnr }: { fnr: string }) => {
  const [isOpen, setIsOpen] = useState(false)
  const [sakType, setSakType] = useState<SakType>()
  const [sakResult, hentEllerOpprettSak] = useApiCall(hentSakForPerson)

  const opprett = () => {
    if (!sakType) {
      setIsOpen(false)
      return
    }

    hentEllerOpprettSak({ fnr, type: sakType, opprettHvisIkkeFinnes: true }, () => {
      setTimeout(() => window.location.reload(), 3000)
    })
  }

  const avbryt = () => setIsOpen(false)

  return (
    <>
      <Button variant="primary" onClick={() => setIsOpen(true)} icon={<PlusCircleIcon aria-hidden />}>
        Opprett sak
      </Button>

      <Modal open={isOpen} onClose={avbryt} width="medium" aria-label="Opprett sak">
        <Modal.Body>
          <Heading size="large" spacing>
            Opprett sak
          </Heading>

          <RadioGroup legend="Velg saktype" onChange={(type) => setSakType(type)}>
            <Radio value={SakType.OMSTILLINGSSTOENAD}>{formaterSakstype(SakType.OMSTILLINGSSTOENAD)}</Radio>
            <Radio value={SakType.BARNEPENSJON}>{formaterSakstype(SakType.BARNEPENSJON)}</Radio>
          </RadioGroup>

          <br />

          {!sakType ? (
            <Alert variant="warning" inline>
              Du må velge sakstype for å opprette ny sak
            </Alert>
          ) : (
            <Alert variant="info">
              Du oppretter nå en sak av typen <strong>{formaterSakstype(sakType).toLowerCase()}</strong>
            </Alert>
          )}

          {isFailureHandler({
            apiResult: sakResult,
            errorMessage: `Kunne ikke opprette sak for ${fnr}`,
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
              disabled={!sakType}
              onClick={opprett}
            >
              Opprett sak
            </Button>
          </HStack>
        </Modal.Footer>
      </Modal>
    </>
  )
}

import { Alert, Button, Heading, HStack, Loader, Modal, Tag, VStack } from '@navikt/ds-react'
import React, { useState } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { oppdaterIdentPaaSak } from '~shared/api/sak'
import { isPending, isSuccess, mapFailure } from '~shared/api/apiUtils'
import { Folkeregisteridentifikatorsamsvar, Grunnlagsendringshendelse } from '~components/person/typer'
import { ISakMedUtlandstilknytning } from '~shared/types/sak'
import { ApiErrorAlert } from '~ErrorBoundary'
import { useNavigate } from 'react-router-dom'
import { useFeatureEnabledMedDefault } from '~shared/hooks/useFeatureToggle'

export const OppdaterIdentModal = ({
  sak,
  hendelse,
}: {
  sak: ISakMedUtlandstilknytning
  hendelse: Grunnlagsendringshendelse
}) => {
  const samsvar = hendelse.samsvarMellomKildeOgGrunnlag as Folkeregisteridentifikatorsamsvar

  const navigate = useNavigate()
  const [isOpen, setIsOpen] = useState(false)

  const [oppdaterIdentResult, apiOppdaterIdentPaaSak] = useApiCall(oppdaterIdentPaaSak)
  const kanOppdatereIdentPaaSak = useFeatureEnabledMedDefault('pensjon-etterlatte.oppdater-ident-paa-sak', false)

  const oppdaterIdent = () => {
    apiOppdaterIdentPaaSak({ sakId: sak.id, hendelseId: hendelse.id }, (sak) => {
      setTimeout(() => navigate('/person', { state: { fnr: sak.ident } }), 3000)
    })
  }

  if (!kanOppdatereIdentPaaSak) {
    return null
  }

  return (
    <>
      <Button onClick={() => setIsOpen(true)}>Oppdater ident</Button>

      <Modal
        open={isOpen}
        onClose={() => setIsOpen(false)}
        aria-labelledby="modal-heading"
        header={{ heading: 'Endre til nyeste ident på sak' }}
      >
        <Modal.Body>
          {isSuccess(oppdaterIdentResult) ? (
            <Alert variant="success">
              Sak og tilhørende oppgaver oppdatert med bruker sin nyeste ident. Laster siden på nytt... <Loader />
            </Alert>
          ) : (
            <VStack gap="4">
              <HStack gap="4" justify="space-evenly">
                <div>
                  <Heading size="xsmall">Ident i sak</Heading>
                  <Tag variant="error">{sak.ident}</Tag>
                </div>

                <div>
                  <Heading size="xsmall">Ident i Grunnlag</Heading>
                  <Tag variant="error">{samsvar.fraGrunnlag}</Tag>
                </div>

                <div>
                  <Heading size="xsmall">Ident i PDL</Heading>
                  <Tag variant="success">{samsvar.fraPdl}</Tag>
                </div>
              </HStack>

              <Alert variant="warning">
                Endring av identifikator vil avbryte alle pågående behandlinger for å sikre at grunnlaget blir korrekt
              </Alert>

              {mapFailure(oppdaterIdentResult, (error) => (
                <ApiErrorAlert>{error.detail}</ApiErrorAlert>
              ))}

              <HStack gap="4" justify="center">
                <Button variant="secondary" onClick={() => setIsOpen(false)} disabled={isPending(oppdaterIdentResult)}>
                  Nei, avbryt
                </Button>

                <Button variant="primary" onClick={oppdaterIdent} loading={isPending(oppdaterIdentResult)}>
                  Ja, oppdater
                </Button>
              </HStack>
            </VStack>
          )}
        </Modal.Body>
      </Modal>
    </>
  )
}

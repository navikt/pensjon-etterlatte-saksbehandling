import { Alert, Button, Heading, HStack, Loader, Modal, Tag, VStack } from '@navikt/ds-react'
import React, { useState } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { oppdaterIdentPaaSak } from '~shared/api/sak'
import { isPending, isSuccess, mapFailure } from '~shared/api/apiUtils'
import { Folkeregisteridentifikatorsamsvar, Grunnlagsendringshendelse } from '~components/person/typer'
import { ISakMedUtlandstilknytning } from '~shared/types/sak'
import { ApiErrorAlert } from '~ErrorBoundary'
import { useNavigate } from 'react-router-dom'
import { usePerson } from '~shared/statusbar/usePerson'
import { ArrowsCirclepathIcon } from '@navikt/aksel-icons'

export const OppdaterIdentModal = ({
  sak,
  hendelse,
}: {
  sak: ISakMedUtlandstilknytning
  hendelse: Grunnlagsendringshendelse | null
}) => {
  const samsvar = hendelse?.samsvarMellomKildeOgGrunnlag as Folkeregisteridentifikatorsamsvar

  const person = usePerson()
  const navigate = useNavigate()
  const [isOpen, setIsOpen] = useState(false)

  const [oppdaterIdentResult, apiOppdaterIdentPaaSak] = useApiCall(oppdaterIdentPaaSak)

  const oppdaterIdent = () => {
    apiOppdaterIdentPaaSak({ sakId: sak.id, hendelseId: hendelse?.id, utenHendelse: hendelse === null }, (sak) => {
      setTimeout(() => navigate('/person', { state: { fnr: sak.ident } }), 3000)
    })
  }
  if (!!person?.foedselsnummer && person.foedselsnummer === sak.ident) {
    return (
      <Alert variant="warning" size="small">
        Saken ser ut til å være koblet til siste gjeldende fødselsnummer. Hendelsen kan arkiveres.
      </Alert>
    )
  }

  return (
    <div>
      <Button onClick={() => setIsOpen(true)} icon={<ArrowsCirclepathIcon aria-hidden />}>
        Oppdater ident
      </Button>
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
            <VStack gap="space-4">
              <HStack gap="space-4" justify="space-evenly">
                <div>
                  <Heading size="xsmall">Ident i sak</Heading>
                  <Tag data-color="danger" variant="outline">
                    {sak.ident}
                  </Tag>
                </div>

                {!!samsvar ? (
                  <>
                    <div>
                      <Heading size="xsmall">Ident i Grunnlag</Heading>
                      <Tag data-color="danger" variant="outline">
                        {samsvar.fraGrunnlag}
                      </Tag>
                    </div>

                    <div>
                      <Heading size="xsmall">Ident i PDL</Heading>
                      <Tag data-color="success" variant="outline">
                        {samsvar.fraPdl}
                      </Tag>
                    </div>
                  </>
                ) : (
                  <div>
                    <Heading size="xsmall">Gjeldende ident i PDL</Heading>
                    <Tag data-color="success" variant="outline">
                      {person?.foedselsnummer}
                    </Tag>
                  </div>
                )}
              </HStack>

              <Alert variant="warning">
                Endring av identifikator vil avbryte alle pågående behandlinger for å sikre at grunnlaget blir korrekt
                <br />
                Behandlinger tilknyttet klage, tilbakekreving, eller kravpakke må manuelt avbrytes.
              </Alert>

              {mapFailure(oppdaterIdentResult, (error) => (
                <ApiErrorAlert>{error.detail}</ApiErrorAlert>
              ))}

              <HStack gap="space-4" justify="center">
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
    </div>
  )
}

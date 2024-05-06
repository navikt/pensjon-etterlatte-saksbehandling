import { BodyLong, BodyShort, Button, Heading, HStack, Label, Modal, Textarea, VStack } from '@navikt/ds-react'
import React, { useContext, useState } from 'react'
import { ExternalLinkIcon } from '@navikt/aksel-icons'
import { ConfigContext } from '~clientConfig'
import { ferdigstillOppgaveMedMerknad } from '~shared/api/oppgaver'
import { useApiCall } from '~shared/hooks/useApiCall'
import { isPending } from '@reduxjs/toolkit'
import { mapFailure } from '~shared/api/apiUtils'
import { ApiErrorAlert } from '~ErrorBoundary'
import { OppgaveDTO, Oppgavestatus } from '~shared/types/oppgave'

export const AktivitetspliktInfoModal = ({ oppgave }: { oppgave: OppgaveDTO }) => {
  const [visModal, setVisModal] = useState(false)
  const [merknad, setMerknad] = useState<string | null>(null)
  const configContext = useContext(ConfigContext)

  const [ferdigstillOppgaveStatus, apiFerdigstillOppgave] = useApiCall(ferdigstillOppgaveMedMerknad)

  const ferdigstill = () => {
    apiFerdigstillOppgave({ id: oppgave.id, merknad }, () => {
      setVisModal(false)
    })
  }

  return (
    <>
      <Button size="small" onClick={() => setVisModal(true)}>
        Se oppgave
      </Button>
      {visModal && (
        <Modal
          open={visModal}
          onClose={() => setVisModal(false)}
          header={{ label: 'Oppfølging av aktivitetsplikt', heading: 'Send brev og opprett oppgave for oppfølging' }}
        >
          <Modal.Body>
            <HStack gap="12">
              <div>
                <Heading size="small" spacing>
                  Opprett informasjonbrev rundt aktivitetsplikt til bruker
                </Heading>
                <BodyLong spacing>
                  Den etterlatte skal informeres om aktivitetskravet som vil tre i kraft 6 måneder etter dødsfallet. Det
                  skal opprettes et manuelt informasjonsbrev som skal bli sendt 3-4 måneder etter dødsfallet.
                </BodyLong>
                <Button
                  variant="primary"
                  size="small"
                  as="a"
                  href={`/person/${oppgave.fnr?.toString()}?fane=BREV`}
                  target="_blank"
                >
                  Opprett manuelt brev
                </Button>
              </div>

              <div>
                <Heading size="small" spacing>
                  Lag intern oppfølgingsoppgave med frist
                </Heading>
                <BodyLong spacing>
                  Den etterlatte skal påminnes og følges opp rundt aktivitetskravet både ved 3-4 måneder etter
                  dødsfallet og på nytt ved 9-10 måneder. Andre frister bør vurderes hvis den etterlatte har andre
                  ytelser eller andre grunner som krever alternativ oppfølging.
                </BodyLong>
                <Button
                  variant="primary"
                  size="small"
                  as="a"
                  href={`${configContext['gosysUrl']}/personoversikt/fnr=${oppgave.fnr?.toString()}`}
                  target="_blank"
                >
                  Lag oppfølgingsoppgave i Gosys <ExternalLinkIcon />
                </Button>
              </div>
              {oppgave.status === Oppgavestatus.UNDER_BEHANDLING ? (
                <Textarea
                  label="Merknad"
                  description="Er det noe spesielt å merke seg ved denne saken?"
                  onChange={(e) => {
                    if (e.target.value === '') {
                      setMerknad(null)
                    } else {
                      setMerknad(e.target.value)
                    }
                  }}
                />
              ) : (
                <VStack>
                  <Label>Merknad</Label>
                  <BodyShort>{oppgave.merknad || 'Ingen merknad'}</BodyShort>
                </VStack>
              )}
            </HStack>
            {mapFailure(ferdigstillOppgaveStatus, (error) => (
              <ApiErrorAlert>{error.detail || 'Det oppsto en feil ved ferdigstilling av oppgave'}</ApiErrorAlert>
            ))}
          </Modal.Body>
          <Modal.Footer>
            {oppgave.status === Oppgavestatus.UNDER_BEHANDLING && (
              <Button loading={isPending(ferdigstillOppgaveStatus)} variant="primary" onClick={ferdigstill}>
                Ferdigstill oppgave
              </Button>
            )}
            <Button
              loading={isPending(ferdigstillOppgaveStatus)}
              variant="secondary"
              onClick={() => setVisModal(false)}
            >
              Lukk modal
            </Button>
          </Modal.Footer>
        </Modal>
      )}
    </>
  )
}

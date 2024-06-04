import { Alert, Box, Button, Detail, Heading, HStack, Modal, Tabs, VStack } from '@navikt/ds-react'
import SlateEditor from '~components/behandling/brev/SlateEditor'
import React, { useEffect, useState } from 'react'
import { FilePdfIcon, PencilIcon } from '@navikt/aksel-icons'
import { useApiCall } from '~shared/hooks/useApiCall'
import Spinner from '~shared/Spinner'
import { isPending, isSuccess, mapResult } from '~shared/api/apiUtils'
import { formaterTidspunktTimeMinutterSekunder } from '~utils/formattering'
import { hentNotatPayload, journalfoerNotat, lagreNotatPayload, Notat } from '~shared/api/notat'
import ForhaandsvisningNotat from '~components/person/notat/ForhaandsvisningNotat'
import { DokumentVisningModal } from '~shared/brev/pdf-visning'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { RedigerNotatTittel } from '~components/person/notat/RedigerNotatTittel'

enum NotatRedigeringFane {
  REDIGER = 'REDIGER',
  FORHAANDSVIS = 'FORHAANDSVIS',
}

export interface LagretStatus {
  lagret: boolean
  beskrivelse?: string
}

interface RedigerbartNotatProps {
  notat: Notat
}

export const NotatRedigeringModal = ({ notat }: RedigerbartNotatProps) => {
  const [fane, setFane] = useState<string>(NotatRedigeringFane.REDIGER)

  const [isOpen, setIsOpen] = useState(false)
  const [toggleJournalfoer, setToggleJournalfoer] = useState(false)

  const [content, setContent] = useState<any[]>([])
  const [lagretStatus, setLagretStatus] = useState<LagretStatus>({ lagret: false })

  const [hentPayloadStatus, apiHentPayload] = useApiCall(hentNotatPayload)
  const [lagrePayloadStatus, apiLagrePayload] = useApiCall(lagreNotatPayload)
  const [journalfoerStatus, apiJournalfoerNotat] = useApiCall(journalfoerNotat)

  const open = () => {
    setIsOpen(true)

    apiHentPayload(notat.id, (payload: any) => {
      console.log('payload', payload)
      setContent(payload)
    })
  }

  const lagre = () => {
    const isDirty = isSuccess(hentPayloadStatus) && hentPayloadStatus.data !== content
    if (!isDirty) {
      return
    }

    apiLagrePayload({ id: notat.id, payload: content }, () => {
      setLagretStatus({
        lagret: true,
        beskrivelse: `Sist lagret kl. ${formaterTidspunktTimeMinutterSekunder(new Date())}`,
      })
    })
  }

  const journalfoer = () => {
    apiJournalfoerNotat(notat.id, () => {
      window.location.reload()
    })
  }

  useEffect(() => {
    const delay = setTimeout(lagre, 1000)
    return () => clearTimeout(delay)
  }, [content])

  return (
    <>
      <Button variant="secondary" onClick={open} size="small" icon={<PencilIcon />}>
        Rediger
      </Button>

      <DokumentVisningModal open={isOpen} onClose={() => setIsOpen(false)} aria-labelledby="modal-heading">
        <Modal.Body>
          <Tabs value={fane} onChange={setFane}>
            <Tabs.List>
              <Tabs.Tab
                value={NotatRedigeringFane.REDIGER}
                label="Rediger"
                icon={<PencilIcon fontSize="1.5rem" aria-hidden />}
              />
              <Tabs.Tab
                value={NotatRedigeringFane.FORHAANDSVIS}
                label="Forhåndsvisning"
                icon={<FilePdfIcon fontSize="1.5rem" aria-hidden />}
              />
            </Tabs.List>

            <Tabs.Panel value={NotatRedigeringFane.REDIGER}>
              <Box paddingBlock="4">
                <VStack gap="4">
                  <RedigerNotatTittel id={notat.id} tittel={notat.tittel} />

                  <Heading size="xsmall">Rediger notat</Heading>

                  {mapResult(hentPayloadStatus, {
                    pending: <Spinner label="Henter notat ..." visible />,
                    success: () => (
                      <SlateEditor value={content} onChange={(value) => setContent(value)} readonly={false} />
                    ),
                  })}
                </VStack>
              </Box>
            </Tabs.Panel>

            <Tabs.Panel value={NotatRedigeringFane.FORHAANDSVIS}>
              <ForhaandsvisningNotat id={notat.id} />
            </Tabs.Panel>
          </Tabs>
        </Modal.Body>

        <Modal.Footer>
          {isFailureHandler({
            apiResult: lagrePayloadStatus,
            errorMessage: 'Feil oppsto ved lagring av notat',
          })}

          {toggleJournalfoer ? (
            <VStack gap="4">
              <Alert variant="warning">
                Er du sikker på at du vil journalføre notatet? Handlingen kan ikke angres.
              </Alert>

              <HStack gap="4" align="center">
                <Button variant="tertiary" onClick={() => setToggleJournalfoer(false)}>
                  Nei, avbryt
                </Button>
                <Button onClick={journalfoer} loading={isPending(journalfoerStatus)}>
                  Ja, journalfør
                </Button>
              </HStack>
            </VStack>
          ) : (
            <HStack gap="4" align="center">
              {isSuccess(lagrePayloadStatus) && lagretStatus.beskrivelse && (
                <Detail as="span">
                  <Alert variant="success" size="small" inline>
                    {lagretStatus.beskrivelse}
                  </Alert>
                </Detail>
              )}
              <Button variant="tertiary" onClick={() => setIsOpen(false)}>
                Lukk
              </Button>
              <Button
                onClick={() => {
                  setFane(NotatRedigeringFane.FORHAANDSVIS)
                  setToggleJournalfoer(true)
                }}
              >
                Journalfør
              </Button>
            </HStack>
          )}
        </Modal.Footer>
      </DokumentVisningModal>
    </>
  )
}

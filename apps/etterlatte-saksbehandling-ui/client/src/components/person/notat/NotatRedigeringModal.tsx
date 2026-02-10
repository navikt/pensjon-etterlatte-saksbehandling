import { Box, Button, Heading, Modal, Tabs, VStack } from '@navikt/ds-react'
import SlateEditor from '~components/behandling/brev/SlateEditor'
import React, { useEffect, useState } from 'react'
import { FilePdfIcon, PencilIcon } from '@navikt/aksel-icons'
import { useApiCall } from '~shared/hooks/useApiCall'
import Spinner from '~shared/Spinner'
import { isSuccess, mapResult } from '~shared/api/apiUtils'
import { hentNotatPayload, lagreNotatPayload, Notat } from '~shared/api/notat'
import ForhaandsvisningNotat from '~components/person/notat/ForhaandsvisningNotat'
import { DokumentVisningModal } from '~shared/brev/PdfVisning'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { RedigerNotatTittel } from '~components/person/notat/RedigerNotatTittel'
import { NotatHandlinger } from '~components/person/notat/NotatHandlinger'

export enum NotatRedigeringFane {
  REDIGER = 'REDIGER',
  FORHAANDSVIS = 'FORHAANDSVIS',
}

interface RedigerbartNotatProps {
  notat: Notat
  minifyRedigerKnapp?: boolean
}

export const NotatRedigeringModal = ({ notat, minifyRedigerKnapp }: RedigerbartNotatProps) => {
  const [fane, setFane] = useState<string>(NotatRedigeringFane.REDIGER)

  const [isOpen, setIsOpen] = useState(false)

  const [content, setContent] = useState<any[]>([])
  const [sistLagret, setSistLagret] = useState<Date>()

  const [hentPayloadStatus, apiHentPayload] = useApiCall(hentNotatPayload)
  const [lagrePayloadStatus, apiLagrePayload] = useApiCall(lagreNotatPayload)

  const open = () => {
    setIsOpen(true)

    apiHentPayload(notat.id, (payload: any) => {
      setContent(payload)
    })
  }

  const lagre = () => {
    const isDirty = isSuccess(hentPayloadStatus) && hentPayloadStatus.data !== content
    if (!isDirty) {
      return
    }

    apiLagrePayload({ id: notat.id, payload: content }, () => {
      setSistLagret(new Date())
    })
  }

  useEffect(() => {
    const delay = setTimeout(lagre, 1000)
    return () => clearTimeout(delay)
  }, [content])

  return (
    <>
      <Button variant="secondary" onClick={open} size="small" icon={<PencilIcon title="Rediger notat" />}>
        {!minifyRedigerKnapp && 'Rediger'}
      </Button>

      <DokumentVisningModal open={isOpen} onClose={() => setIsOpen(false)} aria-labelledby="modal-heading">
        <Modal.Header>
          <Heading size="medium">Rediger notat</Heading>
        </Modal.Header>

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
              <Box paddingBlock="space-4">
                <VStack gap="space-4">
                  <RedigerNotatTittel id={notat.id} tittel={notat.tittel} />

                  <Heading size="xsmall">Rediger notat</Heading>

                  {mapResult(hentPayloadStatus, {
                    pending: <Spinner label="Henter notat ..." />,
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

          {isFailureHandler({
            apiResult: lagrePayloadStatus,
            errorMessage: 'Feil oppsto ved lagring av notat',
          })}

          <NotatHandlinger
            notatId={notat.id}
            setFane={setFane}
            lagre={lagre}
            sistLagret={sistLagret}
            lukkModal={() => setIsOpen(false)}
          />
        </Modal.Body>
      </DokumentVisningModal>
    </>
  )
}

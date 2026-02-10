import { BodyShort, Button, Heading, HStack, Label, Modal, Radio, RadioGroup, Textarea, VStack } from '@navikt/ds-react'
import React, { useState } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { isPending, mapFailure } from '~shared/api/apiUtils'
import { ApiErrorAlert } from '~ErrorBoundary'
import { oppdaterSamordningsmeldingForSak } from '~shared/api/vedtaksvurdering'
import { DocPencilIcon } from '@navikt/aksel-icons'
import { Samordningsmelding } from '~components/vedtak/typer'
import { JaNei } from '~shared/types/ISvar'

export default function SamordningOppdaterMeldingModal({
  fnr,
  sakId,
  vedtakId,
  mld,
  refresh,
}: {
  fnr: string
  sakId: number
  vedtakId: number
  mld: Samordningsmelding
  refresh: () => void
}) {
  const [open, setOpen] = useState(false)

  const [erRefusjonskrav, setErRefusjonskrav] = useState<JaNei>()
  const [kommentar, setKommentar] = useState<string>('')
  const [oppdaterSamordningsmeldingStatus, oppdaterSamordningsmelding] = useApiCall(oppdaterSamordningsmeldingForSak)

  const oppdaterMelding = () => {
    oppdaterSamordningsmelding(
      {
        sakId: sakId,
        oppdaterSamordningsmelding: {
          samId: mld.samId,
          pid: fnr,
          tpNr: mld.tpNr,
          refusjonskrav: erRefusjonskrav === JaNei.JA,
          kommentar: kommentar,
          vedtakId: vedtakId,
        },
      },
      () => {
        setOpen(false)
        refresh()
      }
    )
  }

  return (
    <>
      <Button variant="primary" icon={<DocPencilIcon title="Overstyr" />} onClick={() => setOpen(true)} />

      <Modal open={open} aria-labelledby="modal-heading" onClose={() => setOpen(false)}>
        <Modal.Body>
          <Heading size="medium" id="modal-heading" spacing>
            Overstyr samordningsmelding
          </Heading>

          <VStack gap="space-4">
            <HStack gap="space-4">
              <Label>SamordningsmeldingID:</Label>
              <BodyShort>{mld.samId}</BodyShort>
            </HStack>
            <HStack gap="space-4">
              <Label>Tjenestepensjonsordning:</Label>
              <BodyShort>{mld.tpNr}</BodyShort>
            </HStack>

            <RadioGroup
              legend="Refusjonskrav?"
              onChange={(event) => {
                setErRefusjonskrav(JaNei[event as JaNei])
              }}
              value={erRefusjonskrav || ''}
            >
              <HStack gap="space-4">
                <Radio size="small" value={JaNei.JA}>
                  Ja
                </Radio>
                <Radio size="small" value={JaNei.NEI}>
                  Nei
                </Radio>
              </HStack>
            </RadioGroup>

            <Textarea
              label="Kommentar/dialog med TP-ordning"
              description="Det opprettes et notat på saken for å dokumentere handlingen."
              minRows={5}
              maxRows={10}
              onChange={(event) => setKommentar(event.target.value)}
            />

            <HStack gap="space-4" justify="center">
              <Button
                variant="secondary"
                onClick={() => setOpen(false)}
                disabled={isPending(oppdaterSamordningsmeldingStatus)}
              >
                Avbryt
              </Button>
              <Button
                variant="primary"
                disabled={!erRefusjonskrav || !kommentar.length}
                onClick={oppdaterMelding}
                loading={isPending(oppdaterSamordningsmeldingStatus)}
              >
                Lagre
              </Button>
            </HStack>

            {mapFailure(oppdaterSamordningsmeldingStatus, (error) => (
              <Modal.Footer>
                <ApiErrorAlert>
                  {error.detail || 'Det oppsto en feil ved oppdatering av samordningsmelding'}
                </ApiErrorAlert>
              </Modal.Footer>
            ))}
          </VStack>
        </Modal.Body>
      </Modal>
    </>
  )
}

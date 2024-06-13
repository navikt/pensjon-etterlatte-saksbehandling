import { Button, Heading, HStack, Modal } from '@navikt/ds-react'
import React, { useState } from 'react'
import { FilePdfIcon } from '@navikt/aksel-icons'
import { Notat } from '~shared/api/notat'
import ForhaandsvisningNotat from '~components/person/notat/ForhaandsvisningNotat'
import { DokumentVisningModal } from '~shared/brev/pdf-visning'

export const NotatVisningModal = ({ notat }: { notat: Notat }) => {
  const [isOpen, setIsOpen] = useState(false)

  const open = () => {
    setIsOpen(true)
  }

  return (
    <>
      <Button variant="secondary" onClick={open} size="small" icon={<FilePdfIcon />} />

      <DokumentVisningModal open={isOpen} onClose={() => setIsOpen(false)} aria-labelledby="modal-heading">
        <Modal.Body>
          <Heading size="small" spacing>
            {notat.tittel}
          </Heading>
          {isOpen && <ForhaandsvisningNotat id={notat.id} />}
        </Modal.Body>

        <Modal.Footer>
          <HStack gap="4" align="center">
            <Button variant="tertiary" onClick={() => setIsOpen(false)}>
              Lukk
            </Button>
          </HStack>
        </Modal.Footer>
      </DokumentVisningModal>
    </>
  )
}

import { BodyShort, Button, Heading, HStack, Modal } from '@navikt/ds-react'
import { Dispatch, SetStateAction } from 'react'

export type Props = {
  tittel: string
  beskrivelse?: string
  tekstKnappJa: string
  tekstKnappNei: string
  onYesClick: () => void
  setModalisOpen: Dispatch<SetStateAction<boolean>>
  open: boolean
  loading?: boolean
}

export const GeneriskModal = ({
  tittel,
  beskrivelse,
  tekstKnappJa,
  tekstKnappNei,
  onYesClick,
  setModalisOpen,
  open,
  loading,
}: Props) => {
  return (
    <Modal
      open={open}
      onClose={() => {
        setModalisOpen(false)
      }}
      aria-labelledby="modal-heading"
      className="padding-modal"
    >
      <Modal.Body>
        <Heading spacing level="1" id="modal-heading" size="medium">
          {tittel}
        </Heading>
        {beskrivelse && <BodyShort spacing>{beskrivelse}</BodyShort>}
        <HStack gap="space-4" justify="center">
          <Button variant="primary" onClick={onYesClick} loading={!!loading}>
            {tekstKnappJa}
          </Button>
          <Button
            variant="secondary"
            onClick={() => {
              setModalisOpen(false)
            }}
            disabled={!!loading}
          >
            {tekstKnappNei}
          </Button>
        </HStack>
      </Modal.Body>
    </Modal>
  )
}

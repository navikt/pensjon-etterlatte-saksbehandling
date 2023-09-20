import { BodyShort, Button, Heading, Modal } from '@navikt/ds-react'
import { FlexRow } from '~shared/styled'

export type Props = {
  tittel: string
  beskrivelse?: string
  tekstKnappJa: string
  tekstKnappNei: string
  onYesClick: () => void
  setModalisOpen: React.Dispatch<React.SetStateAction<boolean>>
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
      className={'padding-modal'}
    >
      <Modal.Body>
        <Heading spacing level="1" id="modal-heading" size="medium">
          {tittel}
        </Heading>
        {beskrivelse && <BodyShort spacing>{beskrivelse}</BodyShort>}
        <FlexRow justify={'center'}>
          <Button variant="primary" onClick={onYesClick} loading={!!loading}>
            {tekstKnappJa}
          </Button>
          <Button
            variant="secondary"
            onClick={() => {
              setModalisOpen(false)
            }}
            loading={!!loading}
          >
            {tekstKnappNei}
          </Button>
        </FlexRow>
      </Modal.Body>
    </Modal>
  )
}

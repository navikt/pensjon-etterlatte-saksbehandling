import { BodyShort, Button, Heading, Modal } from '@navikt/ds-react'
import styled from 'styled-components'

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

export const GeneriskModal: React.FC<Props> = ({
  tittel,
  beskrivelse,
  tekstKnappJa,
  tekstKnappNei,
  onYesClick,
  setModalisOpen,
  open,
  loading,
}) => {
  return (
    <Modal
      open={open}
      onClose={() => {
        setModalisOpen(false)
      }}
      aria-labelledby="modal-heading"
      className={'padding-modal'}
    >
      <Modal.Content>
        <Heading spacing level="1" id="modal-heading" size="medium">
          {tittel}
        </Heading>
        {beskrivelse && <BodyShort spacing>{beskrivelse}</BodyShort>}
        <ButtonWrapper>
          <Button
            variant="primary"
            size="medium"
            className="button"
            onClick={onYesClick}
            disabled={!!loading}
            loading={!!loading}
          >
            {tekstKnappJa}
          </Button>
          <Button
            variant="secondary"
            size="medium"
            className="button"
            onClick={() => {
              setModalisOpen(false)
            }}
            disabled={!!loading}
          >
            {tekstKnappNei}
          </Button>
        </ButtonWrapper>
      </Modal.Content>
    </Modal>
  )
}

export const ButtonWrapper = styled.div`
  display: flex;
  justify-content: center;
  gap: 1em;
`

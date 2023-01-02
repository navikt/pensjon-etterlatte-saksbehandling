import { Button, Heading, Modal } from '@navikt/ds-react'
import styled from 'styled-components'

export type Props = {
  tekst: string
  tekstKnappJa: string
  tekstKnappNei: string
  onYesClick: () => void
  setModalisOpen: React.Dispatch<React.SetStateAction<boolean>>
  open: boolean
}
export const GeneriskModal: React.FC<Props> = ({
  tekst,
  tekstKnappJa,
  tekstKnappNei,
  onYesClick,
  setModalisOpen,
  open,
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
          {tekst}
        </Heading>
        <ButtonWrapper>
          <Button variant="primary" size="medium" className="button" onClick={onYesClick}>
            {tekstKnappJa}
          </Button>
          <Button
            variant="secondary"
            size="medium"
            className="button"
            onClick={() => {
              setModalisOpen(false)
            }}
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

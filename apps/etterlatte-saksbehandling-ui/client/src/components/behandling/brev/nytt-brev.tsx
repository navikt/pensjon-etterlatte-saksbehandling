import { BodyShort, Button, Modal, Select } from "@navikt/ds-react";
import { useState } from "react";
import { Add } from "@navikt/ds-icons";
import styled from "styled-components";

const CustomModal = styled(Modal)`
  min-width: 800px;
  max-width: 1000px;
`

export default function NyttBrev() {
  const [isOpen, setIsOpen] = useState<boolean>(false)

  return (
      <>
        <Button variant={'primary'} onClick={() => setIsOpen(true)}>
          Nytt brev &nbsp;<Add/>
        </Button>

        <CustomModal open={isOpen} onClose={() => setIsOpen(false)}>
          <Modal.Content>
            <h1>Opprett nytt brev</h1>

            <BodyShort>
              Lorem ipsum dolor sit amet, consectetur adipiscing elit. Curabitur sed ante sit amet tellus aliquet
              mattis. Donec blandit, urna ac vulputate tincidunt, lorem massa tempor lectus, nec porttitor velit nunc ac
              ex. Vivamus vel elementum magna. Nullam tristique nisl sit amet ante interdum, vitae tincidunt libero
              placerat.
            </BodyShort>

            <br />
            <br />

            <Select label={'Mal'}>
              <option value={''}>Velg mal ...</option>
              <option value={'DOKUMENTASJON_VERGE'}>Dokumentasjon om vergemål</option>
            </Select>

            <br />
            <br />

            <Button variant={'primary'} style={{float: 'right'}}>
              Lagre
            </Button>
            <br />
          </Modal.Content>
        </CustomModal>
      </>
  )
}

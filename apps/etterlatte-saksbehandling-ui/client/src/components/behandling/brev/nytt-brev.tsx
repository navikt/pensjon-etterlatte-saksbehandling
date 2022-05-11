import { BodyShort, Button, Cell, Grid, Modal, Select } from "@navikt/ds-react";
import { useState } from "react";
import { Add } from "@navikt/ds-icons";
import styled from "styled-components";
import { Input } from "nav-frontend-skjema";
import { opprettBrev } from "../../../shared/api/brev";
import { useParams } from "react-router-dom";

const CustomModal = styled(Modal)`
  min-width: 800px;
  max-width: 1000px;
`

export default function NyttBrev() {
  const { behandlingId } = useParams()

  const [isOpen, setIsOpen] = useState<boolean>(false)

  const [mottaker, setMottaker] = useState<any>({})

  const opprett = () => {
    opprettBrev(behandlingId!!, mottaker)
        .then(res => console.log(res))
        .finally(() => setIsOpen(false))
  }

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

            <Grid>
              <Cell xs={6}>
                <Input label={'Fornavn'} onChange={(e) => setMottaker({ ...mottaker, fornavn: e.target.value })}/>
              </Cell>
              <Cell xs={6}>
                <Input label={'Etternavn'} onChange={(e) => setMottaker({ ...mottaker, etternavn: e.target.value })}/>
              </Cell>
            </Grid>

            <br />

            <Grid>
              <Cell xs={6}>
                <Input label={'Adresse'} onChange={(e) => setMottaker({ ...mottaker, adresse: { ...mottaker.adresse, adresse: e.target.value } })}/>
              </Cell>
              <Cell xs={3}>
                <Input label={'Postnummer'} onChange={(e) => setMottaker({ ...mottaker, adresse: { ...mottaker.adresse, postnummer: e.target.value } })}/>
              </Cell>
              <Cell xs={3}>
                <Input label={'Poststed'} onChange={(e) => setMottaker({ ...mottaker, adresse: { ...mottaker.adresse, poststed: e.target.value } })}/>
              </Cell>
            </Grid>

            <Select label={'Mal'}>
              <option value={''}>Velg mal ...</option>
              <option value={'DOKUMENTASJON_VERGE'}>Dokumentasjon om vergemål</option>
            </Select>

            <br />
            <br />

            <Button variant={'primary'} style={{float: 'right'}} onClick={opprett}>
              Lagre
            </Button>
            <br />
          </Modal.Content>
        </CustomModal>
      </>
  )
}

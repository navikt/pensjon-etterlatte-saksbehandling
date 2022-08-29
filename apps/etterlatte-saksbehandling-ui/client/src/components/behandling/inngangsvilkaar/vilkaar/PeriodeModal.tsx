import { Button, Modal, Textarea, TextField } from '@navikt/ds-react'
import { useState } from 'react'
import styled from 'styled-components'
import { DatovelgerPeriode } from './DatovelgerPeriode'

export const PeriodeModal = ({ isOpen, setIsOpen }: { isOpen: boolean; setIsOpen: (value: boolean) => void }) => {
  const [arbeidsgiver, setArbeidsgiver] = useState('')
  const [begrunnelse, setBegrunnelse] = useState('')
  const [kilde, setKilde] = useState('')

  const [fraDato, setFraDato] = useState<Date | null>(null)
  const [tilDato, setTilDato] = useState<Date | null>(null)

  return (
    <div>
      <Modal open={isOpen} onClose={() => setIsOpen(false)}>
        <Modal.Content>
          <Innhold>
            <h2>Legg til periode (Norge)</h2>
            <TextField
              style={{ padding: '10px' }}
              label="Navn på arbeidsgiver"
              value={arbeidsgiver}
              onChange={(e) => setArbeidsgiver(e.target.value)}
              size="small"
            />

            <DatoWrapper>
              <DatovelgerPeriode label={'Fra'} dato={fraDato} setDato={(dato) => setFraDato(dato)} />
              <DatovelgerPeriode label={'Til'} dato={tilDato} setDato={(dato) => setTilDato(dato)} />
            </DatoWrapper>

            <Textarea
              style={{ padding: '10px' }}
              label={'Hvorfor legger du til denne perioden?'}
              value={begrunnelse}
              onChange={(e) => setBegrunnelse(e.target.value)}
              size="small"
              minRows={3}
            />

            <TextField
              style={{ padding: '10px' }}
              label="Kilde"
              value={kilde}
              onChange={(e) => setKilde(e.target.value)}
              size="small"
            />

            <ButtonWrapper>
              <Button variant={'primary'}>Legg til periode</Button>
              <Button variant={'tertiary'} onClick={() => setIsOpen(false)}>
                Avbryt
              </Button>
            </ButtonWrapper>
          </Innhold>
        </Modal.Content>
      </Modal>
    </div>
  )
}

const Innhold = styled.div`
  display: flex;
  flex-direction: column;
  padding: 30px 40px;
  gap: 20px;
`

const ButtonWrapper = styled.div`
  margin-top: 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
`
const DatoWrapper = styled.div`
  display: flex;
`

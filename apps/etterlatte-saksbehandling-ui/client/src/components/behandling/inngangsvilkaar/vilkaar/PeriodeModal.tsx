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

  const [arbeidsgiverError, setArbeidsgiverError] = useState<string | undefined>(undefined)
  const [kildeError, setKildeError] = useState<string | undefined>(undefined)
  const [fraDatoError, setFraDatoError] = useState<string | undefined>(undefined)
  const [tilDatoError, setTilDatoError] = useState<string | undefined>(undefined)

  function valider() {
    if (arbeidsgiver === '') {
      setArbeidsgiverError('Du må skrive inn navnet på arbeidsgiver')
    } else {
      setArbeidsgiverError(undefined)
    }
    if (kilde === '') {
      setKildeError('Du må skrive inn kilden til perioden')
    } else {
      setKildeError(undefined)
    }
    if (fraDato === null) setFraDatoError('Du må velge en startdato')
    if (tilDato === null) setTilDatoError('Du må velge en sluttdato')

    if (arbeidsgiver !== '' && kilde !== '' && fraDato !== null && tilDato !== null) {
      return true
    }
  }

  function leggTilPeriodeTrykket() {
    if (valider()) {
      console.log('Legger til periode')
    }
  }

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
              onChange={(e) => {
                setArbeidsgiver(e.target.value)
                valider()
              }}
              size="small"
              error={arbeidsgiverError ? arbeidsgiverError : false}
            />

            <DatoWrapper>
              <DatovelgerPeriode
                label={'Fra'}
                dato={fraDato}
                setDato={(dato) => setFraDato(dato)}
                error={fraDatoError}
                setErrorUndefined={() => setFraDatoError(undefined)}
              />
              <DatovelgerPeriode
                label={'Til'}
                dato={tilDato}
                setDato={(dato) => setTilDato(dato)}
                error={tilDatoError}
                setErrorUndefined={() => setTilDatoError(undefined)}
              />
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
              onChange={(e) => {
                setKilde(e.target.value)
                valider()
              }}
              size="small"
              error={kildeError ? kildeError : false}
            />

            <ButtonWrapper>
              <Button variant={'primary'} onClick={leggTilPeriodeTrykket}>
                Legg til periode
              </Button>
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

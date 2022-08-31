import { Button, Modal, Select, Textarea, TextField } from '@navikt/ds-react'
import { useContext, useState } from 'react'
import styled from 'styled-components'
import { DatovelgerPeriode } from './DatovelgerPeriode'
import { IPeriodeInput, IPeriodeInputErros, IPeriodeType } from '../types'
import { hentBehandling, lagrePeriodeForAvdoedesMedlemskap } from '../../../../shared/api/behandling'
import { AppContext } from '../../../../store/AppContext'
import Spinner from '../../../../shared/Spinner'
import { ErrorResponse } from '../../felles/ErrorResponse'

export const PeriodeModal = ({ isOpen, setIsOpen }: { isOpen: boolean; setIsOpen: (value: boolean) => void }) => {
  const behandlingId = useContext(AppContext).state.behandlingReducer.id
  const [lagrer, setLagrer] = useState<boolean>(false)
  const [lagreError, setLagreError] = useState<boolean>(false)
  const [periode, setPeriode] = useState<IPeriodeInput>({
    periodeType: IPeriodeType.velg,
    arbeidsgiver: undefined,
    stillingsprosent: undefined,
    begrunnelse: '',
    kilde: '',
    fraDato: null,
    tilDato: null,
  })

  const [periodeErrors, setPeriodeErrors] = useState<IPeriodeInputErros>({
    periodeType: undefined,
    arbeidsgiver: undefined,
    stillingsprosent: undefined,
    kilde: undefined,
    fraDato: undefined,
    tilDato: undefined,
  })

  function prosentValid(input: string | undefined) {
    return input === undefined || /^(100|[1-9]?\d)%?$/.test(input)
  }

  function valider() {
    let errorObject: IPeriodeInputErros = periodeErrors
    const arbeidValid = periode.periodeType === IPeriodeType.arbeidsperiode && periode.arbeidsgiver !== undefined
    const stillingsprosentValid =
      periode.periodeType === IPeriodeType.arbeidsperiode && prosentValid(periode.stillingsprosent)

    if (periode.periodeType === IPeriodeType.velg) {
      errorObject = { ...errorObject, kilde: 'Velg type periode' }
    }
    if (periode.kilde === '') {
      errorObject = { ...errorObject, kilde: 'Skriv inn kilden til perioden' }
    }
    if (!arbeidValid) {
      errorObject = { ...errorObject, arbeidsgiver: 'Skriv inn navn på arbeidsgiver' }
    }
    if (!stillingsprosentValid) {
      errorObject = { ...errorObject, stillingsprosent: 'Skriv inn en gyldig prosent (1-100)' }
    }
    if (periode.fraDato === null) errorObject = { ...errorObject, fraDato: 'Velg en startdato' }
    if (periode.tilDato === null) errorObject = { ...errorObject, tilDato: 'Velg en sluttdato' }

    setPeriodeErrors(errorObject)

    if (
      periode.periodeType !== IPeriodeType.velg &&
      arbeidValid &&
      stillingsprosentValid &&
      periode.kilde !== '' &&
      periode.fraDato !== null &&
      periode.tilDato !== null
    ) {
      return true
    }
  }

  function leggTilPeriodeTrykket() {
    if (valider()) {
      if (!behandlingId) throw new Error('Mangler behandlingsid')
      setLagrer(true)
      lagrePeriodeForAvdoedesMedlemskap(behandlingId, periode).then((response) => {
        if (response.status === 'ok') {
          hentBehandling(behandlingId).then((response) => {
            if (response.status === 200) {
              window.location.reload()
            }
          })
        } else {
          setLagreError(true)
          setLagrer(false)
        }
      })
    }
  }

  return (
    <div>
      <Modal open={isOpen} onClose={() => setIsOpen(false)}>
        <Modal.Content>
          <Innhold>
            <Spinner visible={lagrer} label={'Lagrer periode'} />
            {lagreError && <ErrorResponse />}
            {!lagrer && (
              <>
                <h2>Legg til periode (Norge)</h2>
                <Select
                  label={'Velg periode'}
                  value={periode.periodeType}
                  onChange={(e) => {
                    setPeriode({ ...periode, periodeType: e.target.value as IPeriodeType })
                    setPeriodeErrors({ ...periodeErrors, periodeType: undefined })
                  }}
                >
                  {Object.values(IPeriodeType).map((option) => (
                    <option key={option} value={option}>
                      {option}
                    </option>
                  ))}
                </Select>

                {periode.periodeType === IPeriodeType.arbeidsperiode && (
                  <>
                    <TextField
                      style={{ padding: '10px' }}
                      label="Navn på arbeidsgiver"
                      value={periode.arbeidsgiver}
                      onChange={(e) => {
                        setPeriode({ ...periode, arbeidsgiver: e.target.value })
                        !periode.arbeidsgiver && setPeriodeErrors({ ...periodeErrors, arbeidsgiver: undefined })
                      }}
                      size="small"
                      error={periodeErrors.arbeidsgiver ? periodeErrors.arbeidsgiver : false}
                    />

                    <TextField
                      style={{ padding: '10px', maxWidth: '100px' }}
                      label="Stillingsprosent"
                      value={periode.stillingsprosent}
                      onChange={(e) => {
                        setPeriode({ ...periode, stillingsprosent: e.target.value })
                        prosentValid(e.target.value) &&
                          setPeriodeErrors({ ...periodeErrors, stillingsprosent: undefined })
                      }}
                      size="small"
                      error={periodeErrors.stillingsprosent ? periodeErrors.stillingsprosent : false}
                    />
                  </>
                )}

                <DatoWrapper>
                  <DatovelgerPeriode
                    label={'Fra'}
                    dato={periode.fraDato}
                    setDato={(dato) => setPeriode({ ...periode, fraDato: dato })}
                    error={periodeErrors.fraDato}
                    setErrorUndefined={() => setPeriodeErrors({ ...periodeErrors, fraDato: undefined })}
                  />
                  <DatovelgerPeriode
                    label={'Til'}
                    dato={periode.tilDato}
                    setDato={(dato) => setPeriode({ ...periode, tilDato: dato })}
                    error={periodeErrors.tilDato}
                    setErrorUndefined={() => setPeriodeErrors({ ...periodeErrors, tilDato: undefined })}
                  />
                </DatoWrapper>

                <Textarea
                  style={{ padding: '10px' }}
                  label={'Hvorfor legger du til denne perioden?'}
                  value={periode.begrunnelse}
                  onChange={(e) => setPeriode({ ...periode, begrunnelse: e.target.value })}
                  size="small"
                  minRows={3}
                />

                <TextField
                  style={{ padding: '10px' }}
                  label="Kilde"
                  value={periode.kilde}
                  onChange={(e) => {
                    setPeriode({ ...periode, kilde: e.target.value })
                    !periode.kilde && setPeriodeErrors({ ...periodeErrors, kilde: undefined })
                  }}
                  size="small"
                  error={periodeErrors.kilde ? periodeErrors.kilde : false}
                />

                <ButtonWrapper>
                  <Button variant={'primary'} onClick={leggTilPeriodeTrykket}>
                    Legg til periode
                  </Button>
                  <Button variant={'tertiary'} onClick={() => setIsOpen(false)}>
                    Avbryt
                  </Button>
                </ButtonWrapper>
              </>
            )}
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

import { Button, Label, Select, Textarea, Checkbox, CheckboxGroup } from '@navikt/ds-react'
import { FormKnapper, FormWrapper, Innhold } from '~components/behandling/trygdetid/styled'
import { isFailure, isPending, useApiCall } from '~shared/hooks/useApiCall'
import {
  ILand,
  ITrygdetid,
  ITrygdetidGrunnlag,
  ITrygdetidGrunnlagType,
  lagreTrygdetidgrunnlag,
  OppdaterTrygdetidGrunnlag,
} from '~shared/api/trygdetid'
import React, { FormEvent, useRef, useState } from 'react'
import { ApiErrorAlert } from '~ErrorBoundary'
import styled from 'styled-components'
import DatePicker from 'react-datepicker'
import { Calender } from '@navikt/ds-icons'
import { useParams } from 'react-router-dom'

type Props = {
  eksisterendeGrunnlag: ITrygdetidGrunnlag | undefined
  setTrygdetid: (trygdetid: ITrygdetid) => void
  avbryt: () => void
  trygdetidGrunnlagType: ITrygdetidGrunnlagType
  landListe: ILand[]
}

const initialState = (type: ITrygdetidGrunnlagType) => {
  return { type: type, bosted: '' }
}

export const TrygdetidGrunnlag: React.FC<Props> = ({
  eksisterendeGrunnlag,
  setTrygdetid,
  avbryt,
  trygdetidGrunnlagType,
  landListe,
}) => {
  const { behandlingId } = useParams()
  const [trygdetidgrunnlag, setTrygdetidgrunnlag] = useState<OppdaterTrygdetidGrunnlag>(
    eksisterendeGrunnlag ? eksisterendeGrunnlag : initialState(trygdetidGrunnlagType)
  )
  const [trygdetidgrunnlagStatus, requestLagreTrygdetidgrunnlag] = useApiCall(lagreTrygdetidgrunnlag)
  const fraDatoPickerRef: any = useRef(null)
  const tilDatoPickerRef: any = useRef(null)

  const toggleDatepicker = (ref: any) => {
    return () => {
      ref.current.setOpen(true)
      ref.current.setFocus()
    }
  }

  const onSubmit = (e: FormEvent) => {
    e.preventDefault()
    if (!behandlingId) throw new Error('Mangler behandlingsid')
    requestLagreTrygdetidgrunnlag(
      {
        behandlingsId: behandlingId,
        trygdetidgrunnlag: trygdetidgrunnlag,
      },
      (respons) => {
        const eksisterendeGrunnlag = respons.trygdetidGrunnlag.find(
          (grunnlag) => grunnlag.type === trygdetidGrunnlagType
        )
        eksisterendeGrunnlag && setTrygdetidgrunnlag(eksisterendeGrunnlag)
        setTrygdetid(respons)
      }
    )
  }

  return (
    <TrygdetidGrunnlagWrapper>
      <Innhold>
        <TrygdetidForm onSubmit={onSubmit}>
          <Rows>
            <FormWrapper>
              <Land>
                <Select
                  label="Land"
                  value={trygdetidgrunnlag.bosted}
                  key={`${trygdetidgrunnlag.bosted}-${trygdetidGrunnlagType}`}
                  onChange={(e) =>
                    setTrygdetidgrunnlag({
                      ...trygdetidgrunnlag,
                      bosted: e.target.value,
                    })
                  }
                  autoComplete="off"
                >
                  <option value="">Velg land</option>
                  {landListe.map((land) => (
                    <option key={`${land.isoLandkode}-${trygdetidGrunnlagType}`} value={land.isoLandkode}>
                      {land.beskrivelse.tekst}
                    </option>
                  ))}
                </Select>
              </Land>

              <DatoSection>
                <Label>Fra dato</Label>
                <Datovelger>
                  <DatePicker
                    ref={fraDatoPickerRef}
                    dateFormat={'dd.MM.yyyy'}
                    placeholderText={'dd.mm.åååå'}
                    selected={trygdetidgrunnlag.periodeFra == null ? null : new Date(trygdetidgrunnlag.periodeFra)}
                    locale="nb"
                    autoComplete="off"
                    onChange={(e) =>
                      setTrygdetidgrunnlag({
                        ...trygdetidgrunnlag,
                        periodeFra: e == null ? '' : e.toISOString().split('T')[0],
                      })
                    }
                  />
                  <KalenderIkon
                    tabIndex={0}
                    onKeyPress={toggleDatepicker(fraDatoPickerRef)}
                    onClick={toggleDatepicker(fraDatoPickerRef)}
                    role="button"
                    title="Åpne datovelger"
                    aria-label="Åpne datovelger"
                  >
                    <Calender color="white" />
                  </KalenderIkon>
                </Datovelger>
              </DatoSection>
              <DatoSection>
                <Label>Til dato</Label>
                <Datovelger>
                  <DatePicker
                    ref={tilDatoPickerRef}
                    dateFormat={'dd.MM.yyyy'}
                    placeholderText={'dd.mm.åååå'}
                    selected={trygdetidgrunnlag.periodeTil == null ? null : new Date(trygdetidgrunnlag.periodeTil)}
                    locale="nb"
                    autoComplete="off"
                    onChange={(e) =>
                      setTrygdetidgrunnlag({
                        ...trygdetidgrunnlag,
                        periodeTil: e == null ? '' : e.toISOString().split('T')[0],
                      })
                    }
                  />
                  <KalenderIkon
                    tabIndex={0}
                    onKeyPress={toggleDatepicker(tilDatoPickerRef)}
                    onClick={toggleDatepicker(tilDatoPickerRef)}
                    role="button"
                    title="Åpne datovelger"
                    aria-label="Åpne datovelger"
                  >
                    <Calender color="white" />
                  </KalenderIkon>
                </Datovelger>
              </DatoSection>
            </FormWrapper>

            <FormWrapper>
              <Begrunnelse
                value={trygdetidgrunnlag.begrunnelse}
                key={`begrunnelse-${trygdetidGrunnlagType}`}
                onChange={(e) =>
                  setTrygdetidgrunnlag({
                    ...trygdetidgrunnlag,
                    begrunnelse: e.target.value,
                  })
                }
              />
              {trygdetidGrunnlagType === ITrygdetidGrunnlagType.FAKTISK && (
                <PoengAar
                  legend="Poeng i inn/ut år"
                  value={[trygdetidgrunnlag.poengInnAar ? 'PIA' : '', trygdetidgrunnlag.poengUtAar ? 'PUA' : ''].filter(
                    (val) => val !== ''
                  )}
                >
                  <Checkbox
                    value="PIA"
                    key={`poeng-inn-aar-${trygdetidGrunnlagType}`}
                    onChange={() => {
                      setTrygdetidgrunnlag({
                        ...trygdetidgrunnlag,
                        poengInnAar: !trygdetidgrunnlag.poengInnAar!!,
                      })
                    }}
                  >
                    Poeng i inn år
                  </Checkbox>
                  <Checkbox
                    value="PUA"
                    key={`poeng-ut-aar-${trygdetidGrunnlagType}`}
                    onChange={() =>
                      setTrygdetidgrunnlag({
                        ...trygdetidgrunnlag,
                        poengUtAar: !trygdetidgrunnlag.poengUtAar!!,
                      })
                    }
                  >
                    Poeng i ut år
                  </Checkbox>
                </PoengAar>
              )}
            </FormWrapper>

            <FormKnapper>
              <Button size="small" loading={isPending(trygdetidgrunnlagStatus)} type="submit">
                Lagre
              </Button>
              <Button
                size="small"
                onClick={(event) => {
                  event.preventDefault()
                  avbryt()
                }}
              >
                Avbryt
              </Button>
            </FormKnapper>
          </Rows>
        </TrygdetidForm>
      </Innhold>

      {isFailure(trygdetidgrunnlagStatus) && <ApiErrorAlert>En feil har oppstått</ApiErrorAlert>}
    </TrygdetidGrunnlagWrapper>
  )
}

const TrygdetidGrunnlagWrapper = styled.div`
  padding: 2em 0 0 0;
`

const TrygdetidForm = styled.form`
  display: flex;
`

const Rows = styled.div`
  flex-direction: column;
`

const Land = styled.div`
  width: 250px;
`

const Datovelger = styled.div`
  display: flex;
  align-items: flex-start;

  input {
    border-right: none;
    border-width: 1px;
    border-radius: 4px 0 0 4px;
    width: 160px;
    height: 48px;
    text-indent: 4px;
  }
`

const KalenderIkon = styled.div`
  padding: 4px 10px;
  cursor: pointer;
  background-color: #0167c5;
  border: 1px solid #000;
  border-radius: 0 4px 4px 0;
  height: 48px;
  line-height: 42px;
`

const DatoSection = styled.section`
  display: grid;
  gap: 0.5em;
`
export const Begrunnelse = styled(Textarea).attrs({
  label: 'Begrunnelse',
  hideLabel: false,
  placeholder: 'Valgfritt',
  minRows: 3,
  autoComplete: 'off',
})`
  margin-bottom: 10px;
  margin-top: 10px;
  width: 250px;
`

export const PoengAar = styled(CheckboxGroup)`
  margin-bottom: 10px;
  margin-top: 10px;
  width: 200px;
`

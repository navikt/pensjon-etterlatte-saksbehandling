import { Button, Heading, Label, Select, TextField } from '@navikt/ds-react'
import { FormKnapper, FormWrapper, Innhold } from '~components/behandling/trygdetid/styled'
import { isFailure, isPending, useApiCall } from '~shared/hooks/useApiCall'
import { ITrygdetid, ITrygdetidGrunnlag, ITrygdetidGrunnlagType, lagreTrygdetidgrunnlag } from '~shared/api/trygdetid'
import React, { FormEvent, useRef, useState } from 'react'
import { ApiErrorAlert } from '~ErrorBoundary'
import styled from 'styled-components'
import DatePicker from 'react-datepicker'
import { Calender } from '@navikt/ds-icons'
import { useParams } from 'react-router-dom'

type Props = {
  trygdetid: ITrygdetid
  setTrygdetid: (trygdetid: ITrygdetid) => void
  trygdetidGrunnlagType: ITrygdetidGrunnlagType
}

const initialState = (type: ITrygdetidGrunnlagType) => {
  return {
    type: type,
    bosted: '',
    kilde: '',
  }
}

export const TrygdetidGrunnlag: React.FC<Props> = ({ trygdetid, setTrygdetid, trygdetidGrunnlagType }) => {
  const { behandlingId } = useParams()
  const eksisterendeGrunnlag = trygdetid.trygdetidGrunnlag.find((grunnlag) => grunnlag.type === trygdetidGrunnlagType)
  const [trygdetidgrunnlag, setTrygdetidgrunnlag] = useState<ITrygdetidGrunnlag>(
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

  const dager = eksisterendeGrunnlag ? eksisterendeGrunnlag.trygdetid : 0

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
      <Heading spacing size="small" level="3">
        {
          {
            [ITrygdetidGrunnlagType.NASJONAL]: 'Faktisk trygdetid',
            [ITrygdetidGrunnlagType.FREMTIDIG]: 'Fremtidig trygdetid',
          }[trygdetidGrunnlagType]
        }
      </Heading>
      <Innhold>
        <TrygdetidForm onSubmit={onSubmit}>
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
                <option key={`NORGE-${trygdetidGrunnlagType}`} value="NORGE">
                  Norge
                </option>
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
            <TrygdetidInput>
              <TextField
                label={
                  trygdetidGrunnlagType === ITrygdetidGrunnlagType.FREMTIDIG
                    ? 'Fremtidig trygdetid'
                    : 'Faktisk trygdetid'
                }
                disabled={true}
                size="medium"
                type="text"
                inputMode="numeric"
                pattern="[0-9]*"
                value={dager}
                onChange={(e) =>
                  setTrygdetidgrunnlag({
                    ...trygdetidgrunnlag,
                    trygdetid: Number(e.target.value),
                  })
                }
              />
            </TrygdetidInput>
            <Kilde>
              <TextField
                label="Kilde"
                size="medium"
                type="text"
                value={trygdetidgrunnlag.kilde}
                onChange={(e) =>
                  setTrygdetidgrunnlag({
                    ...trygdetidgrunnlag,
                    kilde: e.target.value,
                  })
                }
              />
            </Kilde>
          </FormWrapper>
          <FormKnapper>
            <Button size="medium" loading={isPending(trygdetidgrunnlagStatus)} type="submit">
              Lagre
            </Button>
          </FormKnapper>
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

const Land = styled.div`
  width: 250px;
`

const Kilde = styled.div`
  width: 250px;
`

const TrygdetidInput = styled.div`
  width: 250px;
`

const Datovelger = styled.div`
  display: flex;
  align-items: flex-end;

  input {
    border-right: none;
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

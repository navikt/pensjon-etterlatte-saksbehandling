import { Button, Heading, Label, Select } from '@navikt/ds-react'
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

  const beregnetTrygdetid = eksisterendeGrunnlag
    ? `${eksisterendeGrunnlag.beregnet?.aar} år ${eksisterendeGrunnlag.beregnet?.maaneder} måneder ${eksisterendeGrunnlag.beregnet?.dager} dager`
    : '-'

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
      {
        {
          [ITrygdetidGrunnlagType.NASJONAL]: (
            <>
              <Heading spacing size="small" level="3">
                Faktisk trygdetid
              </Heading>
              <p>Legg til trygdetid fra avdøde var 16 år frem til hen døde.</p>
            </>
          ),
          [ITrygdetidGrunnlagType.FREMTIDIG]: (
            <>
              <Heading spacing size="small" level="3">
                Fremtidig trygdetid
              </Heading>
              <p>Legg til trygdetid fra dødsdato til og med kalenderåret avdøde hadde blitt 66 år.</p>
            </>
          ),
        }[trygdetidGrunnlagType]
      }
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
              {trygdetidGrunnlagType !== ITrygdetidGrunnlagType.FREMTIDIG && (
                <Kilde>
                  <Select
                    label="Kilde til informasjon"
                    value={trygdetidgrunnlag.kilde}
                    key={`${trygdetidgrunnlag.kilde}-${trygdetidGrunnlagType}`}
                    onChange={(e) =>
                      setTrygdetidgrunnlag({
                        ...trygdetidgrunnlag,
                        kilde: e.target.value,
                      })
                    }
                    autoComplete="off"
                  >
                    <option value="">Velg kilde</option>
                    <option key={`FOLKEREGISTRERET}`} value="FOLKEREGISTRERET">
                      Folkeregisteret
                    </option>
                  </Select>
                </Kilde>
              )}
              <TrygdetidBeregnet>
                <Label>
                  {trygdetidGrunnlagType === ITrygdetidGrunnlagType.FREMTIDIG
                    ? 'Fremtidig trygdetid'
                    : 'Faktisk trygdetid'}
                </Label>
                <div>{beregnetTrygdetid}</div>
              </TrygdetidBeregnet>
            </FormWrapper>

            <FormKnapper>
              <Button size="small" loading={isPending(trygdetidgrunnlagStatus)} type="submit">
                Lagre
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

const Kilde = styled.div`
  width: 250px;
`

const TrygdetidBeregnet = styled.div`
  width: 220px;
  display: grid;
  gap: 0.5em;
`

const Datovelger = styled.div`
  display: flex;
  align-items: flex-end;

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

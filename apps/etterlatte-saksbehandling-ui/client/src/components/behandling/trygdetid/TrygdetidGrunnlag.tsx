import { ContentHeader } from '~shared/styled'
import { Button, Heading, TextField } from '@navikt/ds-react'
import { FormKnapper, FormWrapper, Innhold } from '~components/behandling/trygdetid/styled'
import { isFailure, isPending, useApiCall } from '~shared/hooks/useApiCall'
import { ITrygdetid, ITrygdetidGrunnlag, ITrygdetidType, lagreTrygdetidgrunnlag } from '~shared/api/trygdetid'
import React, { FormEvent, useRef, useState } from 'react'
import { ApiErrorAlert } from '~ErrorBoundary'
import styled from 'styled-components'
import DatePicker from 'react-datepicker'
import { Calender } from '@navikt/ds-icons'
import { useParams } from 'react-router-dom'

type Props = {
  trygdetid: ITrygdetid
  setTrygdetid: (trygdetid: ITrygdetid) => void
  erFremtidigTrygdetid?: boolean
}
export const TrygdetidGrunnlag: React.FC<Props> = ({ trygdetid, setTrygdetid, erFremtidigTrygdetid = false }) => {
  const { behandlingId } = useParams()

  const [nyttTrygdetidgrunnlag, setNyttTrygdetidgrunnlag] = useState<ITrygdetidGrunnlag>({
    type: erFremtidigTrygdetid ? ITrygdetidType.FREMTIDIG_TRYGDETID : ITrygdetidType.NASJONAL_TRYGDETID,
    bosted: '',
    periodeFra: null,
    periodeTil: null,
  })
  const [trygdetidgrunnlagStatus, requestLagreTrygdetidgrunnlag] = useApiCall(lagreTrygdetidgrunnlag)

  const fraDatoPickerRef: any = useRef(null)
  const tilDatoPickerRef: any = useRef(null)

  const onSubmit = (e: FormEvent) => {
    e.preventDefault()
    if (!behandlingId) throw new Error('Mangler behandlingsid')
    requestLagreTrygdetidgrunnlag(
      {
        behandlingsId: behandlingId,
        nyttTrygdetidgrunnlag: nyttTrygdetidgrunnlag!,
      },
      (respons) => {
        setTrygdetid(respons)
      }
    )
  }

  const relevantGrunnlag = (grunnlag: ITrygdetidGrunnlag) => {
    if (erFremtidigTrygdetid) {
      return grunnlag.type === ITrygdetidType.FREMTIDIG_TRYGDETID
    } else {
      return grunnlag.type !== ITrygdetidType.FREMTIDIG_TRYGDETID
    }
  }

  return (
    <>
      <ContentHeader>
        <Heading spacing size="medium" level="3">
          {erFremtidigTrygdetid ? 'Fremtidig trygdetid' : 'Faktisk trygdetid'}
        </Heading>
      </ContentHeader>
      <Innhold>
        <OppsummeringListe>
          {trygdetid.grunnlag.filter(relevantGrunnlag).map((grunnlag) => (
            <li key={grunnlag.bosted}>
              {grunnlag.bosted} fra {grunnlag.periodeTil} til {grunnlag.periodeTil}
            </li>
          ))}
        </OppsummeringListe>
        <TrygdetidForm onSubmit={onSubmit}>
          <FormWrapper>
            <TextField
              label="Bosted"
              size="small"
              type="text"
              value={nyttTrygdetidgrunnlag?.bosted}
              onChange={(e) =>
                setNyttTrygdetidgrunnlag({
                  ...nyttTrygdetidgrunnlag,
                  bosted: e.target.value,
                })
              }
            />

            <Datovelger>
              <div>
                <DatoLabel>Fra dato</DatoLabel>
                <DatePicker
                  ref={tilDatoPickerRef}
                  dateFormat={'dd.MM.yyyy'}
                  placeholderText={'dd.mm.åååå'}
                  selected={
                    nyttTrygdetidgrunnlag?.periodeFra == null ? null : new Date(nyttTrygdetidgrunnlag.periodeFra)
                  }
                  locale="nb"
                  autoComplete="off"
                  showMonthYearPicker
                  onChange={(e) =>
                    setNyttTrygdetidgrunnlag({
                      ...nyttTrygdetidgrunnlag,
                      periodeFra: e == null ? '' : e.toISOString().split('T')[0],
                    })
                  }
                />
              </div>
              <KalenderIkon tabIndex={0} role="button" title="Åpne datovelger" aria-label="Åpne datovelger">
                <Calender color="white" />
              </KalenderIkon>{' '}
            </Datovelger>
            <Datovelger>
              <div>
                <DatoLabel>Til dato</DatoLabel>
                <DatePicker
                  ref={fraDatoPickerRef}
                  dateFormat={'dd.MM.yyyy'}
                  placeholderText={'dd.mm.åååå'}
                  selected={
                    nyttTrygdetidgrunnlag?.periodeTil == null ? null : new Date(nyttTrygdetidgrunnlag.periodeTil)
                  }
                  locale="nb"
                  autoComplete="off"
                  showMonthYearPicker
                  onChange={(e) =>
                    setNyttTrygdetidgrunnlag({
                      ...nyttTrygdetidgrunnlag,
                      periodeTil: e == null ? '' : e.toISOString().split('T')[0],
                    })
                  }
                />
              </div>
              <KalenderIkon tabIndex={0} role="button" title="Åpne datovelger" aria-label="Åpne datovelger">
                <Calender color="white" />
              </KalenderIkon>{' '}
            </Datovelger>
          </FormWrapper>
          <FormKnapper>
            <Button loading={isPending(trygdetidgrunnlagStatus)} type="submit">
              Lagre
            </Button>
          </FormKnapper>
        </TrygdetidForm>
      </Innhold>

      {isFailure(trygdetidgrunnlagStatus) && <ApiErrorAlert>En feil har oppstått</ApiErrorAlert>}
    </>
  )
}

const OppsummeringListe = styled.ul`
  margin: 0 0 2em 2em;
`
const TrygdetidForm = styled.form`
  display: flex;
`

const Datovelger = styled.div`
  display: flex;
  align-items: flex-end;
  margin-bottom: 12px;
  align-self: flex-end;

  input {
    border-right: none;
    border-radius: 4px 0 0 4px;
    height: 48px;
    text-indent: 4px;
  }
`
const DatoLabel = styled.label`
  max-width: 1em;
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

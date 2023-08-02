import { Button, Select, Textarea, Checkbox, CheckboxGroup } from '@navikt/ds-react'
import { FormKnapper, FormWrapper, Innhold } from '~components/behandling/trygdetid/styled'
import { isConflict, isFailure, isPending, useApiCall } from '~shared/hooks/useApiCall'
import {
  ILand,
  ITrygdetid,
  ITrygdetidGrunnlag,
  ITrygdetidGrunnlagType,
  lagreTrygdetidgrunnlag,
  OppdaterTrygdetidGrunnlag,
} from '~shared/api/trygdetid'
import React, { FormEvent, useState } from 'react'
import { ApiErrorAlert } from '~ErrorBoundary'
import styled from 'styled-components'
import { useParams } from 'react-router-dom'
import { format } from 'date-fns'
import { DatoVelger } from '~shared/DatoVelger'

type Props = {
  eksisterendeGrunnlag: ITrygdetidGrunnlag | undefined
  setTrygdetid: (trygdetid: ITrygdetid) => void
  avbryt: () => void
  trygdetidGrunnlagType: ITrygdetidGrunnlagType
  landListe: ILand[]
}

const initialState = (type: ITrygdetidGrunnlagType) => {
  return { type: type, bosted: '', poengInnAar: false, poengUtAar: false, prorata: false }
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
                <DatoVelger
                  value={trygdetidgrunnlag.periodeFra == null ? null : new Date(trygdetidgrunnlag.periodeFra)}
                  onChange={(date: Date | null) =>
                    setTrygdetidgrunnlag({
                      ...trygdetidgrunnlag,
                      periodeFra: date == null ? '' : format(date, 'yyyy-MM-dd'),
                    })
                  }
                  label="Fra dato"
                />
              </DatoSection>
              <DatoSection>
                <DatoVelger
                  value={trygdetidgrunnlag.periodeTil == null ? null : new Date(trygdetidgrunnlag.periodeTil)}
                  onChange={(date: Date | null) =>
                    setTrygdetidgrunnlag({
                      ...trygdetidgrunnlag,
                      periodeTil: date == null ? '' : format(date, 'yyyy-MM-dd'),
                    })
                  }
                  label="Til dato"
                />
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
                <>
                  <PoengAar
                    legend="Poeng i inn/ut år"
                    value={[
                      trygdetidgrunnlag.poengInnAar ? 'PIA' : '',
                      trygdetidgrunnlag.poengUtAar ? 'PUA' : '',
                    ].filter((val) => val !== '')}
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

                  <Prorata
                    legend="Prorata"
                    value={[trygdetidgrunnlag.prorata ? 'PRORATA' : ''].filter((val) => val !== '')}
                  >
                    <Checkbox
                      value="PRORATA"
                      key={`prorata-${trygdetidGrunnlagType}`}
                      onChange={() => {
                        setTrygdetidgrunnlag({
                          ...trygdetidgrunnlag,
                          prorata: !trygdetidgrunnlag.prorata!!,
                        })
                      }}
                    >
                      Med i prorata
                    </Checkbox>
                  </Prorata>
                </>
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

      {isFailure(trygdetidgrunnlagStatus) && (
        <ApiErrorAlert>
          {isConflict(trygdetidgrunnlagStatus)
            ? 'Trygdetidsperioder kan ikke være overlappende'
            : 'En feil har oppstått'}
        </ApiErrorAlert>
      )}
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

export const Prorata = styled(CheckboxGroup)`
  margin-bottom: 10px;
  margin-top: 10px;
  width: 200px;
`

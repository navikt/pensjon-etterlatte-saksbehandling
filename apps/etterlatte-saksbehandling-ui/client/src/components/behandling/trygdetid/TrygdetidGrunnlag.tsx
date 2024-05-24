import { Button, Checkbox, CheckboxGroup, Select, Textarea } from '@navikt/ds-react'
import { FormKnapper, FormWrapper, Innhold } from '~components/behandling/trygdetid/styled'
import { useApiCall } from '~shared/hooks/useApiCall'
import {
  ILand,
  ITrygdetid,
  ITrygdetidGrunnlag,
  ITrygdetidGrunnlagType,
  lagreTrygdetidgrunnlag,
  OppdaterTrygdetidGrunnlag,
} from '~shared/api/trygdetid'
import React from 'react'
import { ApiErrorAlert } from '~ErrorBoundary'
import styled from 'styled-components'
import { useParams } from 'react-router-dom'
import { isPending, mapFailure } from '~shared/api/apiUtils'
import { useForm } from 'react-hook-form'
import { ControlledDatoVelger } from '~shared/components/datoVelger/ControlledDatoVelger'

type Props = {
  eksisterendeGrunnlag: ITrygdetidGrunnlag | undefined
  trygdetidId: string
  setTrygdetid: (trygdetid: ITrygdetid) => void
  avbryt: () => void
  trygdetidGrunnlagType: ITrygdetidGrunnlagType
  landListe: ILand[]
}

const initialState = (type: ITrygdetidGrunnlagType) => {
  return { type: type, bosted: '', poengInnAar: false, poengUtAar: false, prorata: false }
}

export const TrygdetidGrunnlag = ({
  trygdetidId,
  eksisterendeGrunnlag,
  setTrygdetid,
  avbryt,
  trygdetidGrunnlagType,
  landListe,
}: Props) => {
  const { behandlingId } = useParams()

  const {
    register,
    handleSubmit,
    formState: { errors },
    control,
    getValues,
  } = useForm<OppdaterTrygdetidGrunnlag>({
    defaultValues: eksisterendeGrunnlag
      ? { ...eksisterendeGrunnlag, prorata: !eksisterendeGrunnlag.prorata }
      : initialState(trygdetidGrunnlagType),
  })

  const [trygdetidgrunnlagStatus, requestLagreTrygdetidgrunnlag] = useApiCall(lagreTrygdetidgrunnlag)

  const onSubmit = (data: OppdaterTrygdetidGrunnlag) => {
    if (!behandlingId) throw new Error('Mangler behandlingsid')
    requestLagreTrygdetidgrunnlag(
      {
        behandlingId,
        trygdetidId,
        // Flippe verdi av prorata for å matche backend
        trygdetidgrunnlag: { ...data, prorata: !data.prorata },
      },
      (respons) => {
        setTrygdetid(respons)
      }
    )
  }

  return (
    <TrygdetidGrunnlagWrapper>
      <Innhold>
        <TrygdetidForm onSubmit={handleSubmit((data) => onSubmit(data))}>
          <Rows>
            <FormWrapper>
              <Land>
                <Select
                  {...register('bosted', {
                    required: {
                      value: true,
                      message: 'Obligatorisk',
                    },
                  })}
                  label="Land"
                  key={`${getValues().bosted}-${trygdetidGrunnlagType}`}
                  autoComplete="off"
                  error={errors.bosted?.message}
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
                <ControlledDatoVelger
                  name="periodeFra"
                  label="Fra dato"
                  control={control}
                  errorVedTomInput="Obligatorisk"
                />
              </DatoSection>
              <DatoSection>
                <ControlledDatoVelger
                  name="periodeTil"
                  label="Til dato"
                  control={control}
                  errorVedTomInput="Obligatorisk"
                />
              </DatoSection>
            </FormWrapper>

            <FormWrapper>
              <Begrunnelse
                {...register('begrunnelse')}
                key={`begrunnelse-${trygdetidGrunnlagType}`}
                label="Begrunnelse"
                placeholder="Valgfritt"
                minRows={3}
                autoComplete="off"
              />
              {trygdetidGrunnlagType === ITrygdetidGrunnlagType.FAKTISK && (
                <>
                  <PoengAar legend="Poeng i inn/ut år">
                    {/* Stoppe aksel å klage på at checkbox ikke har value, mens RHF styrer den */}
                    {/* Hvis man setter verdien fra RHF i Aksel Checkbox vil den overridet til string */}
                    <Checkbox {...register('poengInnAar')} value="">
                      Poeng i inn år
                    </Checkbox>
                    <Checkbox {...register('poengUtAar')} value="">
                      Poeng i ut år
                    </Checkbox>
                  </PoengAar>

                  <Prorata legend="Prorata">
                    <Checkbox {...register('prorata')} value="">
                      Ikke med i prorata
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

      {mapFailure(trygdetidgrunnlagStatus, (error) =>
        error.status === 409 ? (
          <ApiErrorAlert>Trygdetidsperioder kan ikke være overlappende</ApiErrorAlert>
        ) : (
          <ApiErrorAlert>{error.detail}</ApiErrorAlert>
        )
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
export const Begrunnelse = styled(Textarea)`
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

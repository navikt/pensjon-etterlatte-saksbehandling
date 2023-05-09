import { BodyShort, Button, ErrorMessage, Heading, Label, ReadMore, Select, TextField } from '@navikt/ds-react'
import styled from 'styled-components'
import DatePicker from 'react-datepicker'
import { Calender } from '@navikt/ds-icons'
import React, { FormEvent, useRef, useState } from 'react'
import { IAvkorting, IAvkortingGrunnlag } from '~shared/types/IAvkorting'
import { isPending, useApiCall } from '~shared/hooks/useApiCall'
import { lagreAvkortingGrunnlag } from '~shared/api/avkorting'
import { useParams } from 'react-router-dom'
import { formaterDatoTilYearMonth, formaterStringDato } from '~utils/formattering'
import { HjemmelLenke } from '~components/behandling/felles/HjemmelLenke'
import { Info } from '~components/behandling/soeknadsoversikt/Info'

export const AvkortingInntekt = (props: {
  avkortingGrunnlag?: IAvkortingGrunnlag[]
  setAvkorting: (avkorting: IAvkorting) => void
}) => {
  const { behandlingId } = useParams()
  const [inntektGrunnlag, setInntektGrunnlag] = useState<IAvkortingGrunnlag>(
    props.avkortingGrunnlag ? props.avkortingGrunnlag[0] : {}
  )
  const [inntektGrunnlagStatus, requestLagreAvkortingGrunnlag] = useApiCall(lagreAvkortingGrunnlag)
  const [errorTekst, setErrorTekst] = useState<string | null>(null)

  const fomPickerRef: any = useRef(null)
  const toggleDatepicker = (ref: any) => {
    return () => {
      ref.current.setOpen(true)
      ref.current.setFocus()
    }
  }

  const onSubmit = (e: FormEvent) => {
    e.preventDefault()

    setErrorTekst('')
    if (inntektGrunnlag.aarsinntekt == null) return setErrorTekst('Årsinntekt må fylles ut')
    if (inntektGrunnlag.gjeldendeAar === 0) return setErrorTekst('Gjeldende år må fylles ut')
    if (inntektGrunnlag.fom == null) return setErrorTekst('Fra og med dato må fylles ut')

    if (!behandlingId) throw new Error('Mangler behandlingsid')
    requestLagreAvkortingGrunnlag({ behandlingId: behandlingId, avkortingGrunnlag: inntektGrunnlag }, (respons) => {
      const nyttAvkortingGrunnlag = respons.avkortingGrunnlag[0]
      nyttAvkortingGrunnlag && setInntektGrunnlag(nyttAvkortingGrunnlag)
      props.setAvkorting(respons)
    })
  }

  return (
    <AvkortingInntektWrapper>
      <Heading spacing size="small" level="2">
        Inntektsavkorting
      </Heading>
      <HjemmelLenke
        tittel={'Folketrygdloven § 17-9 (mangler lenke)'}
        lenke={'https://lovdata.no/lov/'} // TODO lenke finnes ikke enda
      />
      <BodyShort>
        Omstillingsstønaden reduseres med 45 prosent av den gjenlevende sin inntekt som på årsbasis overstiger et halvt
        grunnbeløp. Inntekt avrundes til nærmeste tusen.
      </BodyShort>
      <InntektAvkortingForm onSubmit={onSubmit}>
        <Rows>
          <FormWrapper>
            <TextField
              label={'Forventet årsinnekt'}
              size="medium"
              type="text"
              inputMode="numeric"
              pattern="[0-9]*"
              value={inntektGrunnlag.aarsinntekt}
              onChange={(e) =>
                setInntektGrunnlag({
                  ...inntektGrunnlag,
                  aarsinntekt: Number(e.target.value),
                })
              }
            />
            <Select
              label="År"
              value={inntektGrunnlag.gjeldendeAar}
              key={`INNTEKT-${inntektGrunnlag.gjeldendeAar}`}
              onChange={(e) =>
                setInntektGrunnlag({
                  ...inntektGrunnlag,
                  gjeldendeAar: e.target.value === 'Velg inntektsår' ? 0 : Number(e.target.value),
                })
              }
              autoComplete="off"
            >
              <option value="">Velg inntektsår</option>
              {Array.from({ length: 10 }, (_, i) => new Date().getFullYear() - i).map((aar) => (
                <option key={`INNTEKT-${aar}`} value={aar}>
                  {aar}
                </option>
              ))}
            </Select>
            <DatoSection>
              <Label>F.o.m dato (inntekt)</Label>
              <Datovelger>
                <DatePicker
                  ref={fomPickerRef}
                  dateFormat={'dd.MM.yyyy'}
                  placeholderText={'dd.mm.åååå'}
                  selected={inntektGrunnlag.fom == null ? null : new Date(inntektGrunnlag.fom)}
                  locale="nb"
                  autoComplete="off"
                  showMonthYearPicker
                  onChange={(date) =>
                    setInntektGrunnlag({
                      ...inntektGrunnlag,
                      fom: date == null ? undefined : formaterDatoTilYearMonth(date),
                    })
                  }
                />
                <KalenderIkon
                  tabIndex={0}
                  onKeyPress={toggleDatepicker(fomPickerRef)}
                  onClick={toggleDatepicker(fomPickerRef)}
                  role="button"
                  title="Åpne datovelger"
                  aria-label="Åpne datovelger"
                >
                  <Calender color="white" />
                </KalenderIkon>
              </Datovelger>
            </DatoSection>
            {inntektGrunnlag.kilde && (
              <Kilde>
                <Label>Kilde</Label>
                <Info
                  tekst={inntektGrunnlag.kilde.ident}
                  label={''}
                  undertekst={`saksbehandler: ${formaterStringDato(inntektGrunnlag.kilde.tidspunkt)}`}
                />
              </Kilde>
            )}
          </FormWrapper>
          {errorTekst == null ? null : <ErrorMessage>{errorTekst}</ErrorMessage>}
          <TextAreaWrapper>
            <SpesifikasjonLabel>
              <Label>Spesifikasjon av inntekt</Label>
              <ReadMore header={'Hva regnes som inntekt?'}>
                Med inntekt menes all arbeidsinntekt og ytelser som likestilles med arbeidsinntekt. Likestilt med
                arbeidsinntekt er dagpenger etter kap 4, sykepenger etter kap 8, stønad ved barns og andre nærståendes
                sykdom etter kap 9, arbeidsavklaringspenger etter kap 11, uføretrygd etter kap 12 der uføregraden er
                under 100 prosent, svangerskapspenger og foreldrepenger etter kap 14 og pensjonsytelser etter AFP
                tilskottloven kapitlene 2 og 3.
              </ReadMore>
            </SpesifikasjonLabel>
            <textarea
              value={inntektGrunnlag.spesifikasjon}
              onChange={(e) =>
                setInntektGrunnlag({
                  ...inntektGrunnlag,
                  spesifikasjon: e.target.value,
                })
              }
            />
          </TextAreaWrapper>
          <FormKnapper>
            <Button size="small" loading={isPending(inntektGrunnlagStatus)} type="submit">
              Lagre
            </Button>
          </FormKnapper>
        </Rows>
      </InntektAvkortingForm>
    </AvkortingInntektWrapper>
  )
}

const AvkortingInntektWrapper = styled.div`
  width: 50em;
  margin-bottom: 3em;
`

const Kilde = styled.div`
  width: 2em;
  display: grid;
  gap: 0.5em;
`

const InntektAvkortingForm = styled.form`
  display: flex;
  margin: 1em 0 1em 0;
`

const FormWrapper = styled.div`
  display: flex;
  gap: 1rem;
  margin-top: 2em;
  margin-bottom: 2em;
`

const FormKnapper = styled.div`
  margin-top: 1rem;
  margin-right: 1em;
  gap: 1rem;
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

const TextAreaWrapper = styled.div`
  display: grid;
  align-items: flex-end;
  margin-top: 2em;

  textArea {
    margin-top: 1em;
    border-width: 1px;
    border-radius: 4px 4px 0 4px;
    width: 46em;
    height: 98px;
    text-indent: 4px;
    resize: none;
  }
`

const SpesifikasjonLabel = styled.div``

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
  margin-left: 4.5em;
`
const Rows = styled.div`
  flex-direction: column;
`

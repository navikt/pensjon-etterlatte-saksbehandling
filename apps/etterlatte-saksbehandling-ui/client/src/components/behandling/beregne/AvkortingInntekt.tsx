import { BodyShort, Button, Label, Select, Textarea, TextField } from '@navikt/ds-react'
import styled from 'styled-components'
import { FormKnapper, FormWrapper } from '~components/behandling/trygdetid/styled'
import DatePicker from 'react-datepicker'
import { Calender } from '@navikt/ds-icons'
import React, { FormEvent, useRef, useState } from 'react'
import { IAvkorting, IAvkortingGrunnlag } from '~shared/types/IAvkorting'
import { isPending, useApiCall } from '~shared/hooks/useApiCall'
import { lagreAvkortingGrunnlag } from '~shared/api/avkorting'
import { useParams } from 'react-router-dom'
import { formaterDatoTilYearMonth } from '~utils/formattering'
import { HjemmelLenke } from '~components/behandling/felles/HjemmelLenke'

export const AvkortingInntekt = (props: {
  avkortingGrunnlag?: IAvkortingGrunnlag[]
  setAvkorting: (avkorting: IAvkorting) => void
}) => {
  const { behandlingId } = useParams()
  const [inntektGrunnlag, setInntektGrunnlag] = useState<IAvkortingGrunnlag>(
    props.avkortingGrunnlag ? props.avkortingGrunnlag[0] : {}
  )
  const [inntektGrunnlagStatus, requestLagreAvkortingGrunnlag] = useApiCall(lagreAvkortingGrunnlag)

  const fomPickerRef: any = useRef(null)
  const toggleDatepicker = (ref: any) => {
    return () => {
      ref.current.setOpen(true)
      ref.current.setFocus()
    }
  }

  const onSubmit = (e: FormEvent) => {
    e.preventDefault()
    if (!behandlingId) throw new Error('Mangler behandlingsid')
    requestLagreAvkortingGrunnlag({ behandlingId: behandlingId, avkortingGrunnlag: inntektGrunnlag }, (respons) => {
      const nyttAvkortingGrunnlag = respons.avkortingGrunnlag[0]
      nyttAvkortingGrunnlag && setInntektGrunnlag(nyttAvkortingGrunnlag)
      props.setAvkorting(respons)
    })
  }

  return (
    <div>
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
              value={inntektGrunnlag.aarsInntekt}
              onChange={(e) =>
                setInntektGrunnlag({
                  ...inntektGrunnlag,
                  aarsInntekt: Number(e.target.value),
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
                  gjeldendeAar: Number(e.target.value),
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
                      fom: date == null ? '' : formaterDatoTilYearMonth(date),
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
          </FormWrapper>
          <FormWrapper>
            <Textarea
              label={'Spesifikasjon av inntekt'}
              value={inntektGrunnlag.spesifikasjon}
              onChange={(e) =>
                setInntektGrunnlag({
                  ...inntektGrunnlag,
                  spesifikasjon: e.target.value,
                })
              }
            />
          </FormWrapper>
          <FormKnapper>
            <Button size="small" loading={isPending(inntektGrunnlagStatus)} type="submit">
              Lagre
            </Button>
          </FormKnapper>
        </Rows>
      </InntektAvkortingForm>
    </div>
  )
}

const InntektAvkortingForm = styled.form`
  display: flex;
  margin: 1em 0 1em 0;
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
const Rows = styled.div`
  flex-direction: column;
`

import { IBehandlingReducer } from '~store/reducers/BehandlingReducer'
import React from 'react'
import { LovtekstMedLenke } from '~components/behandling/soeknadsoversikt/soeknadoversikt/LovtekstMedLenke'
import styled from 'styled-components'
import { Button, Heading, Select, TextField } from '@navikt/ds-react'
import { PlusCircleIcon } from '@navikt/aksel-icons'
import { InstitusjonsoppholdGrunnlag, Reduksjon } from '~shared/types/Beregning'
import { Controller, useFieldArray, useForm } from 'react-hook-form'
import MaanedVelger from '~components/behandling/beregningsgrunnlag/MaanedVelger'

type InstitusjonsoppholdProps = {
  behandling: IBehandlingReducer
  onSubmit: (data: InstitusjonsoppholdGrunnlag) => void
}

const Institusjonsopphold = (props: InstitusjonsoppholdProps) => {
  const { behandling } = props
  const { control, register } = useForm<{ institusjonsOppholdForm: InstitusjonsoppholdGrunnlag }>({
    defaultValues: {
      institusjonsOppholdForm: behandling.beregningsGrunnlag?.institusjonsopphold,
    },
  })

  const { fields, append, remove } = useFieldArray({
    name: 'institusjonsOppholdForm',
    control,
  })

  console.log(behandling.sakType)
  //TODO: hent hendelser på behandling som er av type instittusjonsopphold
  return (
    <InstitusjonsoppholdsWrapper>
      <>
        <LovtekstMedLenke
          tittel={'Institusjonsopphold'}
          hjemler={[
            {
              tittel: '§ 18-8.Barnepensjon under opphold i institusjon',
              lenke: 'https://lovdata.no/dokument/NL/lov/1997-02-28-19/KAPITTEL_6-6#%C2%A718-8',
            },
          ]}
          status={null}
        >
          <p>
            Barnepensjonen skal reduseres under opphold i en institusjon med fri kost og losji under statlig ansvar
            eller tilsvarende institusjon i utlandet. Regelen gjelder ikke ved opphold i somatiske sykehusavdelinger.
            Oppholdet må vare i tre måneder i tillegg til innleggelsesmåneden for at barnepensjonen skal bli redusert.
            Dersom barnet har faste og nødvendige utgifter til bolig, kan arbeids- og velferdsetaten bestemme at
            barnepensjonen ikke skal reduseres eller reduseres mindre enn hovedregelen sier.
          </p>
        </LovtekstMedLenke>
        <Heading level="3" size="small">
          Beregningsperiode institusjonsopphold
        </Heading>
      </>
      <form>
        <>
          {fields.map((item, index) => (
            <div key={item.id}>
              <InstitusjonsperioderWrapper>
                <Controller
                  name={`institusjonsOppholdForm.${index}.fom`}
                  control={control}
                  rules={{ required: true }}
                  render={(fom) => (
                    <MaanedVelger
                      label="Fra og med"
                      value={fom.field.value}
                      onChange={(date: Date | null) => fom.field.onChange(date)}
                    />
                  )}
                />
                <Controller
                  name={`institusjonsOppholdForm.${index}.tom`}
                  control={control}
                  render={(tom) => (
                    <MaanedVelger
                      label="Til og med"
                      value={tom.field.value}
                      onChange={(date: Date | null) => tom.field.onChange(date)}
                    />
                  )}
                />
                <Select
                  label="Reduksjon"
                  {...register(`institusjonsOppholdForm.${index}.data.reduksjon`, {
                    required: true,
                    validate: { notDefault: (v) => v !== Reduksjon.VELG_REDUKSJON },
                  })}
                >
                  {Object.entries(Reduksjon).map(([reduksjonsKey, reduksjontekst]) => (
                    <option key={reduksjonsKey} value={reduksjonsKey}>
                      {reduksjontekst}
                    </option>
                  ))}
                </Select>
              </InstitusjonsperioderWrapper>
              <TextField
                label="Begrunnelse for periode(hvis aktuelt)"
                {...register(`institusjonsOppholdForm.${index}.data.begrunnelse`)}
              />
              <Button onClick={() => remove(index)}>Fjern opphold</Button>
            </div>
          ))}
        </>
      </form>
      <div>
        <LagreButtonWrapper>Lagre</LagreButtonWrapper>
      </div>
      <Button
        icon={<PlusCircleIcon title="a11y-title" />}
        iconPosition="left"
        variant="tertiary"
        onClick={() =>
          append([
            {
              fom: new Date(Date.now()),
              tom: undefined,
              data: { reduksjon: Reduksjon.VELG_REDUKSJON, egenReduksjon: undefined, begrunnelse: '' },
            },
          ])
        }
      >
        Legg til beregningsperiode
      </Button>
    </InstitusjonsoppholdsWrapper>
  )
}

export default Institusjonsopphold

const InstitusjonsoppholdsWrapper = styled.div`
  padding: 0em 4em;
  max-width: 56em;
`

const InstitusjonsperioderWrapper = styled.div`
  width: 100%;
  display: flex;
  flex-direction: row;
  justify-content: space-between;
`

const LagreButtonWrapper = styled(Button)`
  margin-top: 15px;
  margin-bottom: 15px;
  width: 150px;
`

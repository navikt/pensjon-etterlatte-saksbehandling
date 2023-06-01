import { IBehandlingReducer } from '~store/reducers/BehandlingReducer'
import React from 'react'
import { LovtekstMedLenke } from '~components/behandling/soeknadsoversikt/soeknadoversikt/LovtekstMedLenke'
import styled from 'styled-components'
import { Heading, TextField } from '@navikt/ds-react'
import { InstitusjonsoppholdGrunnlag } from '~shared/types/Beregning'
import { Controller, useFieldArray, useForm } from 'react-hook-form'
import MaanedVelger from '~components/behandling/beregningsgrunnlag/MaanedVelger'

type InstitusjonsoppholdProps = {
  behandling: IBehandlingReducer
  onSubmit: (data: InstitusjonsoppholdGrunnlag) => void //TODO: muyst be changed
}

const Institusjonsopphold = (props: InstitusjonsoppholdProps) => {
  const { behandling } = props
  const { control } = useForm<{ institusjonsOppholdForm: InstitusjonsoppholdGrunnlag }>({
    defaultValues: { institusjonsOppholdForm: behandling.beregningsGrunnlag?.institusjonsopphold },
  })
  const { fields } = useFieldArray({
    name: 'institusjonsOppholdForm',
    control,
  })

  console.log(behandling.sakType)
  //TODO: hent hendelser på behandling som er av type instittusjonsopphold
  return (
    <>
      <InstitusjonsoppholdsWrapper>
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
      </InstitusjonsoppholdsWrapper>
      <form>
        <>
          {fields.map((item, index) => (
            <div key={item.id}>
              <Controller
                name={`institusjonsOppholdForm.${index}.fom`}
                control={control}
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
              <TextField label="Reduksjon" />
            </div>
          ))}
        </>
      </form>
    </>
  )
}

export default Institusjonsopphold

const InstitusjonsoppholdsWrapper = styled.div`
  padding: 0em 4em;
  max-width: 56em;
`

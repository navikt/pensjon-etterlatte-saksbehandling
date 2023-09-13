import { IBehandlingReducer } from '~store/reducers/BehandlingReducer'
import React, { useState } from 'react'
import { LovtekstMedLenke } from '~components/behandling/soeknadsoversikt/soeknadoversikt/LovtekstMedLenke'
import { Button, Heading, ReadMore } from '@navikt/ds-react'
import { AGreen500 } from '@navikt/ds-tokens/dist/tokens'
import { PlusCircleIcon, CheckmarkCircleIcon } from '@navikt/aksel-icons'
import { InstitusjonsoppholdGrunnlagData } from '~shared/types/Beregning'
import { useFieldArray, useForm } from 'react-hook-form'
import Insthendelser from '~components/behandling/beregningsgrunnlag/Insthendelser'
import {
  feilIKomplettePerioderOverIntervallInstitusjonsopphold,
  mapListeFraDto,
} from '~components/behandling/beregningsgrunnlag/PeriodisertBeregningsgrunnlag'
import { hentBehandlesFraStatus } from '~components/behandling/felles/utils'
import { InstitusjonsoppholdsWrapper } from './institusjonsopphold-styling'
import {
  FeilIPeriode,
  FeilIPerioder,
  validerInstitusjonsopphold,
} from '~components/behandling/beregningsgrunnlag/InstitusjonsoppholdPerioder'
import { Table } from '@navikt/ds-react'
import InstitusjonsoppholdTableWrapper from '~components/behandling/beregningsgrunnlag/InstitusjonsoppholdTableWrapper'
import styled from 'styled-components'

const ReadMoreMarginBottom = styled(ReadMore)`
  margin-bottom: 1rem;
`

type InstitusjonsoppholdProps = {
  behandling: IBehandlingReducer
  onSubmit: (data: InstitusjonsoppholdGrunnlagData) => void
}

const InstitusjonsoppholdBP = (props: InstitusjonsoppholdProps) => {
  const { behandling, onSubmit } = props
  const behandles = hentBehandlesFraStatus(behandling?.status)
  const [visFeil, setVisFeil] = useState(false)
  const [visOkLagret, setVisOkLagret] = useState(false)
  const { control, register, watch, handleSubmit, formState } = useForm<{
    institusjonsOppholdForm: InstitusjonsoppholdGrunnlagData
  }>({
    defaultValues: {
      institusjonsOppholdForm: mapListeFraDto(behandling.beregningsGrunnlag?.institusjonsopphold ?? []),
    },
  })

  const { isValid, errors } = formState
  const { fields, append, remove } = useFieldArray({
    name: 'institusjonsOppholdForm',
    control,
  })

  const heleSkjemaet = watch('institusjonsOppholdForm')
  const feilOverlappendePerioder: [number, FeilIPeriode][] = [
    ...feilIKomplettePerioderOverIntervallInstitusjonsopphold(heleSkjemaet),
  ]
  const ferdigstilleForm = (data: { institusjonsOppholdForm: InstitusjonsoppholdGrunnlagData }) => {
    if (validerInstitusjonsopphold(data.institusjonsOppholdForm) && isValid && feilOverlappendePerioder?.length === 0) {
      onSubmit(data.institusjonsOppholdForm)
      setVisFeil(false)
      setVisOkLagret(true)
      setTimeout(() => {
        setVisOkLagret(false)
      }, 1000)
    } else {
      setVisFeil(true)
      setVisOkLagret(false)
    }
  }

  return (
    <InstitusjonsoppholdsWrapper>
      {(behandling.beregningsGrunnlag?.institusjonsopphold &&
        behandling.beregningsGrunnlag?.institusjonsopphold?.length > 0) ||
      behandles ? (
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
          <Insthendelser sakid={behandling.sakId} />
          <Heading level="3" size="small">
            Beregningsperioder institusjonsopphold
          </Heading>
          <ReadMoreMarginBottom header="Hva skal registreres?">
            Registrer perioden da ytelsen skal reduseres, altså fom-dato fra den 1. i fjerde måneden etter innleggelse
            (fra måneden etter innleggelse hvis vedkommende innen tre måneder etter utskrivelsen på nytt kommer i
            institusjon), og siste dato i måneden før utskrivingsmåneden.
          </ReadMoreMarginBottom>
        </>
      ) : null}
      {fields.length ? (
        <Table>
          <Table.Header>
            <Table.Row>
              <Table.HeaderCell />
              <Table.HeaderCell scope="col">Periode</Table.HeaderCell>
              <Table.HeaderCell scope="col">Reduksjon</Table.HeaderCell>
              <Table.HeaderCell scope="col">Begrunnelse</Table.HeaderCell>
            </Table.Row>
          </Table.Header>
          <Table.Body id="forminstitusjonsopphold">
            {fields.map((item, index) => (
              <InstitusjonsoppholdTableWrapper
                key={item.id}
                item={item}
                index={index}
                control={control}
                register={register}
                remove={remove}
                watch={watch}
                setVisFeil={setVisFeil}
                errors={errors.institusjonsOppholdForm?.[index]}
                behandles={behandles}
              />
            ))}
          </Table.Body>
        </Table>
      ) : null}
      {behandles && (
        <Button
          type="button"
          icon={<PlusCircleIcon title="legg til" />}
          iconPosition="left"
          variant="tertiary"
          onClick={() => {
            setVisFeil(false)
            append([
              {
                fom: new Date(Date.now()),
                tom: undefined,
                data: { reduksjon: 'VELG_REDUKSJON', egenReduksjon: undefined, begrunnelse: '' },
              },
            ])
          }}
        >
          Legg til beregningsperiode
        </Button>
      )}
      {behandles && fields.length ? (
        <Button type="submit" onClick={handleSubmit(ferdigstilleForm)}>
          Lagre institusjonsopphold
        </Button>
      ) : null}
      {visFeil && feilOverlappendePerioder?.length > 0 && (
        <>
          <FeilIPerioder feil={feilOverlappendePerioder} />
        </>
      )}
      {visOkLagret && <CheckmarkCircleIcon color={AGreen500} fontSize={20} />}
    </InstitusjonsoppholdsWrapper>
  )
}

export default InstitusjonsoppholdBP

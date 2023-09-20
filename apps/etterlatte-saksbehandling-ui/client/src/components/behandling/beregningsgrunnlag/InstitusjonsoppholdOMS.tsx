import { IBehandlingReducer } from '~store/reducers/BehandlingReducer'
import React, { useState } from 'react'
import { LovtekstMedLenke } from '~components/behandling/soeknadsoversikt/soeknadoversikt/LovtekstMedLenke'
import { Button, Heading, ReadMore, Table } from '@navikt/ds-react'
import { AGreen500 } from '@navikt/ds-tokens/dist/tokens'
import { PlusCircleIcon, CheckmarkCircleIcon } from '@navikt/aksel-icons'
import { InstitusjonsoppholdGrunnlagData, ReduksjonOMS } from '~shared/types/Beregning'
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
import InstitusjonsoppholdTableWrapper from './InstitusjonsoppholdTableWrapper'

type InstitusjonsoppholdProps = {
  behandling: IBehandlingReducer
  onSubmit: (data: InstitusjonsoppholdGrunnlagData) => void
}

const InstitusjonsoppholdOMS = (props: InstitusjonsoppholdProps) => {
  const { behandling, onSubmit } = props
  const behandles = hentBehandlesFraStatus(behandling?.status)
  const [visFeil, setVisFeil] = useState(false)
  const [visOkLagret, setVisOkLagret] = useState(false)
  const { control, register, watch, handleSubmit, formState } = useForm<{
    institusjonsOppholdForm: InstitusjonsoppholdGrunnlagData
  }>({
    defaultValues: {
      institusjonsOppholdForm: mapListeFraDto(behandling.beregningsGrunnlagOMS?.institusjonsopphold ?? []),
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
      {(behandling.beregningsGrunnlagOMS?.institusjonsopphold &&
        behandling.beregningsGrunnlagOMS?.institusjonsopphold?.length > 0) ||
      behandles ? (
        <>
          <LovtekstMedLenke
            tittel={'Institusjonsopphold'}
            hjemler={[
              {
                tittel: '§ 17-13.Ytelser til gjenlevende ektefelle under opphold i institusjon',
                lenke: 'https://lovdata.no/dokument/NL/lov/1997-02-28-19/KAPITTEL_6-4#%C2%A717-13',
              },
            ]}
            status={null}
          >
            <p>
              Personer som mottar ytelser etter dette kapitlet, får ytelsene redusert etter bestemmelsene i denne
              paragrafen under opphold i en institusjon med fri kost og losji under statlig ansvar eller tilsvarende
              institusjon i utlandet. Ytelsene blir ikke redusert under opphold i somatiske sykehusavdelinger. Ytelser
              etter dette kapitlet gis uten reduksjon i innleggelsesmåneden og de tre påfølgende månedene. Deretter blir
              ytelsene redusert og skal under oppholdet utgjøre 45 prosent av grunnbeløpet. Ytelsene skal ikke reduseres
              når vedkommende forsørger barn. Dersom vedkommende har faste og nødvendige utgifter til bolig, kan
              arbeids- og velferdsetaten bestemme at ytelsene ikke skal reduseres eller reduseres mindre enn nevnt i
              andre ledd. Dersom vedkommende innen tre måneder etter utskrivelsen på nytt kommer i institusjon, gis det
              redusert ytelse fra og med måneden etter at det nye oppholdet tar til. Ytelsene skal utbetales etter
              lovens vanlige bestemmelser fra og med utskrivingsmåneden. Ytelsen etter denne paragrafen må ikke
              overstige den ytelsen vedkommende har rett til etter lovens vanlige bestemmelser.
            </p>
          </LovtekstMedLenke>
          <Insthendelser sakid={behandling.sakId} />
          <Heading level="3" size="small">
            Beregningsperiode institusjonsopphold
          </Heading>
          <ReadMore header="Hva skal registreres?">
            Registrer perioden da ytelsen skal reduseres, altså fom-dato fra den 1. i fjerde måneden etter innleggelse
            (fra måneden etter innleggelse hvis vedkommende innen tre måneder etter utskrivelsen på nytt kommer i
            institusjon), og siste dato i måneden før utskrivingsmåneden.
          </ReadMore>
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
                reduksjon={ReduksjonOMS}
              />
            ))}
          </Table.Body>
        </Table>
      ) : null}
      {behandles && (
        <Button
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
      {behandles && (
        <Button type="submit" onClick={handleSubmit(ferdigstilleForm)}>
          Lagre institusjonsopphold
        </Button>
      )}
      {visFeil && feilOverlappendePerioder?.length > 0 && (
        <>
          <FeilIPerioder feil={feilOverlappendePerioder} />
        </>
      )}
      {visOkLagret && <CheckmarkCircleIcon color={AGreen500} fontSize={20} />}
    </InstitusjonsoppholdsWrapper>
  )
}

export default InstitusjonsoppholdOMS

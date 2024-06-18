import { IBehandlingReducer } from '~store/reducers/BehandlingReducer'
import React, { useEffect, useState } from 'react'
import { Button, Heading, ReadMore, Table } from '@navikt/ds-react'
import { AGreen500 } from '@navikt/ds-tokens/dist/tokens'
import { CheckmarkCircleIcon, PlusCircleIcon } from '@navikt/aksel-icons'
import { InstitusjonsoppholdGrunnlagData, InstitusjonsoppholdGrunnlagDTO } from '~shared/types/Beregning'
import { useFieldArray, useForm } from 'react-hook-form'
import Insthendelser from '~components/behandling/beregningsgrunnlag/Insthendelser'
import {
  feilIKomplettePerioderOverIntervallInstitusjonsopphold,
  mapListeFraDto,
} from '~components/behandling/beregningsgrunnlag/PeriodisertBeregningsgrunnlag'
import { behandlingErRedigerbar } from '~components/behandling/felles/utils'
import { InstitusjonsoppholdsWrapper } from './institusjonsopphold-styling'
import {
  FeilIPeriode,
  FeilIPerioder,
  validerInstitusjonsopphold,
} from '~components/behandling/beregningsgrunnlag/InstitusjonsoppholdPerioder'
import InstitusjonsoppholdTableWrapper from '~components/behandling/beregningsgrunnlag/InstitusjonsoppholdTableWrapper'
import styled from 'styled-components'
import { useInnloggetSaksbehandler } from '../useInnloggetSaksbehandler'

const ReadMoreMarginBottom = styled(ReadMore)`
  margin-bottom: 1rem;
`

type InstitusjonsoppholdProps = {
  behandling: IBehandlingReducer
  institusjonsopphold: InstitusjonsoppholdGrunnlagDTO | undefined
  lovtekstMedLenke: React.ReactNode
  reduksjonsTyper: Record<string, string>
  onSubmit: (data: InstitusjonsoppholdGrunnlagData) => void
}

const InstitusjonsoppholdBeregning = (props: InstitusjonsoppholdProps) => {
  const { behandling, onSubmit, institusjonsopphold, lovtekstMedLenke, reduksjonsTyper } = props
  const innloggetSaksbehandler = useInnloggetSaksbehandler()
  const behandles = behandlingErRedigerbar(
    behandling.status,
    behandling.sakEnhetId,
    innloggetSaksbehandler.skriveEnheter
  )
  const [visFeil, setVisFeil] = useState(false)
  const [visOkLagret, setVisOkLagret] = useState(false)
  const { control, register, watch, handleSubmit, formState } = useForm<{
    institusjonsOppholdForm: InstitusjonsoppholdGrunnlagData
  }>({
    defaultValues: {
      institusjonsOppholdForm: mapListeFraDto(institusjonsopphold ?? []),
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
  useEffect(() => {
    if (heleSkjemaet.length == 0) {
      onSubmit([])
    }
  }, [heleSkjemaet])

  const lagreSkjema = (data: { institusjonsOppholdForm: InstitusjonsoppholdGrunnlagData }) => {
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
      {(institusjonsopphold && institusjonsopphold?.length > 0) || behandles ? (
        <>
          {lovtekstMedLenke}
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
                reduksjon={reduksjonsTyper}
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
        <Button type="submit" onClick={handleSubmit(lagreSkjema)}>
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

export default InstitusjonsoppholdBeregning

import { BodyShort, Button, Heading, Loader, Radio, RadioGroup } from '@navikt/ds-react'
import { Content, Header } from '../../../shared/styled'
import React, { useContext, useState } from 'react'
import { Border } from '../soeknadsoversikt/styled'
import { Barn } from '../soeknadsoversikt/familieforhold/personer/Barn'
import { Soesken } from '../soeknadsoversikt/familieforhold/personer/Soesken'
import { AppContext } from '../../../store/AppContext'
import styled from 'styled-components'
import { BehandlingHandlingKnapper } from '../handlinger/BehandlingHandlingKnapper'
import { hentBehandling, lagreSoeskenMedIBeregning } from '../../../shared/api/behandling'
import { useBehandlingRoutes } from '../BehandlingRoutes'
import { Controller, useForm } from 'react-hook-form'
import { formaterStringDato } from '../../../utils/formattering'
import { addBehandlingAction } from '../../../store/reducers/BehandlingReducer'
import { hentBehandlesFraStatus } from '../felles/utils'
import { NesteOgTilbake } from '../handlinger/NesteOgTilbake'

interface SoeskenMedIBeregning {
  foedselsnummer: string
  skalBrukes: boolean
}

const Beregningsgrunnlag = () => {
  const { next } = useBehandlingRoutes()
  const ctx = useContext(AppContext)
  const behandling = ctx.state.behandlingReducer
  const behandles = hentBehandlesFraStatus(ctx.state.behandlingReducer?.status)
  const [isLoading, setIsLoading] = useState(false)

  if (behandling.kommerSoekerTilgode == null || behandling.familieforhold?.avdoede == null) {
    return <div style={{ color: 'red' }}>Familieforhold kan ikke hentes ut</div>
  }

  const soeker = behandling.kommerSoekerTilgode.familieforhold.soeker
  const soesken = behandling.familieforhold.avdoede.opplysning.avdoedesBarn?.filter(
    (barn) => barn.foedselsnummer !== soeker.fnr
  )
  const beregningsperiode = behandling.beregning?.beregningsperioder ?? []

  const { control, handleSubmit } = useForm<{ beregningsgrunnlag: SoeskenMedIBeregning[] }>({
    defaultValues: {
      beregningsgrunnlag:
        soesken?.map((person) => ({
          foedselsnummer: person.foedselsnummer,
          skalBrukes: !!beregningsperiode[0]?.soeskenFlokk?.find((p) => p.foedselsnummer === person.foedselsnummer),
        })) ?? [],
    },
  })

  const doedsdato = behandling.kommerSoekerTilgode.familieforhold.avdoed.doedsdato

  return (
    <Content>
      <Header>
        <h1>Beregningsgrunnlag</h1>
        <BodyShort spacing>
          Vilkårsresultat: <strong>Innvilget fra {formaterStringDato(behandling.virkningstidspunkt)}</strong>
        </BodyShort>
        <Heading level="2" size="small">
          Søskenjustering
        </Heading>
      </Header>

      <FamilieforholdWrapper
        id="form"
        onSubmit={handleSubmit(async (formValues) => {
          if (formValues.beregningsgrunnlag.length !== 0) {
            setIsLoading(true)
            await lagreSoeskenMedIBeregning(behandling.id, formValues.beregningsgrunnlag)
          }
          await hentBehandling(behandling.id).then((response) => {
            setIsLoading(false)
            if (response.status === 'ok' && response.data) {
              ctx.dispatch(addBehandlingAction(response.data))
              next()
            } else {
              console.error({ response })
            }
          })
        })}
      >
        <Barn person={behandling.kommerSoekerTilgode.familieforhold.soeker} doedsdato={doedsdato} />
        <Border />
        {soesken?.map((barn, index) => (
          <SoeskenContainer key={barn.foedselsnummer}>
            <Soesken person={barn} familieforhold={behandling.familieforhold!} />
            <Controller
              name={`beregningsgrunnlag.${index}.skalBrukes`}
              control={control}
              render={(soesken) => (
                <RadioGroupRow
                  legend="Oppdras sammen"
                  value={soesken.field.value}
                  onChange={(value: boolean) => soesken.field.onChange(value)}
                >
                  <Radio value={true}>Ja</Radio>
                  <Radio value={false}>Nei</Radio>
                </RadioGroupRow>
              )}
            />
          </SoeskenContainer>
        ))}
      </FamilieforholdWrapper>
      {behandles ? (
        <BehandlingHandlingKnapper>
          <Button variant="primary" size="medium" form="form">
            Beregne og fatte vedtak {isLoading && <Loader />}
          </Button>
        </BehandlingHandlingKnapper>
      ) : (
        <NesteOgTilbake />
      )}
    </Content>
  )
}

const SoeskenContainer = styled.div`
  display: flex;
  align-items: center;
`

const RadioGroupRow = styled(RadioGroup)`
  margin-top: 1.2em;
  .navds-radio-buttons {
    display: flex;
    flex-direction: row;
    gap: 12px;
  }

  legend {
    padding-top: 9px;
  }
`
const FamilieforholdWrapper = styled.form`
  margin-left: 3em;
`

export default Beregningsgrunnlag

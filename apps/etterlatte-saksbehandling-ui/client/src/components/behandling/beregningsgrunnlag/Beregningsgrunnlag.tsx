import { BodyShort, Button, Heading, Loader, Radio, RadioGroup } from '@navikt/ds-react'
import { Content, ContentHeader } from '~shared/styled'
import { useState } from 'react'
import { Border, HeadingWrapper } from '../soeknadsoversikt/styled'
import { Barn } from '../soeknadsoversikt/familieforhold/personer/Barn'
import { Soesken } from '../soeknadsoversikt/familieforhold/personer/Soesken'
import styled from 'styled-components'
import { BehandlingHandlingKnapper } from '../handlinger/BehandlingHandlingKnapper'
import { lagreSoeskenMedIBeregning } from '~shared/api/behandling'
import { useBehandlingRoutes } from '../BehandlingRoutes'
import { Controller, useForm } from 'react-hook-form'
import { formaterStringDato } from '~utils/formattering'
import { hentBehandlesFraStatus } from '../felles/utils'
import { NesteOgTilbake } from '../handlinger/NesteOgTilbake'
import { useAppSelector } from '~store/Store'
import { opprettEllerEndreBeregning } from '~shared/api/beregning'

interface SoeskenMedIBeregning {
  foedselsnummer: string
  skalBrukes: boolean
}

const Beregningsgrunnlag = () => {
  const { next } = useBehandlingRoutes()
  const behandling = useAppSelector((state) => state.behandlingReducer.behandling)
  const behandles = hentBehandlesFraStatus(behandling?.status)
  const [isLoading, setIsLoading] = useState(false)

  if (behandling.kommerBarnetTilgode == null || behandling.familieforhold?.avdoede == null) {
    return <div style={{ color: 'red' }}>Familieforhold kan ikke hentes ut</div>
  }

  const soeker = behandling.søker
  const soesken = behandling.familieforhold.avdoede.opplysning.avdoedesBarn?.filter(
    (barn) => barn.foedselsnummer !== soeker?.foedselsnummer
  )
  const beregningsperiode = behandling.beregning?.beregningsperioder ?? []

  const { control, handleSubmit } = useForm<{ beregningsgrunnlag: SoeskenMedIBeregning[] }>({
    defaultValues: {
      beregningsgrunnlag:
        soesken?.map((person) => ({
          foedselsnummer: person.foedselsnummer,
          skalBrukes: !!beregningsperiode[0]?.soeskenFlokk?.find((fnr) => fnr === person.foedselsnummer),
        })) ?? [],
    },
  })

  const lagBeregning = () => opprettEllerEndreBeregning(behandling.id).then(() => next())

  const doedsdato = behandling.familieforhold.avdoede.opplysning.doedsdato

  return (
    <Content>
      <ContentHeader>
        <HeadingWrapper>
          <Heading spacing size={'large'} level={'1'}>
            Beregningsgrunnlag
          </Heading>
          <BodyShort spacing>
            Vilkårsresultat:{' '}
            <strong>
              Innvilget fra{' '}
              {behandling.virkningstidspunkt ? formaterStringDato(behandling.virkningstidspunkt.dato) : 'ukjent dato'}
            </strong>
          </BodyShort>
          <Heading level="2" size="medium">
            Søskenjustering
          </Heading>
        </HeadingWrapper>
      </ContentHeader>
      <FamilieforholdWrapper
        id="form"
        onSubmit={handleSubmit(async (formValues) => {
          if (formValues.beregningsgrunnlag.length >= 0) {
            setIsLoading(true)

            await lagreSoeskenMedIBeregning(behandling.id, formValues.beregningsgrunnlag)
              .then(() => lagBeregning())
              .finally(() => setIsLoading(false))
          }
        })}
      >
        {behandling.søker && <Barn person={behandling.søker} doedsdato={doedsdato} />}
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
  padding: 0em 5em;
`

export default Beregningsgrunnlag

import { BodyShort, Button, Heading, Loader, Radio, RadioGroup } from '@navikt/ds-react'
import { Content, ContentHeader } from '~shared/styled'
import { useEffect } from 'react'
import { Border, HeadingWrapper } from '../soeknadsoversikt/styled'
import { Barn } from '../soeknadsoversikt/familieforhold/personer/Barn'
import { Soesken } from '../soeknadsoversikt/familieforhold/personer/Soesken'
import styled from 'styled-components'
import { BehandlingHandlingKnapper } from '../handlinger/BehandlingHandlingKnapper'
import { useBehandlingRoutes } from '../BehandlingRoutes'
import { Controller, useForm } from 'react-hook-form'
import { formaterStringDato } from '~utils/formattering'
import { hentBehandlesFraStatus } from '../felles/utils'
import { NesteOgTilbake } from '../handlinger/NesteOgTilbake'
import { useAppSelector } from '~store/Store'
import { opprettEllerEndreBeregning } from '~shared/api/beregning'
import { isFailure, isPending, isPendingOrInitial, isSuccess, useApiCall } from '~shared/hooks/useApiCall'
import { hentSoeskenMedIBeregning, lagreSoeskenMedIBeregning } from '~shared/api/grunnlag'
import { SoeskenMedIBeregning } from '~shared/types/Grunnlagsopplysning'
import Spinner from '~shared/Spinner'
import { IPdlPerson } from '~shared/types/Person'

interface FormValues {
  foedselsnummer: string
  skalBrukes?: boolean
}

const Beregningsgrunnlag = () => {
  const { next } = useBehandlingRoutes()
  const behandling = useAppSelector((state) => state.behandlingReducer.behandling)
  const behandles = hentBehandlesFraStatus(behandling?.status)
  const [beregningsgrunnlag, hentBeregningsgrunnlag] = useApiCall(hentSoeskenMedIBeregning)
  const [soeskenMedIBeregning, postSoeskenMedIBeregning] = useApiCall(lagreSoeskenMedIBeregning)
  const [endreBeregning, postOpprettEllerEndreBeregning] = useApiCall(opprettEllerEndreBeregning)
  const { control, handleSubmit, setValue } = useForm<{ beregningsgrunnlag: FormValues[] }>()

  useEffect(() => {
    hentBeregningsgrunnlag(behandling.sak, (result) => {
      setValue('beregningsgrunnlag', result.opplysning.beregningsgrunnlag)
    })
  }, [])

  if (behandling.kommerBarnetTilgode == null || behandling.familieforhold?.avdoede == null) {
    return <div style={{ color: 'red' }}>Familieforhold kan ikke hentes ut</div>
  }

  const soesken: IPdlPerson[] =
    behandling.familieforhold.avdoede.opplysning.avdoedesBarn?.filter(
      (barn) => barn.foedselsnummer !== behandling.søker?.foedselsnummer
    ) ?? []

  const onSubmit = (beregningsgrunnlag: SoeskenMedIBeregning[]) =>
    postSoeskenMedIBeregning({ behandlingsId: behandling.id, soeskenMedIBeregning: beregningsgrunnlag }, () =>
      postOpprettEllerEndreBeregning(behandling.id, () => next())
    )

  const doedsdato = behandling.familieforhold.avdoede.opplysning.doedsdato

  const visSoeskenjustering =
    isSuccess(beregningsgrunnlag) || (isFailure(beregningsgrunnlag) && beregningsgrunnlag.error.statusCode === 404)

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
        onSubmit={handleSubmit(
          ({ beregningsgrunnlag }) =>
            validerSoeskenjustering(soesken, beregningsgrunnlag) && onSubmit(beregningsgrunnlag)
        )}
      >
        {behandling.søker && <Barn person={behandling.søker} doedsdato={doedsdato} />}
        <Border />
        <Spinner visible={isPendingOrInitial(beregningsgrunnlag)} label={'Henter beregningsgrunnlag for søsken'} />
        {visSoeskenjustering &&
          soesken.map((barn, index) => (
            <SoeskenContainer key={barn.foedselsnummer}>
              <Soesken person={barn} familieforhold={behandling.familieforhold!} />
              <Controller
                name={`beregningsgrunnlag.${index}`}
                control={control}
                render={(soesken) =>
                  behandles ? (
                    <RadioGroupRow
                      legend="Oppdras sammen"
                      value={soesken.field.value?.skalBrukes ?? null}
                      onChange={(value: boolean) =>
                        soesken.field.onChange({ foedselsnummer: barn.foedselsnummer, skalBrukes: value })
                      }
                    >
                      <Radio value={true}>Ja</Radio>
                      <Radio value={false}>Nei</Radio>
                    </RadioGroupRow>
                  ) : (
                    <OppdrasSammenLes>
                      <strong>Oppdras sammen</strong>
                      <label>{soesken.field.value ? 'Ja' : 'Nei'}</label>
                    </OppdrasSammenLes>
                  )
                }
              />
            </SoeskenContainer>
          ))}
      </FamilieforholdWrapper>
      {behandles ? (
        <BehandlingHandlingKnapper>
          <Button variant="primary" size="medium" form="form">
            Beregne og fatte vedtak {(isPending(soeskenMedIBeregning) || isPending(endreBeregning)) && <Loader />}
          </Button>
        </BehandlingHandlingKnapper>
      ) : (
        <NesteOgTilbake />
      )}
    </Content>
  )
}

const validerSoeskenjustering = (soesken: IPdlPerson[], justering: FormValues[]): justering is SoeskenMedIBeregning[] =>
  soesken.length === justering.length && justering.every((barn) => barn.skalBrukes !== undefined)

const OppdrasSammenLes = styled.div`
  display: flex;
  flex-direction: column;
`

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

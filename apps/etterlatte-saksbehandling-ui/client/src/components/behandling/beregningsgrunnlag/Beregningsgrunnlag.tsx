import { BodyShort, Button, Heading, Loader, Radio, RadioGroup } from '@navikt/ds-react'
import { Content, ContentHeader } from '~shared/styled'
import { useEffect, useState } from 'react'
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
import { useAppDispatch, useAppSelector } from '~store/Store'
import { opprettEllerEndreBeregning } from '~shared/api/beregning'
import { isFailure, isPending, isPendingOrInitial, isSuccess, useApiCall } from '~shared/hooks/useApiCall'
import { hentSoeskenMedIBeregning, lagreSoeskenMedIBeregning } from '~shared/api/grunnlag'
import { SoeskenMedIBeregning } from '~shared/types/Grunnlagsopplysning'
import Spinner from '~shared/Spinner'
import { IPdlPerson } from '~shared/types/Person'
import { Soeknadsvurdering } from '~components/behandling/soeknadsoversikt/soeknadoversikt/SoeknadsVurdering'
import { oppdaterBehandlingsstatus, resetBeregning } from '~store/reducers/BehandlingReducer'
import { IBehandlingStatus } from '~shared/types/IDetaljertBehandling'
import { ApiErrorAlert } from '~ErrorBoundary'

interface FormValues {
  foedselsnummer: string
  skalBrukes?: boolean
}

const Beregningsgrunnlag = () => {
  const { next } = useBehandlingRoutes()
  const behandling = useAppSelector((state) => state.behandlingReducer.behandling)
  const behandles = hentBehandlesFraStatus(behandling?.status)
  const [ikkeValgtOppdrasSammenPaaAlle, setIkkeValgtOppdrasSammenPaaAlleFeil] = useState(false)
  const dispatch = useAppDispatch()
  const [beregningsgrunnlag, hentBeregningsgrunnlag] = useApiCall(hentSoeskenMedIBeregning)
  const [soeskenMedIBeregning, postSoeskenMedIBeregning] = useApiCall(lagreSoeskenMedIBeregning)
  const [endreBeregning, postOpprettEllerEndreBeregning] = useApiCall(opprettEllerEndreBeregning)
  const { control, handleSubmit, setValue } = useForm<{ beregningsgrunnlag: FormValues[] }>({
    defaultValues: {
      beregningsgrunnlag: [],
    },
  })

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

  const onSubmit = (beregningsgrunnlag: SoeskenMedIBeregning[]) => {
    dispatch(resetBeregning())
    postSoeskenMedIBeregning({ behandlingsId: behandling.id, soeskenMedIBeregning: beregningsgrunnlag }, () =>
      postOpprettEllerEndreBeregning(behandling.id, () => {
        dispatch(oppdaterBehandlingsstatus(IBehandlingStatus.BEREGNET))
        next()
      })
    )
  }

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
        </HeadingWrapper>
      </ContentHeader>
      <TrygdetidWrapper>
        <Soeknadsvurdering
          tittel={'Trygdetid'}
          hjemler={[
            { tittel: '§ 3-5 Trygdetid ved beregning av ytelser', lenke: 'https://lovdata.no/lov/1997-02-28-19/§3-5' },
          ]}
          vurderingsResultat={null}
          status={null}
        >
          <TrygdetidInfo>
            <p>
              Trygdetiden er minst 40 år som følge av faktisk trygdetid og fremtidig trygdetid. Faktisk trygdetid er den
              tiden fra avdøde fylte 16 til personen døde. Fremtidig trygdetid er tiden fra dødsfallet til året avdøde
              hadde blitt 66 år. Saksbehandler bekrefter at følgende stemmer for denne behandlingen ved å gå videre til
              beregning:
            </p>
            <p>
              Trygdetid: <strong>40 år</strong>
            </p>
          </TrygdetidInfo>
        </Soeknadsvurdering>
      </TrygdetidWrapper>
      <ContentHeader>
        <HeadingWrapper>
          <Heading level="2" size="medium">
            Søskenjustering
          </Heading>
        </HeadingWrapper>
      </ContentHeader>
      <FamilieforholdWrapper
        id="form"
        onSubmit={handleSubmit(({ beregningsgrunnlag }) => {
          if (validerSoeskenjustering(soesken, beregningsgrunnlag)) {
            onSubmit(beregningsgrunnlag)
          } else {
            setIkkeValgtOppdrasSammenPaaAlleFeil(true)
          }
        })}
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
                      error={
                        soesken.field.value?.skalBrukes === undefined && ikkeValgtOppdrasSammenPaaAlle
                          ? 'Du må velge ja/nei på alle søsken'
                          : ''
                      }
                      onChange={(value: boolean) => {
                        soesken.field.onChange({ foedselsnummer: barn.foedselsnummer, skalBrukes: value })
                        setIkkeValgtOppdrasSammenPaaAlleFeil(false)
                      }}
                    >
                      <Radio value={true}>Ja</Radio>
                      <Radio value={false}>Nei</Radio>
                    </RadioGroupRow>
                  ) : (
                    <OppdrasSammenLes>
                      <strong>Oppdras sammen</strong>
                      <label>{soesken.field.value?.skalBrukes ? 'Ja' : 'Nei'}</label>
                    </OppdrasSammenLes>
                  )
                }
              />
            </SoeskenContainer>
          ))}
      </FamilieforholdWrapper>

      {isFailure(endreBeregning) && <ApiErrorAlert>Kunne ikke opprette ny beregning</ApiErrorAlert>}
      {isFailure(soeskenMedIBeregning) && <ApiErrorAlert>Kunne ikke lagre beregningsgrunnlag</ApiErrorAlert>}

      {visSoeskenjustering &&
        (behandles ? (
          <BehandlingHandlingKnapper>
            <Button variant="primary" size="medium" form="form">
              Beregne og fatte vedtak {(isPending(soeskenMedIBeregning) || isPending(endreBeregning)) && <Loader />}
            </Button>
          </BehandlingHandlingKnapper>
        ) : (
          <NesteOgTilbake />
        ))}
    </Content>
  )
}

const validerSoeskenjustering = (soesken: IPdlPerson[], justering: FormValues[]): justering is SoeskenMedIBeregning[] =>
  soesken.length === justering.length && justering.every((barn) => barn?.skalBrukes !== undefined)

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
  padding: 0em 6em;
`

const TrygdetidWrapper = styled.form`
  padding: 0em 4em;
  max-width: 56em;
`

const TrygdetidInfo = styled.form`
  display: flex;
  flex-direction: column;
`

export default Beregningsgrunnlag

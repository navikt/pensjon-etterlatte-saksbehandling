import { Button, Heading, Loader, Radio, RadioGroup } from '@navikt/ds-react'
import { ContentHeader } from '~shared/styled'
import { useEffect, useState } from 'react'
import { Border, HeadingWrapper } from '../soeknadsoversikt/styled'
import { Barn } from '../soeknadsoversikt/familieforhold/personer/Barn'
import { Soesken } from '../soeknadsoversikt/familieforhold/personer/Soesken'
import styled from 'styled-components'
import { BehandlingHandlingKnapper } from '../handlinger/BehandlingHandlingKnapper'
import { useBehandlingRoutes } from '../BehandlingRoutes'
import { Controller, useForm } from 'react-hook-form'
import { hentBehandlesFraStatus } from '../felles/utils'
import { NesteOgTilbake } from '../handlinger/NesteOgTilbake'
import { useAppDispatch } from '~store/Store'
import { opprettEllerEndreBeregning } from '~shared/api/beregning'
import { isFailure, isPending, useApiCall } from '~shared/hooks/useApiCall'
import { hentSoeskenjusteringsgrunnlag, lagreSoeskenMedIBeregning } from '~shared/api/grunnlag'
import { SoeskenMedIBeregning } from '~shared/types/grunnlag'
import Spinner from '~shared/Spinner'
import { IPdlPerson } from '~shared/types/Person'
import {
  IBehandlingReducer,
  oppdaterBehandlingsstatus,
  oppdaterSoeskenjusteringsgrunnlag,
  resetBeregning,
} from '~store/reducers/BehandlingReducer'
import { IBehandlingStatus } from '~shared/types/IDetaljertBehandling'
import { ApiErrorAlert } from '~ErrorBoundary'
import Trygdetid from '~components/behandling/beregningsgrunnlag/Trygdetid'

interface FormValues {
  foedselsnummer: string
  skalBrukes?: boolean
}

const BeregningsgrunnlagBarnepensjon = (props: { behandling: IBehandlingReducer }) => {
  const { behandling } = props
  const { next } = useBehandlingRoutes()
  const behandles = hentBehandlesFraStatus(behandling?.status)
  const soeskenjustering = behandling.soeskenjusteringsgrunnlag?.beregningsgrunnlag
  const [ikkeValgtOppdrasSammenPaaAlle, setIkkeValgtOppdrasSammenPaaAlleFeil] = useState(false)
  const dispatch = useAppDispatch()
  const [soeskenjusteringsgrunnlag, fetchSoeskenjusteringsgrunnlag] = useApiCall(hentSoeskenjusteringsgrunnlag)
  const [lagreSoeskenMedIBeregningStatus, postSoeskenMedIBeregning] = useApiCall(lagreSoeskenMedIBeregning)
  const [endreBeregning, postOpprettEllerEndreBeregning] = useApiCall(opprettEllerEndreBeregning)
  const { control, handleSubmit, reset } = useForm<{ soeskengrunnlag: FormValues[] }>({
    defaultValues: {
      soeskengrunnlag: soeskenjustering,
    },
  })

  const soeskenjusteringErDefinertIRedux = soeskenjustering !== undefined

  useEffect(() => {
    if (!soeskenjusteringErDefinertIRedux) {
      fetchSoeskenjusteringsgrunnlag(behandling.sak, (result) => {
        reset({ soeskengrunnlag: result?.opplysning?.beregningsgrunnlag ?? [] })
        dispatch(
          oppdaterSoeskenjusteringsgrunnlag({ beregningsgrunnlag: result?.opplysning?.beregningsgrunnlag ?? [] })
        )
      })
    }
  }, [])

  if (behandling.kommerBarnetTilgode == null || behandling.familieforhold?.avdoede == null) {
    return <ApiErrorAlert>Familieforhold kan ikke hentes ut</ApiErrorAlert>
  }

  const soesken: IPdlPerson[] =
    behandling.familieforhold.avdoede.opplysning.avdoedesBarn?.filter(
      (barn) => barn.foedselsnummer !== behandling.søker?.foedselsnummer
    ) ?? []

  const onSubmit = (soeskengrunnlag: SoeskenMedIBeregning[]) => {
    dispatch(resetBeregning())
    postSoeskenMedIBeregning({ behandlingsId: behandling.id, soeskenMedIBeregning: soeskengrunnlag }, () =>
      postOpprettEllerEndreBeregning(behandling.id, () => {
        dispatch(
          oppdaterSoeskenjusteringsgrunnlag({
            beregningsgrunnlag: soeskengrunnlag,
          })
        )
        dispatch(oppdaterBehandlingsstatus(IBehandlingStatus.BEREGNET))
        next()
      })
    )
  }

  const doedsdato = behandling.familieforhold.avdoede.opplysning.doedsdato

  return (
    <>
      <Trygdetid />
      <ContentHeader>
        <HeadingWrapper>
          <Heading level="2" size="medium">
            Søskenjustering
          </Heading>
        </HeadingWrapper>
      </ContentHeader>
      <FamilieforholdWrapper
        id="form"
        onSubmit={handleSubmit(({ soeskengrunnlag }) => {
          if (validerSoeskenjustering(soesken, soeskengrunnlag)) {
            onSubmit(soeskengrunnlag)
          } else {
            setIkkeValgtOppdrasSammenPaaAlleFeil(true)
          }
        })}
      >
        {behandling.søker && <Barn person={behandling.søker} doedsdato={doedsdato} />}
        <Border />
        <Spinner visible={isPending(soeskenjusteringsgrunnlag)} label={'Henter beregningsgrunnlag for søsken'} />
        {isFailure(soeskenjusteringsgrunnlag) && <ApiErrorAlert>Søskenjustering kan ikke hentes</ApiErrorAlert>}
        {soeskenjusteringErDefinertIRedux &&
          soesken.map((barn, index) => (
            <SoeskenContainer key={barn.foedselsnummer}>
              <Soesken person={barn} familieforhold={behandling.familieforhold!} />
              <Controller
                name={`soeskengrunnlag.${index}`}
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
      {isFailure(lagreSoeskenMedIBeregningStatus) && <ApiErrorAlert>Kunne ikke lagre beregningsgrunnlag</ApiErrorAlert>}

      {behandles ? (
        <BehandlingHandlingKnapper>
          {soeskenjusteringErDefinertIRedux && (
            <Button variant="primary" size="medium" form="form">
              Beregne og fatte vedtak{' '}
              {(isPending(lagreSoeskenMedIBeregningStatus) || isPending(endreBeregning)) && <Loader />}
            </Button>
          )}
        </BehandlingHandlingKnapper>
      ) : (
        <NesteOgTilbake />
      )}
    </>
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

export default BeregningsgrunnlagBarnepensjon

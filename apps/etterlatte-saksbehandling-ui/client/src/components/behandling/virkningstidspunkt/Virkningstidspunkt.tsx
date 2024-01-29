import { BodyShort, ErrorMessage, Heading, HelpText, MonthValidationT, VStack } from '@navikt/ds-react'
import React, { useState } from 'react'
import { oppdaterBehandlingsstatus, oppdaterVirkningstidspunkt } from '~store/reducers/BehandlingReducer'
import { formaterStringDato } from '~utils/formattering'
import { fastsettVirkningstidspunkt } from '~shared/api/behandling'
import { useApiCall } from '~shared/hooks/useApiCall'
import { InfobokserWrapper, InfoWrapper, VurderingsContainerWrapper, VurderingsTitle } from '../soeknadsoversikt/styled'
import { useAppDispatch } from '~store/Store'
import { IBehandlingStatus, IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { addMonths, addYears, subYears } from 'date-fns'
import { LovtekstMedLenke } from '../soeknadsoversikt/LovtekstMedLenke'
import { LeggTilVurderingButton } from '~components/behandling/soeknadsoversikt/LeggTilVurderingButton'
import { VurderingsboksWrapper } from '~components/vurderingsboks/VurderingsboksWrapper'
import { SoeknadsoversiktTextArea } from '~components/behandling/soeknadsoversikt/SoeknadsoversiktTextArea'
import { hentMinimumsVirkningstidspunkt } from '~components/behandling/virkningstidspunkt/utils'
import styled from 'styled-components'
import { usePersonopplysninger } from '~components/person/usePersonopplysninger'
import { useForm } from 'react-hook-form'
import { ControlledMaanedVelger } from '~shared/components/maanedVelger/ControlledMaanedVelger'
import { ControlledDatoVelger } from '~shared/components/datoVelger/ControlledDatoVelger'

export interface Hjemmel {
  lenke: string
  tittel: string
}

interface VirkningstidspunktSkjema {
  virkningstidspunkt: Date | null
  begrunnelse: string
  kravdato?: Date | null
}

interface Props {
  behandling: IDetaljertBehandling
  redigerbar: boolean
  hjemler: Hjemmel[]
  beskrivelse: string
  children?: { info: React.ReactNode }
  erBosattUtland: boolean
}

const Virkningstidspunkt = ({ behandling, redigerbar, hjemler, beskrivelse, erBosattUtland, children }: Props) => {
  const avdoede = usePersonopplysninger()?.avdoede.find((po) => po)
  const dispatch = useAppDispatch()
  const [, fastsettVirkningstidspunktRequest, resetToInitial] = useApiCall(fastsettVirkningstidspunkt)

  const [vurdert, setVurdert] = useState<boolean>(behandling.virkningstidspunkt !== null)
  const [virkningstidspunkt, setVirkningstidspunkt] = useState<Date | null>(
    behandling.virkningstidspunkt ? new Date(behandling.virkningstidspunkt.dato) : null
  )
  const [begrunnelse, setBegrunnelse] = useState<string>(behandling.virkningstidspunkt?.begrunnelse ?? '')
  const [kravdato, setKravdato] = useState<Date | null>(
    behandling.virkningstidspunkt?.kravdato ? new Date(behandling.virkningstidspunkt.kravdato) : null
  )

  const [errorTekst, setErrorTekst] = useState<string>('')

  const {
    control,
    register,
    formState: { errors },
  } = useForm<VirkningstidspunktSkjema>({
    defaultValues: {
      virkningstidspunkt: behandling.virkningstidspunkt ? new Date(behandling.virkningstidspunkt.dato) : null,
      begrunnelse: behandling.virkningstidspunkt?.begrunnelse ?? '',
      kravdato: behandling.virkningstidspunkt?.kravdato ? new Date(behandling.virkningstidspunkt.kravdato) : null,
    },
  })

  const validerVirkningstidspunkt = (maaned: MonthValidationT): string | undefined => {
    if (!maaned) {
      return 'Du må velge virkningstidspunkt'
    } else if (maaned.isBefore || maaned.isAfter) {
      return 'Virkningstidspunkt er ikke gyldig'
    }
  }

  const fastsett = (onSuccess?: () => void) => {
    setErrorTekst('')
    if (!virkningstidspunkt) {
      return setErrorTekst('Du må velge virkningstidspunkt')
    }
    if (begrunnelse.trim().length === 0) {
      return setErrorTekst('Begrunnelsen må fylles ut')
    }
    if (erBosattUtland && !kravdato) {
      return setErrorTekst('Kravdato kreves på bosatt utland saker')
    }

    return fastsettVirkningstidspunktRequest(
      {
        id: behandling.id,
        dato: virkningstidspunkt,
        begrunnelse: begrunnelse,
        kravdato: kravdato,
      },
      (res) => {
        dispatch(oppdaterVirkningstidspunkt(res))
        dispatch(oppdaterBehandlingsstatus(IBehandlingStatus.OPPRETTET))
        onSuccess?.()
      },
      (error) =>
        setErrorTekst(
          `Kunne ikke sette virkningstidspunkt. ${
            error.detail ? error.detail : 'Last siden på nytt og prøv igjen, meld sak hvis problemet vedvarer'
          }`
        )
    )
  }

  const reset = (onSuccess?: () => void) => {
    resetToInitial()
    setVirkningstidspunkt(behandling.virkningstidspunkt ? new Date(behandling.virkningstidspunkt.dato) : null)
    setKravdato(behandling.virkningstidspunkt?.kravdato ? new Date(behandling.virkningstidspunkt.kravdato) : null)
    setBegrunnelse(behandling.virkningstidspunkt?.begrunnelse ?? '')
    setErrorTekst('')
    setVurdert(behandling.virkningstidspunkt !== null)
    onSuccess?.()
  }

  return (
    <>
      <LovtekstMedLenke
        tittel="Virkningstidspunkt"
        hjemler={hjemler}
        status={Boolean(behandling.virkningstidspunkt) ? 'success' : 'warning'}
      >
        <div>
          <Beskrivelse>{beskrivelse}</Beskrivelse>
          <InfobokserWrapper>
            <InfoWrapper>{children?.info}</InfoWrapper>
          </InfobokserWrapper>
        </div>

        <VurderingsContainerWrapper>
          {!vurdert ? (
            <LeggTilVurderingButton onClick={() => setVurdert(true)}>Legg til vurdering</LeggTilVurderingButton>
          ) : (
            <VurderingsboksWrapper
              tittel="Hva er virkningstidspunkt for behandlingen?"
              subtittelKomponent={
                <VStack gap="4">
                  {erBosattUtland && (
                    <div>
                      <Heading size="xsmall">
                        <HelpTextWrapper>
                          Kravdato utland
                          <HelpText strategy="fixed">
                            Skriv inn kravdato for søknad i utlandet, som hentes fra SED P2100.
                          </HelpText>
                        </HelpTextWrapper>
                      </Heading>
                      <BodyShort>
                        {behandling.virkningstidspunkt?.kravdato
                          ? formaterStringDato(behandling.virkningstidspunkt.kravdato)
                          : 'Ikke fastsatt'}
                      </BodyShort>
                    </div>
                  )}
                  <div>
                    <Heading size="xsmall">Virkningstidspunkt</Heading>
                    <BodyShort spacing>
                      {behandling.virkningstidspunkt
                        ? formaterStringDato(behandling.virkningstidspunkt.dato)
                        : 'Ikke fastsatt'}
                    </BodyShort>
                  </div>
                </VStack>
              }
              vurdering={
                behandling.virkningstidspunkt
                  ? {
                      saksbehandler: behandling.virkningstidspunkt.kilde.ident,
                      tidspunkt: new Date(behandling.virkningstidspunkt.kilde.tidspunkt),
                    }
                  : undefined
              }
              redigerbar={redigerbar}
              lagreklikk={fastsett}
              avbrytklikk={reset}
              kommentar={behandling.virkningstidspunkt?.begrunnelse}
              defaultRediger={behandling.virkningstidspunkt === null}
            >
              <VStack gap="4">
                <VurderingsTitle title="Hva er virkningstidspunkt for behandlingen?" />

                {erBosattUtland && (
                  <ControlledDatoVelger
                    name="kravdato"
                    label={
                      <HelpTextWrapper>
                        Kravdato utland
                        <HelpText strategy="fixed">
                          Skriv inn kravdato for søknad i utlandet, som hentes fra SED P2100.
                        </HelpText>
                      </HelpTextWrapper>
                    }
                    fromDate={subYears(new Date(), 18)}
                    toDate={addYears(new Date(), 2)}
                    control={control}
                    errorVedTomInput="Kravdato kreves på bosatt utland saker"
                  />
                )}

                <ControlledMaanedVelger
                  name="virkningstidspunkt"
                  label="Virkningstidspunkt"
                  fromDate={hentMinimumsVirkningstidspunkt(
                    avdoede?.opplysning?.doedsdato,
                    erBosattUtland ? subYears(new Date(), 20) : new Date(behandling.soeknadMottattDato)
                  )}
                  toDate={addMonths(new Date(), 4)}
                  inputFormat="dd.MM.yyyy"
                  control={control}
                  validate={(maaned) => {
                    return validerVirkningstidspunkt(maaned as MonthValidationT)
                  }}
                />

                <SoeknadsoversiktTextArea
                  {...register('begrunnelse', {
                    required: {
                      value: true,
                      message: 'Begrunnelsen må fylles ut',
                    },
                  })}
                  error={errors.begrunnelse?.message}
                />

                {errorTekst !== '' ? <ErrorMessage>{errorTekst}</ErrorMessage> : null}
              </VStack>
            </VurderingsboksWrapper>
          )}
        </VurderingsContainerWrapper>
      </LovtekstMedLenke>
    </>
  )
}

export default Virkningstidspunkt

const HelpTextWrapper = styled.div`
  display: flex;
  gap: 0.3em;
`

export const Beskrivelse = styled.div`
  margin: 10px 0;
  max-width: 41em;
  white-space: pre-wrap;
`

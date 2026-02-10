/*
TODO: Aksel Box migration:
Could not migrate the following:
  - borderColor=border-neutral-subtle
*/

import { Alert, BodyShort, Box, Button, Heading, HStack, Link, Radio, Textarea, VStack } from '@navikt/ds-react'
import React, { useEffect, useState } from 'react'
import { JaNei, JaNeiRec, mapBooleanToJaNei } from '~shared/types/ISvar'
import { useForm } from 'react-hook-form'
import { useApiCall } from '~shared/hooks/useApiCall'
import {
  IBrevAktivitetspliktDto,
  IBrevAktivitetspliktRequest,
  lagreAktivitetspliktBrevdata,
  opprettAktivitetspliktsbrev,
} from '~shared/api/aktivitetsplikt'
import { ControlledRadioGruppe } from '~shared/components/radioGruppe/ControlledRadioGruppe'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { ExternalLinkIcon, PencilIcon } from '@navikt/aksel-icons'
import { Info } from '~components/behandling/soeknadsoversikt/Info'
import { useAktivitetspliktOppgaveVurdering } from '~components/aktivitetsplikt/AktivitetspliktOppgaveVurderingRoutes'
import { erOppgaveRedigerbar, Oppgavestatus } from '~shared/types/oppgave'
import { useDispatch } from 'react-redux'
import { setAktivtetspliktbrevdata, setBrevid } from '~store/reducers/AktivitetsplikReducer'
import { useNavigate } from 'react-router'
import { AktivitetspliktSteg } from '~components/aktivitetsplikt/stegmeny/AktivitetspliktStegmeny'
import { isPending, isSuccess, mapFailure } from '~shared/api/apiUtils'
import { ApiErrorAlert } from '~ErrorBoundary'
import { handlinger } from '~components/behandling/handlinger/typer'
import { Spraak } from '~shared/types/Brev'
import { formaterSpraak } from '~utils/formatering/formatering'
import { LoependeUnntakInfo } from '~components/aktivitetsplikt/brev/LoependeUnntakInfo'
import { formaterDatoMedTidspunkt } from '~utils/formatering/dato'
import { hentSisteIverksatteBehandlingId } from '~shared/api/sak'
import { ClickEvent, trackClick } from '~utils/analytics'

interface IBrevAktivitetsplikt {
  skalSendeBrev: JaNei
  utbetaling: JaNei
  begrunnelse?: string
  redusertEtterInntekt: JaNei
  spraak?: Spraak
}

function mapFromDto(brevdata: IBrevAktivitetspliktDto): Partial<IBrevAktivitetsplikt> {
  return {
    skalSendeBrev: mapBooleanToJaNei(brevdata.skalSendeBrev),
    utbetaling: mapBooleanToJaNei(brevdata.utbetaling),
    redusertEtterInntekt: mapBooleanToJaNei(brevdata.redusertEtterInntekt),
    spraak: brevdata.spraak,
  }
}

function mapToDto(brevdata: IBrevAktivitetsplikt): IBrevAktivitetspliktRequest {
  return {
    skalSendeBrev: brevdata.skalSendeBrev === JaNei.JA,
    utbetaling: brevdata.utbetaling ? brevdata.utbetaling === JaNei.JA : undefined,
    redusertEtterInntekt: brevdata.redusertEtterInntekt ? brevdata.redusertEtterInntekt === JaNei.JA : undefined,
    spraak: brevdata.spraak,
    begrunnelse: brevdata.skalSendeBrev === JaNei.NEI ? brevdata.begrunnelse : undefined,
  }
}

export const ValgForInfobrev = () => {
  const { oppgave, aktivtetspliktbrevdata, sak } = useAktivitetspliktOppgaveVurdering()
  const {
    handleSubmit,
    watch,
    control,
    resetField,
    reset,
    register,
    formState: { errors },
  } = useForm<IBrevAktivitetsplikt>({})

  const dispatch = useDispatch()
  const [lagrebrevdataStatus, lagrebrevdata, tilbakestillApiResult] = useApiCall(lagreAktivitetspliktBrevdata)
  const [hentSisteIverksatteBehandlingStatus, hentSisteIverksatteBehandling] = useApiCall(
    hentSisteIverksatteBehandlingId
  )

  const gammelFlyt = oppgave.status === Oppgavestatus.FERDIGSTILT && !aktivtetspliktbrevdata
  const [redigeres, setRedigeres] = useState<boolean>(!gammelFlyt)
  const brevdata = aktivtetspliktbrevdata

  const lagreBrevutfall = (data: IBrevAktivitetsplikt) => {
    const brevdatamappedToDo = mapToDto(data)
    lagrebrevdata(
      { oppgaveId: oppgave.id, brevdata: brevdatamappedToDo },
      (brevdata) => {
        dispatch(setAktivtetspliktbrevdata(brevdata))
        setRedigeres(false)
      },
      () => {}
    )
  }

  useEffect(() => {
    hentSisteIverksatteBehandling(sak.id)
  }, [sak.id])

  useEffect(() => {
    if (redigeres && !!aktivtetspliktbrevdata) {
      reset(mapFromDto(aktivtetspliktbrevdata))
    }
  }, [redigeres])

  const skalsendebrev = watch('skalSendeBrev')

  useEffect(() => {
    if (skalsendebrev === JaNei.NEI) {
      resetField('utbetaling')
      resetField('redusertEtterInntekt')
    }
    tilbakestillApiResult()
  }, [skalsendebrev])

  return (
    <Box paddingInline="space-16" paddingBlock="space-16" maxWidth="120rem">
      <VStack gap="space-4" maxWidth="30rem">
        <HStack gap="space-4" align="center">
          <Heading level="1" size="large">
            Valg for infobrev
          </Heading>
        </HStack>
        {isSuccess(hentSisteIverksatteBehandlingStatus) && (
          <Link
            onClick={() => trackClick(ClickEvent.SJEKKER_SISTE_BEREGNING)}
            href={`/behandling/${hentSisteIverksatteBehandlingStatus.data.id}/beregne`}
            as="a"
            target="_blank"
          >
            Vis siste beregning
            <ExternalLinkIcon aria-hidden />
          </Link>
        )}
        {redigeres ? (
          <form onSubmit={handleSubmit(lagreBrevutfall)}>
            <VStack gap="space-4">
              <ControlledRadioGruppe
                name="skalSendeBrev"
                control={control}
                legend="Skal sende brev"
                errorVedTomInput="Du må velge om du skal sende brev eller ikke"
                radios={
                  <>
                    <Radio value={JaNei.JA}>{JaNeiRec.JA}</Radio>
                    <Radio value={JaNei.NEI}>{JaNeiRec.NEI}</Radio>
                  </>
                }
              />
              {skalsendebrev === JaNei.NEI && (
                <Textarea
                  label="Begrunnelse"
                  {...register('begrunnelse', {
                    required: {
                      value: true,
                      message: 'Du må si hvorfor brev ikke skal sendes',
                    },
                  })}
                  error={errors?.begrunnelse?.message}
                />
              )}
              {skalsendebrev === JaNei.JA && (
                <>
                  <ControlledRadioGruppe
                    name="utbetaling"
                    control={control}
                    legend="Har bruker utbetaling?"
                    errorVedTomInput="Du må velge ja eller nei"
                    radios={
                      <>
                        <Radio value={JaNei.JA}>{JaNeiRec.JA}</Radio>
                        <Radio value={JaNei.NEI}>{JaNeiRec.NEI}</Radio>
                      </>
                    }
                  />

                  <ControlledRadioGruppe
                    name="redusertEtterInntekt"
                    control={control}
                    legend="Er ytelsen til bruker redusert på grunn av inntekt?"
                    errorVedTomInput="Du må velge ja eller nei"
                    radios={
                      <>
                        <Radio value={JaNei.JA}>{JaNeiRec.JA}</Radio>
                        <Radio value={JaNei.NEI}>{JaNeiRec.NEI}</Radio>
                      </>
                    }
                  />

                  <ControlledRadioGruppe
                    name="spraak"
                    control={control}
                    legend="Hvilken målform skal brevet ha?"
                    errorVedTomInput="Du må velge målformen til brevet."
                    radios={
                      <>
                        <Radio value={Spraak.NB}>Bokmål</Radio>
                        <Radio value={Spraak.NN}>Nynorsk</Radio>
                        <Radio value={Spraak.EN}>Engelsk</Radio>
                      </>
                    }
                  />
                </>
              )}

              {isFailureHandler({
                apiResult: lagrebrevdataStatus,
                errorMessage: 'Kan ikke lagre valg for infobrevet',
              })}
              <Box>
                <Button size="small" type="submit" loading={isPending(lagrebrevdataStatus)} variant="primary">
                  Lagre valg for infobrev
                </Button>
              </Box>
            </VStack>
          </form>
        ) : (
          <VStack gap="space-4">
            {!!brevdata && (
              <HStack gap="space-4">
                <Info label="Skal sende brev" tekst={brevdata.skalSendeBrev ? JaNeiRec.JA : JaNeiRec.NEI} />
                {!brevdata.skalSendeBrev && brevdata.begrunnelse && (
                  <Info label="Begrunnelse" tekst={brevdata.begrunnelse} />
                )}
                {brevdata.skalSendeBrev && (
                  <>
                    <Info label="Utbetaling" tekst={brevdata.utbetaling ? JaNeiRec.JA : JaNeiRec.NEI} />
                    <Info
                      label="Redusert etter inntekt"
                      tekst={brevdata.redusertEtterInntekt ? JaNeiRec.JA : JaNeiRec.NEI}
                    />
                    <Info label="Målform" tekst={brevdata.spraak ? formaterSpraak(brevdata.spraak) : '-'} />
                  </>
                )}
                <BodyShort>
                  Sist endret {formaterDatoMedTidspunkt(new Date(brevdata.kilde.tidspunkt))} av {brevdata.kilde.ident}
                </BodyShort>
              </HStack>
            )}
            {erOppgaveRedigerbar(oppgave.status) && (
              <>
                <Box>
                  <Button
                    type="button"
                    size="small"
                    icon={<PencilIcon aria-hidden />}
                    variant="secondary"
                    onClick={() => setRedigeres(true)}
                  >
                    Rediger
                  </Button>
                </Box>
                {aktivtetspliktbrevdata?.brevId && (
                  <Alert variant="info">Hvis valgene redigeres vil innholdet i brevet tilbakestilles.</Alert>
                )}
                <LoependeUnntakInfo />
              </>
            )}
          </VStack>
        )}
        {gammelFlyt && (
          <BodyShort>
            Denne oppgaven er fra en gammel vurdering, derfor har den ingen brevvalg eller muligheten til å lagre dette.
            For å se på brevet må du gå til brevoversikten til brukeren.
          </BodyShort>
        )}
        <NesteEllerOpprettBrevValg gammelFlyt={gammelFlyt} />
      </VStack>
    </Box>
  )
}

interface NesteEllerOpprettBrevValgProps {
  gammelFlyt: boolean
}

function NesteEllerOpprettBrevValg({ gammelFlyt }: NesteEllerOpprettBrevValgProps) {
  const { oppgave, aktivtetspliktbrevdata } = useAktivitetspliktOppgaveVurdering()
  const navigate = useNavigate()

  const [opprettBrevStatus, opprettBrevCall] = useApiCall(opprettAktivitetspliktsbrev)
  const dispatch = useDispatch()
  const erRedigerbar = erOppgaveRedigerbar(oppgave.status)
  const skalOppretteBrev = erRedigerbar && aktivtetspliktbrevdata?.skalSendeBrev && !aktivtetspliktbrevdata.brevId

  function opprettBrev() {
    opprettBrevCall(
      {
        oppgaveId: oppgave.id,
      },
      (brevId) => {
        dispatch(setBrevid(brevId.brevId))
        navigate(`../${AktivitetspliktSteg.OPPSUMMERING_OG_BREV}`)
      }
    )
  }

  return (
    <Box paddingBlock="space-4 space-0" borderWidth="1 0 0 0" borderColor="border-neutral-subtle">
      <HStack gap="space-4" justify="center">
        <Button
          variant="secondary"
          onClick={() => {
            navigate(`../${AktivitetspliktSteg.VURDERING}`)
          }}
        >
          {handlinger.TILBAKE.navn}
        </Button>
        {mapFailure(opprettBrevStatus, (error) => (
          <ApiErrorAlert>Kunne ikke opprette brev: {error.detail}</ApiErrorAlert>
        ))}
        {!gammelFlyt && (
          <>
            {skalOppretteBrev ? (
              <Button onClick={opprettBrev} loading={isPending(opprettBrevStatus)}>
                Opprett og gå til brev
              </Button>
            ) : (
              <Button onClick={() => navigate(`../${AktivitetspliktSteg.OPPSUMMERING_OG_BREV}`)}>Neste</Button>
            )}
          </>
        )}
      </HStack>
    </Box>
  )
}

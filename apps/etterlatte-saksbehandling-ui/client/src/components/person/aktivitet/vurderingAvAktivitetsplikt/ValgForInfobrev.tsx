import { Box, Button, Heading, HStack, Radio, VStack } from '@navikt/ds-react'
import React, { useEffect, useState } from 'react'
import { JaNei, JaNeiRec } from '~shared/types/ISvar'
import { useForm } from 'react-hook-form'
import { isPending } from '@reduxjs/toolkit'
import { useApiCall } from '~shared/hooks/useApiCall'
import {
  IBrevAktivitetspliktRequest,
  lagreAktivitetspliktBrevdata,
  opprettAktivitetspliktsbrev,
} from '~shared/api/aktivitetsplikt'
import { ControlledRadioGruppe } from '~shared/components/radioGruppe/ControlledRadioGruppe'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { PencilIcon } from '@navikt/aksel-icons'
import { Info } from '~components/behandling/soeknadsoversikt/Info'
import { useAktivitetspliktOppgaveVurdering } from '~components/aktivitetsplikt/OppgaveVurderingRoute'
import { erOppgaveRedigerbar } from '~shared/types/oppgave'
import { useDispatch } from 'react-redux'
import { setAktivtetspliktbrevdata, setBrevid } from '~store/reducers/Aktivitetsplikt12mnd'
import { useNavigate } from 'react-router'
import { AktivitetspliktSteg } from '~components/aktivitetsplikt/stegmeny/AktivitetspliktStegmeny'
import { mapFailure } from '~shared/api/apiUtils'
import { ApiErrorAlert } from '~ErrorBoundary'
import { handlinger } from '~components/behandling/handlinger/typer'

interface IBrevAktivitetsplikt {
  skalSendeBrev: JaNei
  utbetaling: JaNei
  redusertEtterInntekt: JaNei
}

function mapToDto(brevdata: IBrevAktivitetsplikt): IBrevAktivitetspliktRequest {
  return {
    skalSendeBrev: brevdata.skalSendeBrev === JaNei.JA,
    utbetaling: brevdata.utbetaling ? brevdata.utbetaling === JaNei.JA : undefined,
    redusertEtterInntekt: brevdata.redusertEtterInntekt ? brevdata.redusertEtterInntekt === JaNei.JA : undefined,
  }
}

export const ValgForInfobrev = () => {
  const { oppgave, aktivtetspliktbrevdata } = useAktivitetspliktOppgaveVurdering()
  const { handleSubmit, watch, control, resetField } = useForm<IBrevAktivitetsplikt>({})

  const dispatch = useDispatch()
  const [lagrebrevdataStatus, lagrebrevdata, tilbakestillApiResult] = useApiCall(lagreAktivitetspliktBrevdata)
  const [redigeres, setRedigeres] = useState<boolean>(!aktivtetspliktbrevdata)
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
  console.log('oppgave: ', oppgave.status)

  const skalsendebrev = watch('skalSendeBrev')

  useEffect(() => {
    if (skalsendebrev === JaNei.NEI) {
      resetField('utbetaling')
      resetField('redusertEtterInntekt')
    }
    tilbakestillApiResult()
  }, [skalsendebrev])

  return (
    <Box paddingInline="16" paddingBlock="16" maxWidth="120rem">
      <VStack gap="4" maxWidth="30rem">
        <HStack gap="4" align="center">
          <Heading level="1" size="large">
            Valg for infobrev
          </Heading>
        </HStack>
        {redigeres ? (
          <form onSubmit={handleSubmit(lagreBrevutfall)}>
            <VStack gap="4">
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
          <VStack gap="4">
            {!!brevdata && (
              <HStack gap="4">
                <Info label="Skal sende brev" tekst={brevdata.skalSendeBrev ? JaNeiRec.JA : JaNeiRec.NEI} />
                {brevdata.skalSendeBrev && (
                  <>
                    <Info label="Utbetaling" tekst={brevdata.utbetaling ? JaNeiRec.JA : JaNeiRec.NEI} />
                    <Info
                      label="Redusert etter inntekt"
                      tekst={brevdata.redusertEtterInntekt ? JaNeiRec.JA : JaNeiRec.NEI}
                    />
                  </>
                )}
              </HStack>
            )}
            {erOppgaveRedigerbar(oppgave.status) && (
              <Box>
                <Button
                  type="button"
                  size="small"
                  icon={<PencilIcon />}
                  variant="secondary"
                  onClick={() => setRedigeres(true)}
                >
                  Rediger
                </Button>
              </Box>
            )}
          </VStack>
        )}
        <NesteEllerOpprettBrevValg />
      </VStack>
    </Box>
  )
}

function NesteEllerOpprettBrevValg() {
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
    <Box paddingBlock="4 0" borderWidth="1 0 0 0" borderColor="border-subtle">
      <HStack gap="4" justify="center">
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
        {skalOppretteBrev ? (
          <Button onClick={opprettBrev} loading={isPending(opprettBrevStatus)}>
            Opprett og gå til brev
          </Button>
        ) : (
          <Button onClick={() => navigate(`../${AktivitetspliktSteg.OPPSUMMERING_OG_BREV}`)}>Neste</Button>
        )}
      </HStack>
    </Box>
  )
}

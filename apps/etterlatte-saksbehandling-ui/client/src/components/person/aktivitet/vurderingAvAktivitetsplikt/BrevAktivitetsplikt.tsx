import { Button, Heading, HStack, Radio, VStack } from '@navikt/ds-react'
import React, { useEffect, useState } from 'react'
import { RadioGroupWrapper } from '~components/behandling/vilkaarsvurdering/Vurdering'
import { JaNei, JaNeiRec } from '~shared/types/ISvar'
import { useForm } from 'react-hook-form'
import { isPending } from '@reduxjs/toolkit'
import { useApiCall } from '~shared/hooks/useApiCall'
import { IBrevAktivitetspliktRequest, lagreAktivitetspliktBrevdata } from '~shared/api/aktivitetsplikt'
import { ControlledRadioGruppe } from '~shared/components/radioGruppe/ControlledRadioGruppe'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { PencilIcon } from '@navikt/aksel-icons'
import { Info } from '~components/behandling/soeknadsoversikt/Info'
import { useAktivitetspliktOppgaveVurdering } from '~components/aktivitetsplikt/OppgaveVurderingRoute'
import { erOppgaveRedigerbar } from '~shared/types/oppgave'

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

export const BrevAktivitetsplikt = () => {
  const { oppgave, aktivtetspliktbrevdata } = useAktivitetspliktOppgaveVurdering()
  const { handleSubmit, watch, control, resetField } = useForm<IBrevAktivitetsplikt>({})

  const [lagrebrevdataStatus, lagrebrevdata, tilbakestillApiResult] = useApiCall(lagreAktivitetspliktBrevdata)
  const [redigeres, setRedigeres] = useState<boolean>(!aktivtetspliktbrevdata)
  const [brevdata, oppdaterBrevdata] = useState<IBrevAktivitetspliktRequest | undefined>(aktivtetspliktbrevdata)

  const lagreBrevutfall = (data: IBrevAktivitetsplikt) => {
    const brevdatamappedToDo = mapToDto(data)
    lagrebrevdata(
      { oppgaveId: oppgave.id, brevdata: brevdatamappedToDo },
      () => {
        oppdaterBrevdata(brevdatamappedToDo)
        setRedigeres(false)
      },
      () => {}
    )
  }

  const skalsendebrev = watch('skalSendeBrev')

  useEffect(() => {
    if (skalsendebrev === JaNei.NEI) {
      resetField('utbetaling')
      resetField('redusertEtterInntekt')
    }
    tilbakestillApiResult()
  }, [skalsendebrev])

  return (
    <VStack gap="4" maxWidth="30rem">
      <HStack gap="4" align="center">
        <Heading size="small">Valg for infobrev</Heading>
      </HStack>
      {redigeres ? (
        <form onSubmit={handleSubmit(lagreBrevutfall)}>
          <RadioGroupWrapper>
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
          </RadioGroupWrapper>

          {skalsendebrev === JaNei.JA && (
            <>
              <RadioGroupWrapper>
                <ControlledRadioGruppe
                  name="utbetaling"
                  control={control}
                  legend="Skal bruker få utbetaling"
                  errorVedTomInput="Du må velge ja eller nei"
                  radios={
                    <>
                      <Radio value={JaNei.JA}>{JaNeiRec.JA}</Radio>
                      <Radio value={JaNei.NEI}>{JaNeiRec.NEI}</Radio>
                    </>
                  }
                />
              </RadioGroupWrapper>

              <RadioGroupWrapper>
                <ControlledRadioGruppe
                  name="redusertEtterInntekt"
                  control={control}
                  legend="Skal bruker få redusert inntekt "
                  errorVedTomInput="Du må velge ja eller nei"
                  radios={
                    <>
                      <Radio value={JaNei.JA}>{JaNeiRec.JA}</Radio>
                      <Radio value={JaNei.NEI}>{JaNeiRec.NEI}</Radio>
                    </>
                  }
                />
              </RadioGroupWrapper>
            </>
          )}

          {isFailureHandler({
            apiResult: lagrebrevdataStatus,
            errorMessage: 'Kan ikke lagre valg for infobrevet',
          })}
          <Button type="submit" loading={isPending(lagrebrevdataStatus)} variant="primary">
            Lagre valg for infobrev
          </Button>
        </form>
      ) : (
        <div>
          <div>
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
          </div>
          {erOppgaveRedigerbar(oppgave.status) && (
            <Button
              type="button"
              size="small"
              icon={<PencilIcon />}
              variant="secondary"
              onClick={() => setRedigeres(true)}
            >
              Rediger
            </Button>
          )}
        </div>
      )}
    </VStack>
  )
}

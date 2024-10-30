import { Button, Heading, HStack, Radio, VStack } from '@navikt/ds-react'
import React, { useEffect, useState } from 'react'
import { RadioGroupWrapper } from '~components/behandling/vilkaarsvurdering/Vurdering'
import { JaNei, JaNeiRec } from '~shared/types/ISvar'
import { useForm } from 'react-hook-form'
import { isPending } from '@reduxjs/toolkit'
import { useApiCall } from '~shared/hooks/useApiCall'
import { IBrevAktivitetspliktDto, lagreAktivitetspliktBrevdata } from '~shared/api/aktivitetsplikt'
import { ControlledRadioGruppe } from '~shared/components/radioGruppe/ControlledRadioGruppe'
import { OppgaveDTO } from '~shared/types/oppgave'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { PencilIcon } from '@navikt/aksel-icons'
import { Info } from '~components/behandling/soeknadsoversikt/Info'

interface IBrevAktivitetsplikt {
  skalSendeBrev: JaNei
  utbetaling: JaNei
  redusertEtterInntekt: JaNei
}

function mapToDto(brevdata: IBrevAktivitetsplikt): IBrevAktivitetspliktDto {
  return {
    skalSendeBrev: brevdata.skalSendeBrev === JaNei.JA,
    utbetaling: brevdata.utbetaling === JaNei.JA,
    redusertEtterInntekt: brevdata.redusertEtterInntekt === JaNei.JA,
  }
}

export const BrevAktivitetsplikt = ({
  oppgave,
  aktivtetspliktbrevdata,
}: {
  oppgave: OppgaveDTO
  aktivtetspliktbrevdata?: IBrevAktivitetspliktDto
}) => {
  const { handleSubmit, watch, control, resetField } = useForm<IBrevAktivitetsplikt>({})

  const [lagrebrevdataStatus, lagrebrevdata, tilbakestillApiResult] = useApiCall(lagreAktivitetspliktBrevdata)
  const [redigeres, setRedigeres] = useState<boolean>(!aktivtetspliktbrevdata)

  const lagreBrevutfall = (data: IBrevAktivitetsplikt) => {
    lagrebrevdata(
      { oppgaveId: oppgave.id, brevdata: mapToDto(data) },
      () => {
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
        <Heading size="small">Brev data</Heading>
      </HStack>
      {redigeres ? (
        <>
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
            errorMessage: 'Kan ikke lagre brevdata',
          })}
          <Button
            loading={isPending(lagrebrevdataStatus)}
            variant="primary"
            type="button"
            onClick={handleSubmit(lagreBrevutfall)}
          >
            Lagre brevvurdering
          </Button>
        </>
      ) : (
        <div>
          <div>
            {aktivtetspliktbrevdata && (
              <HStack gap="4">
                <Info
                  label="Skal sende brev"
                  tekst={aktivtetspliktbrevdata.skalSendeBrev ? JaNeiRec.JA : JaNeiRec.NEI}
                />
                <Info label="Utbetaling" tekst={aktivtetspliktbrevdata.utbetaling ? JaNeiRec.JA : JaNeiRec.NEI} />
                <Info
                  label="Redusert etter inntekt "
                  tekst={aktivtetspliktbrevdata.redusertEtterInntekt ? JaNeiRec.JA : JaNeiRec.NEI}
                />
              </HStack>
            )}
          </div>
          <Button
            type="button"
            size="small"
            icon={<PencilIcon />}
            variant="secondary"
            onClick={() => setRedigeres(true)}
          >
            Rediger
          </Button>
        </div>
      )}
    </VStack>
  )
}

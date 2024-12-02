import { Box, Button, Heading, HStack, Modal, Select, Textarea, VStack } from '@navikt/ds-react'
import { erOppgaveRedigerbar, OppgaveDTO, Oppgavestatus } from '~shared/types/oppgave'
import { useState } from 'react'
import { ClockDashedIcon, ClockIcon } from '@navikt/aksel-icons'
import { useForm } from 'react-hook-form'
import { EndrePaaVentRequest, redigerFristApi, settOppgavePaaVentApi } from '~shared/api/oppgaver'
import { ControlledDatoVelger } from '~shared/components/datoVelger/ControlledDatoVelger'
import { datoIMorgen, datoToAarFramITid } from '~components/oppgavebenk/frist/utils'
import { useApiCall } from '~shared/hooks/useApiCall'
import { useAppDispatch } from '~store/Store'
import { settOppgave } from '~store/reducers/OppgaveReducer'
import { isPending } from '~shared/api/apiUtils'

enum AarsakForAaSettePaaVent {
  OPPLYSNING_FRA_BRUKER = 'Opplysning fra bruker',
  OPPLYSNING_FRA_ANDRE = 'Opplysning fra andre',
  KRAVGRUNNLAG_SPERRET = 'Kravgrunnlag sperret',
  ANNET = 'Annet',
}

interface SettPaaVentSkjema extends EndrePaaVentRequest {
  nyFrist: Date
}

export const SettPaaVentModal = ({ oppgave }: { oppgave: OppgaveDTO }) => {
  const [aapen, setAapen] = useState<boolean>(false)

  const dispatch = useAppDispatch()

  const [settOppgavePaaVentResult, settOppgavePaaVentFunc] = useApiCall(settOppgavePaaVentApi)
  const [redigerFristResult, redigerFristFunc] = useApiCall(redigerFristApi)

  const {
    register,
    control,
    handleSubmit,
    formState: { errors },
  } = useForm<SettPaaVentSkjema>({
    defaultValues: { merknad: oppgave.merknad },
  })

  if (!erOppgaveRedigerbar(oppgave.status)) return null

  const settPaaEllerAvVent = (data: SettPaaVentSkjema) => {
    redigerFristFunc({ oppgaveId: oppgave.id, redigerFristRequest: { frist: data.nyFrist, versjon: null } }, () => {
      settOppgavePaaVentFunc(
        {
          oppgaveId: oppgave.id,
          settPaaVentRequest: {
            aarsak: data.aarsak,
            merknad: data.merknad,
            paaVent: oppgave.status !== Oppgavestatus.PAA_VENT,
          },
        },
        (oppgave) => {
          dispatch(settOppgave(oppgave))
          setAapen(false)
        }
      )
    })
  }

  return (
    <>
      <Button
        size="small"
        variant="secondary"
        icon={oppgave.status === Oppgavestatus.PAA_VENT ? <ClockDashedIcon aria-hidden /> : <ClockIcon aria-hidden />}
        iconPosition="right"
        onClick={() => setAapen(true)}
      >
        {oppgave.status === Oppgavestatus.PAA_VENT ? 'Ta av vent' : 'Sett på vent'}
      </Button>

      <Modal open={aapen} onClose={() => setAapen(false)} aria-labelledby="modal for å sette oppgave på vent">
        <Modal.Header>
          <Heading size="medium" spacing>
            {oppgave.status === Oppgavestatus.PAA_VENT ? 'Ta av vent' : 'Sett på vent'}
          </Heading>
        </Modal.Header>
        <Modal.Body>
          <Box width="25rem">
            <form onSubmit={handleSubmit(settPaaEllerAvVent)}>
              <VStack gap="4">
                {oppgave.status !== Oppgavestatus.PAA_VENT && (
                  <Select
                    {...register('aarsak', {
                      required: {
                        value: true,
                        message: 'Du må velge en årsak',
                      },
                    })}
                    label="Årsak"
                    error={errors.aarsak?.message}
                  >
                    <option value="">Velg årsak</option>
                    {Object.entries(AarsakForAaSettePaaVent).map(([aarsak, beskrivelse]) => (
                      <option key={aarsak} value={aarsak}>
                        {beskrivelse}
                      </option>
                    ))}
                  </Select>
                )}
                <Textarea {...register('merknad')} label="Merknad" />

                {oppgave.status !== Oppgavestatus.PAA_VENT && (
                  <ControlledDatoVelger
                    name="nyFrist"
                    label="Ny frist"
                    control={control}
                    errorVedTomInput="Du må velge en ny frist"
                    fromDate={datoIMorgen()}
                    toDate={datoToAarFramITid()}
                  />
                )}

                <HStack gap="2" justify="end">
                  <Button
                    type="button"
                    variant="secondary"
                    disabled={isPending(settOppgavePaaVentResult) || isPending(redigerFristResult)}
                    onClick={() => setAapen(false)}
                  >
                    Avbryt
                  </Button>
                  <Button type="submit" loading={isPending(settOppgavePaaVentResult) || isPending(redigerFristResult)}>
                    {oppgave.status === Oppgavestatus.PAA_VENT ? 'Ta av vent' : 'Sett på vent'}
                  </Button>
                </HStack>
              </VStack>
            </form>
          </Box>
        </Modal.Body>
      </Modal>
    </>
  )
}

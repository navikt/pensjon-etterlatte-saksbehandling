import { Button, Heading, Modal, Select, TextField, VStack } from '@navikt/ds-react'
import { erOppgaveRedigerbar, OppgaveDTO, Oppgavestatus } from '~shared/types/oppgave'
import { useState } from 'react'
import { ClockDashedIcon, ClockIcon } from '@navikt/aksel-icons'
import { useForm } from 'react-hook-form'
import { EndrePaaVentRequest } from '~shared/api/oppgaver'
import { ControlledDatoVelger } from '~shared/components/datoVelger/ControlledDatoVelger'
import { datoIMorgen, datoToAarFramITid } from '~components/oppgavebenk/frist/utils'

enum AarsakForAaSettePaaVent {
  OPPLYSNING_FRA_BRUKER = 'Opplysning fra bruker',
  OPPLYSNING_FRA_ANDRE = 'Opplysning fra andre',
  KRAVGRUNNLAG_SPERRET = 'Kravgrunnlag sperret',
  ANNET = 'Annet',
}

interface SettPaaVentSkjema extends EndrePaaVentRequest {
  nyFrist: string
}

export const SettPaaVentModal = ({ oppgave }: { oppgave: OppgaveDTO }) => {
  const [aapen, setAapen] = useState<boolean>(false)

  const { register, control } = useForm<SettPaaVentSkjema>({ defaultValues: { merknad: oppgave.merknad } })

  if (!erOppgaveRedigerbar(oppgave.status)) return null

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
        <Modal.Body>
          <Modal.Header>
            <Heading size="medium" spacing>
              {oppgave.status === Oppgavestatus.PAA_VENT ? 'Ta av vent' : 'Sett på vent'}
            </Heading>
          </Modal.Header>
          <form>
            <VStack gap="2">
              {oppgave.status !== Oppgavestatus.PAA_VENT && (
                <Select
                  {...register('aarsak', {
                    required: {
                      value: true,
                      message: 'Du må velge en årsak',
                    },
                  })}
                  label="Årsak"
                >
                  <option value="">Velg årsak</option>
                  {Object.entries(AarsakForAaSettePaaVent).map(([aarsak, beskrivelse]) => (
                    <option key={aarsak} value={aarsak}>
                      {beskrivelse}
                    </option>
                  ))}
                </Select>
              )}
              <TextField {...register('merknad')} label="Merknad" />

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
            </VStack>
          </form>
        </Modal.Body>
      </Modal>
    </>
  )
}

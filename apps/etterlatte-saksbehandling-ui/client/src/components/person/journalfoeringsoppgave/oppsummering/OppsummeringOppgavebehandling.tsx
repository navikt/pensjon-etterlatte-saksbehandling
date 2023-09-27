import { Button, Detail, Heading, Tag } from '@navikt/ds-react'
import { useJournalfoeringOppgave } from '~components/person/journalfoeringsoppgave/useJournalfoeringOppgave'
import AvbrytBehandleJournalfoeringOppgave from '~components/person/journalfoeringsoppgave/AvbrytBehandleJournalfoeringOppgave'
import { Navigate, useNavigate } from 'react-router-dom'
import { Info } from '~components/behandling/soeknadsoversikt/Info'
import { SakType } from '~shared/types/sak'
import { formaterSakstype } from '~utils/formattering'
import { InfoList } from '~components/behandling/soeknadsoversikt/styled'
import { FormWrapper } from '~components/person/journalfoeringsoppgave/BehandleJournalfoeringOppgave'
import FullfoerOppgaveModal from '~components/person/journalfoeringsoppgave/oppsummering/FullfoerOppgaveModal'
import { FlexRow } from '~shared/styled'

export default function OppsummeringOppgavebehandling() {
  const { behandlingBehov, oppgave } = useJournalfoeringOppgave()

  const navigate = useNavigate()

  const tilbake = () => navigate('../nybehandling', { relative: 'path' })

  const persongalleri = behandlingBehov?.persongalleri
  if (!behandlingBehov || !oppgave || !persongalleri) {
    return <Navigate to="../nybehandling" relative="path" />
  }

  return (
    <FormWrapper column>
      <Heading size="medium" spacing>
        Opprett behandling fra oppgave
      </Heading>

      <InfoList>
        <div>
          <Tag variant="success" size="medium">
            {formaterSakstype(oppgave.sakType)}
          </Tag>
        </div>

        <Info label="Søker" tekst={persongalleri.soeker} />
        <Info label="Innsender" tekst={persongalleri.innsender || <i>Ikke oppgitt</i>} />

        {oppgave.sakType === SakType.BARNEPENSJON &&
          persongalleri.gjenlevende!!.map((gjenlevende) => (
            <Info key={gjenlevende} label="Gjenlevende" tekst={gjenlevende || ''} />
          ))}

        {persongalleri.avdoed!!.map((avdoed) => (
          <Info key={avdoed} label="Avdød" tekst={avdoed} />
        ))}

        {!persongalleri.soesken?.length && <Detail>Ingen barn/søsken oppgitt</Detail>}
        {persongalleri.soesken?.map((soeskenEllerBarn) =>
          oppgave!!.sakType === SakType.BARNEPENSJON ? (
            <Info key={soeskenEllerBarn} label="Søsken" tekst={soeskenEllerBarn || ''} />
          ) : (
            <Info key={soeskenEllerBarn} label="Barn" tekst={soeskenEllerBarn || ''} />
          )
        )}
      </InfoList>

      <div>
        <FlexRow justify="center" $spacing>
          <Button variant="secondary" onClick={tilbake}>
            Tilbake
          </Button>

          <FullfoerOppgaveModal oppgave={oppgave} behandlingBehov={behandlingBehov} />
        </FlexRow>
        <FlexRow justify="center">
          <AvbrytBehandleJournalfoeringOppgave />
        </FlexRow>
      </div>
    </FormWrapper>
  )
}

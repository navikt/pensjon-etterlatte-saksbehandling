import { useAktivitetspliktOppgaveVurdering } from '~components/aktivitetsplikt/OppgaveVurderingRoute'
import { Sidebar, SidebarPanel } from '~shared/components/Sidebar'
import { BodyShort, Heading, Label, VStack } from '@navikt/ds-react'
import { formaterOppgaveStatus } from '~utils/formatering/formatering'
import { SakTypeTag } from '~shared/tags/SakTypeTag'
import { DokumentlisteLiten } from '~components/person/dokumenter/DokumentlisteLiten'

export function AktivitetspliktSidemeny() {
  const { oppgave } = useAktivitetspliktOppgaveVurdering()
  return (
    <Sidebar>
      <SidebarPanel $border>
        <Heading size="small">Vurdering av aktivitet</Heading>
        <Heading size="xsmall">{formaterOppgaveStatus(oppgave.status)}</Heading>

        <VStack gap="2">
          <div>
            <SakTypeTag sakType={oppgave.sakType} />
          </div>
          <div>
            <Label>Saksbehandler</Label>
            <BodyShort>{oppgave.saksbehandler?.navn ?? '-'}</BodyShort>
          </div>
        </VStack>
      </SidebarPanel>

      {oppgave.fnr && <DokumentlisteLiten fnr={oppgave.fnr} />}
    </Sidebar>
  )
}

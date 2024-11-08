import { Box, Button, Heading, HStack } from '@navikt/ds-react'
import { useSidetittel } from '~shared/hooks/useSidetittel'
import { useAktivitetspliktOppgaveVurdering } from '~components/aktivitetsplikt/OppgaveVurderingRoute'
import React from 'react'
import { Aktivitetspliktbrev } from '~components/aktivitetsplikt/brev/AktivitetspliktBrev'
import { UtenBrevVisning } from '~components/aktivitetsplikt/brev/UtenBrevVisning'
import { isPending, Result } from '~shared/api/apiUtils'
import { useNavigate } from 'react-router-dom'
import { AktivitetspliktSteg } from '~components/aktivitetsplikt/stegmeny/AktivitetspliktStegmeny'
import { handlinger } from '~components/behandling/handlinger/typer'

export function VurderingInfoBrevOgOppsummering() {
  useSidetittel('Aktivitetsplikt brev og oppsummering')

  const { aktivtetspliktbrevdata } = useAktivitetspliktOppgaveVurdering()

  if (!aktivtetspliktbrevdata?.brevId) {
    return (
      <Box paddingInline="16" paddingBlock="16">
        <Heading size="large">Vurdering av {aktivtetspliktbrevdata?.skalSendeBrev ? 'brev' : 'oppgave'}</Heading>
        <UtenBrevVisning />
      </Box>
    )
  }

  return <Aktivitetspliktbrev brevId={aktivtetspliktbrevdata.brevId} />
}

export function InfobrevKnapperad(props: {
  ferdigstill?: () => void
  status?: Result<unknown>
  tekst?: string
  children?: React.ReactElement
}) {
  const navigate = useNavigate()
  return (
    <Box paddingBlock="4 0" borderWidth="1 0 0 0" borderColor="border-subtle">
      {props.children}
      <HStack gap="4" justify="center">
        <Button
          variant="secondary"
          onClick={() => {
            navigate(`../${AktivitetspliktSteg.BREVVALG}`)
          }}
        >
          {handlinger.TILBAKE.navn}
        </Button>
        {props.ferdigstill && props.status && (
          <Button onClick={props.ferdigstill} loading={isPending(props.status)}>
            {props.tekst ? props.tekst : 'Ferdigstill brev'}
          </Button>
        )}
      </HStack>
    </Box>
  )
}

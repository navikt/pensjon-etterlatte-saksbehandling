import { Box, Button, Heading, HStack } from '@navikt/ds-react'
import { useSidetittel } from '~shared/hooks/useSidetittel'
import { useAktivitetspliktOppgaveVurdering } from '~components/aktivitetsplikt/AktivitetspliktOppgaveVurderingRoutes'
import React from 'react'
import { Aktivitetspliktbrev } from '~components/aktivitetsplikt/brev/AktivitetspliktBrev'
import { UtenBrevVisning } from '~components/aktivitetsplikt/brev/UtenBrevVisning'
import { useNavigate } from 'react-router-dom'
import { AktivitetspliktSteg } from '~components/aktivitetsplikt/stegmeny/AktivitetspliktStegmeny'
import { handlinger } from '~components/behandling/handlinger/typer'

export function VurderingInfoBrevOgOppsummering() {
  useSidetittel('Aktivitetsplikt brev og oppsummering')

  const { aktivtetspliktbrevdata } = useAktivitetspliktOppgaveVurdering()

  if (!aktivtetspliktbrevdata?.brevId) {
    return (
      <Box paddingInline="space-64" paddingBlock="space-64">
        <Heading size="large">Vurdering av {aktivtetspliktbrevdata?.skalSendeBrev ? 'brev' : 'oppgave'}</Heading>
        <UtenBrevVisning />
      </Box>
    )
  }

  return <Aktivitetspliktbrev brevId={aktivtetspliktbrevdata.brevId} />
}

export function InfobrevKnapperad(props: { children?: React.ReactElement }) {
  const navigate = useNavigate()
  return (
    <Box paddingBlock="space-16 space-0" borderWidth="1 0 0 0" borderColor="neutral-subtle">
      <HStack gap="space-16" justify="center">
        <Button
          variant="secondary"
          onClick={() => {
            navigate(`../${AktivitetspliktSteg.BREVVALG}`)
          }}
        >
          {handlinger.TILBAKE.navn}
        </Button>
        {props.children}
      </HStack>
    </Box>
  )
}

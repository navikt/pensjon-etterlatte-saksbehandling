import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { StatusBar } from '~shared/statusbar/Statusbar'
import { Container } from '~shared/styled'
import { SakOversikt } from './SakOversikt'
import Spinner from '~shared/Spinner'
import { isFailure, isPending, isSuccess, useApiCall } from '~shared/hooks/useApiCall'
import { BodyShort, Tabs } from '@navikt/ds-react'
import { getPerson } from '~shared/api/grunnlag'
import { GYLDIG_FNR } from '~utils/fnr'
import NavigerTilbakeMeny from '~components/person/NavigerTilbakeMeny'
import { BulletListIcon, FileTextIcon } from '@navikt/aksel-icons'
import { Dokumentoversikt } from './dokumenter/dokumentoversikt'

enum Fane {
  SAKER = 'SAKER',
  DOKUMENTER = 'DOKUMENTER',
}

export const Person = () => {
  const [personStatus, hentPerson] = useApiCall(getPerson)
  const [fane, setFane] = useState(Fane.SAKER)

  const { fnr } = useParams()

  useEffect(() => {
    if (GYLDIG_FNR(fnr)) {
      hentPerson(fnr!!)
    }
  }, [fnr])

  if (isFailure(personStatus)) {
    return (
      <Container>
        <BodyShort>{!GYLDIG_FNR(fnr) ? 'Fødselsnummeret i URLen er ugyldig' : JSON.stringify(personStatus)}</BodyShort>
      </Container>
    )
  }

  return (
    <>
      <StatusBar result={personStatus} />
      <NavigerTilbakeMeny label={'Tilbake til oppgavebenken'} path={'/'} />

      {isPending(personStatus) && <Spinner visible label={'Laster personinfo ...'} />}
      {isSuccess(personStatus) && (
        <Tabs value={fane} onChange={(val) => setFane(val as Fane)}>
          <Tabs.List>
            <Tabs.Tab value={Fane.SAKER} label="Sak og behandling" icon={<BulletListIcon title="saker" />} />
            <Tabs.Tab value={Fane.DOKUMENTER} label="Dokumenter" icon={<FileTextIcon title="dokumenter" />} />
          </Tabs.List>
          <Tabs.Panel value={Fane.SAKER}>
            <SakOversikt fnr={personStatus.data.foedselsnummer} />
          </Tabs.Panel>
          <Tabs.Panel value={Fane.DOKUMENTER}>
            <Dokumentoversikt fnr={personStatus.data.foedselsnummer} />
          </Tabs.Panel>
        </Tabs>
      )}
    </>
  )
}

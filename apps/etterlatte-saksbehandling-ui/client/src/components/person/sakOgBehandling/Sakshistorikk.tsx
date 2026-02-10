import React, { useEffect } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { mapResult } from '~shared/api/apiUtils'
import { hentSaksendringer } from '~shared/api/sak'
import Spinner from '~shared/Spinner'
import { BodyShort, Box, Detail, List } from '@navikt/ds-react'
import { formaterDatoMedKlokkeslett } from '~utils/formatering/dato'
import {
  AdressebeskyttelseGradering,
  Endringstype,
  Identtype,
  ISaksendring,
  tekstAdressebeskyttelseGradering,
  tekstEndringstype,
} from '~shared/types/sak'
import { BagdeIcon, Buildings3Icon, FolderIcon, PadlockLockedIcon } from '@navikt/aksel-icons'
import { ENHETER } from '~shared/types/Enhet'

export const Sakshistorikk = ({ sakId }: { sakId: number }) => {
  const [hentSaksendringerStatus, hentSaksendringerKall] = useApiCall(hentSaksendringer)

  useEffect(() => {
    hentSaksendringerKall(sakId)
  }, [sakId])

  const sorterNyligsteFoerstOgBakover = (a: ISaksendring, b: ISaksendring) =>
    new Date(b.tidspunkt).getTime() - new Date(a.tidspunkt).getTime()

  return mapResult(hentSaksendringerStatus, {
    pending: <Spinner label="Henter historikk..." />,
    success: (sakshistorikk) => (
      <Box marginBlock="space-12" asChild>
        <List data-aksel-migrated-v8 as="ul" size="small">
          {sakshistorikk.sort(sorterNyligsteFoerstOgBakover).map((endring) => (
            <List.Item
              title={tekstEndringstype[endring.endringstype]}
              icon={<Ikon endringstype={endring.endringstype} />}
              key={endring.id}
            >
              <Box maxWidth="18rem" borderWidth="0 0 1 0" borderColor="neutral-subtle" paddingBlock="space-0 space-2">
                <BodyShort size="small">
                  <Endringstekst endring={endring} />
                </BodyShort>
                {endring.kommentar && (
                  <Box marginBlock="space-2 space-2">
                    <BodyShort size="small">{endring.kommentar}</BodyShort>
                  </Box>
                )}
                <Detail textColor="subtle">{formaterDatoMedKlokkeslett(endring.tidspunkt)}</Detail>
                <Detail textColor="subtle">
                  {endring.identtype == Identtype.SAKSBEHANDLER ? endring.ident : 'Gjenny (automatisk)'}
                </Detail>
              </Box>
            </List.Item>
          ))}
        </List>
      </Box>
    ),
  })
}

const Ikon = ({ endringstype }: { endringstype: Endringstype }) => {
  if (endringstype === Endringstype.ENDRE_ENHET) {
    return <Buildings3Icon aria-hidden width="1rem" height="1rem" />
  } else if (endringstype === Endringstype.ENDRE_IDENT) {
    return <BagdeIcon aria-hidden width="1rem" height="1rem" />
  } else if (endringstype === Endringstype.OPPRETT_SAK) {
    return <FolderIcon aria-hidden width="1rem" height="1rem" />
  } else if (endringstype === Endringstype.ENDRE_ADRESSEBESKYTTELSE) {
    return <PadlockLockedIcon aria-hidden width="1rem" height="1rem" />
  } else if (endringstype === Endringstype.ENDRE_SKJERMING) {
    return <PadlockLockedIcon aria-hidden width="1rem" height="1rem" />
  }
}

const Endringstekst = ({ endring }: { endring: ISaksendring }) => {
  if (endring.endringstype === Endringstype.ENDRE_ENHET) {
    return (
      <>
        Fra {ENHETER[endring.foer.enhet]} til {ENHETER[endring.etter.enhet]}
      </>
    )
  } else if (endring.endringstype === Endringstype.ENDRE_IDENT) {
    return (
      <>
        Identitetsnummer {endring.foer.ident} er erstattet med {endring.etter.ident}
      </>
    )
  } else if (endring.endringstype === Endringstype.ENDRE_ADRESSEBESKYTTELSE) {
    const adressebeskyttelseFoer = endring.foer.adressebeskyttelse ?? AdressebeskyttelseGradering.UGRADERT
    const adressebeskyttelseEtter = endring.etter.adressebeskyttelse ?? AdressebeskyttelseGradering.UGRADERT
    return (
      <>
        Fra {tekstAdressebeskyttelseGradering[adressebeskyttelseFoer]} til{' '}
        {tekstAdressebeskyttelseGradering[adressebeskyttelseEtter]}
      </>
    )
  } else if (endring.endringstype === Endringstype.ENDRE_SKJERMING) {
    const skjermetVerdi = (skjermet?: boolean) => (skjermet === true ? 'Skjermet' : 'Ikke skjermet')
    return (
      <>
        Fra {skjermetVerdi(endring.foer.erSkjermet)} til {skjermetVerdi(endring.etter.erSkjermet)}
      </>
    )
  }
}

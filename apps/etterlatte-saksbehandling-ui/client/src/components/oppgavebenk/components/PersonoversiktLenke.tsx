import { Link } from '@navikt/ds-react'
import { fnrHarGyldigFormat } from '~utils/fnr'
import { useCallback } from 'react'
import { formaterFnr } from '~utils/formatering/formatering'

export enum PersonOversiktLenkeStorage {
  FnrPerson = 'fnrPerson',
}

const PersonOversiktLenke = ({ fnr, formater = false }: { fnr: string | null; formater: boolean }) => {
  const setFnrPersonStorage = useCallback(() => {
    if (fnr && fnrHarGyldigFormat(fnr)) {
      sessionStorage.setItem(PersonOversiktLenkeStorage.FnrPerson, fnr)
    }
  }, [fnr])

  const fnrOutput = formater && fnr ? formaterFnr(fnr) : fnr

  return (
    <Link href="/person" onClick={setFnrPersonStorage}>
      {fnrOutput ? fnrOutput : ''}
    </Link>
  )
}

export default PersonOversiktLenke

// TODO: flytte til en utils?
export const GetFnrFromSessionStorage = () => {
  return sessionStorage.getItem(PersonOversiktLenkeStorage.FnrPerson)
}

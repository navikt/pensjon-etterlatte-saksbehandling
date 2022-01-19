import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom';
import { getPerson, opprettSakPaaPerson } from '../../shared/api/person'

export const Person = () => {
  const [personData, setPersonData] = useState({})
  const match = useParams<{fnr: string}>();

  console.log(personData)
  useEffect(() => {
    (async () => {
        if(match.fnr) {
          const person = await getPerson(match.fnr);
          setPersonData(person);
        }
    })();
  }, [])

  const opprettSak = () => {
    if(match.fnr) {
      opprettSakPaaPerson(match.fnr)
    }
  }

  return <div>Personinfo
    <button onClick={opprettSak}>Opprett/hent sak</button>
  </div>
}

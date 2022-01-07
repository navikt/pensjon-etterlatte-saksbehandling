import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom';
import { getPerson } from '../../shared/api/person'

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

  return <div>Personinfo</div>
}

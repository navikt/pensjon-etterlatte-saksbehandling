import { useState } from 'react'
import { ClockIcon } from '~shared/icons/clockIcon'
import { DialogIcon } from '~shared/icons/dialogIcon'
import { FolderIcon } from '~shared/icons/folderIcon'
import styled from 'styled-components'

export const Tab = () => {
  const [selected, setSelected] = useState('1')

  const select = (e: any) => {
    setSelected(e.currentTarget.dataset.value)
  }

  const isSelectedClass = (val: string): string => {
    if (selected === val) {
      return 'active'
    }
    return ''
  }

  const renderSubElement = () => {
    switch (selected) {
      case '1':
        return <div>Hei</div>
      case '2':
        return <div>på</div>
      case '3':
        return <div>deg</div>
    }
    return
  }

  return (
    <>
      <MenuHead style={{ paddingTop: '14px', paddingLeft: 0, paddingRight: 0 }}>
        <Tabs>
          <IconButton data-value="1" onClick={select} className={isSelectedClass('1')}>
            <ClockIcon />
          </IconButton>
          <IconButton data-value="2" onClick={select} className={isSelectedClass('2')}>
            <DialogIcon />
          </IconButton>
          <IconButton data-value="3" onClick={select} className={isSelectedClass('3')}>
            <FolderIcon />
          </IconButton>
        </Tabs>
      </MenuHead>
      {renderSubElement()}
    </>
  )
}

const Tabs = styled.div`
  display: flex;

  > div {
    width: 33.333%;
    height: 100%;
    justify-content: center;
    align-items: center;
    text-align: center;
  }
`
const IconButton = styled.div`
  cursor: pointer;
  padding: 1em 1em 1.8em;

  &.active {
    border-bottom: 3px solid #0067c5;
  }
`

const MenuHead = styled.div`
  padding: 2em 1em;
  height: 100px;
`

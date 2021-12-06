import React, { ReactNode, Reducer, createContext, useReducer } from "react";
import { combineReducers } from "./combineReducers";
import { IMenuReducer, menuReducer, menuReducerInitialState } from "./reducers/MenuReducer";
import { IUserReducer, userReducerInitialState, userReducer } from "./reducers/UserReducer";

export interface IAppState {
  menuReducer: IMenuReducer;
  userReducer: IUserReducer;
}

export interface IAction {
  type: string;
  data?: any
}

export interface IAppContext {
  state: IAppState;
  dispatch: (action: IAction) => void;
}

export const initialState: IAppState = {
  menuReducer: menuReducerInitialState,
  userReducer: userReducerInitialState
};

const reducer = combineReducers({ menuReducer, userReducer });

export const AppContext = createContext<IAppContext>({state: initialState, dispatch: () => {}});

export const ContextProvider = (props: {children: ReactNode}) => {
  const [state, dispatch] = useReducer<Reducer<IAppState, IAction>>(reducer, initialState);

  return (
    <AppContext.Provider value={{state, dispatch}}>
      {props.children}
    </AppContext.Provider>
  );
};

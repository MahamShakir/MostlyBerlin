import {createContext, useContext, useEffect, useReducer} from "react";

const initialState = {
    openCount: 0,
    lastUpdated: null
};

function reducer(state, action) {
    switch (action.type) {

        case "HYDRATE":
            return {
                openCount: action.count,
                lastUpdated: Date.now()
            };

        case "RESOLVE":
            return {
                ...state,
                openCount: Math.max(0, state.openCount - 1),
                lastUpdated: Date.now()
            };

        case "REOPEN":
            return {
                ...state,
                openCount: state.openCount + 1,
                lastUpdated: Date.now()
            };

        default:
            return state;
    }
}

const BreakContext = createContext(null);


export function BreakProvider({children}) {

    const [state, dispatch] = useReducer(
        reducer,
        initialState
    );


    useEffect(() => {

        fetch("/api/v1/recon/results")
            .then(response => response.json())
            .then(data => {
                data = data.filter(result => result.status === "OPEN")

                dispatch({
                    type: "HYDRATE",
                    count: data.count ?? 0
                });

            })
            .catch(() => {
                // keep default zero
            });

    }, []);


    return (
        <BreakContext.Provider
            value={{
                state,
                dispatch
            }}
        >
            {children}
        </BreakContext.Provider>
    );
}


export function useBreaks() {

    const context = useContext(BreakContext);

    if (!context) {
        throw new Error(
            "useBreaks must be used inside <BreakProvider>"
        );
    }

    return context;
}
import {
    createContext,
    useContext,
    useState,
    type ReactNode,
} from "react";

interface CurrentUser {
    id: number;
    username: string;
    role: "USER" | "ADMIN";
}

interface AuthContextType {
    isAuthenticated: boolean;
    currentUser: CurrentUser | null;
    login: (token: string) => void;
    logout: () => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

interface AuthProviderProps {
    children: ReactNode;
}

function getUserFromToken(token: string): CurrentUser | null {
    try {
        const payload = JSON.parse(atob(token.split(".")[1]));

        return {
            id: Number(payload.id),
            username: payload.sub,
            role: payload.role,
        };
    } catch (error) {
        console.error("Impossible de lire le JWT :", error);
        return null;
    }
}

export function AuthProvider({children}: AuthProviderProps) {
    const token = localStorage.getItem("token");

    const [isAuthenticated, setIsAuthenticated] = useState(
        !!token
    );

    const [currentUser, setCurrentUser] = useState<CurrentUser | null>(
        token ? getUserFromToken(token) : null
    );

    const login = (token: string) => {
        localStorage.setItem("token", token);

        setIsAuthenticated(true);
        setCurrentUser(getUserFromToken(token));
    };

    const logout = () => {
        localStorage.removeItem("token");

        setIsAuthenticated(false);
        setCurrentUser(null);
    };

    return (
        <AuthContext.Provider
            value={{
                isAuthenticated,
                currentUser,
                login,
                logout,
            }}
        >
            {children}
        </AuthContext.Provider>
    );
}

export function useAuth() {
    const context = useContext(AuthContext);

    if (!context) {
        throw new Error("useAuth must be used inside AuthProvider");
    }

    return context;
}

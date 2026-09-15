import { Client, type IMessage } from "@stomp/stompjs";
import SockJS from "sockjs-client";

const API_URL = window.location.hostname === "localhost"
    ? "http://localhost:8080"
    : `http://${window.location.hostname}:8080`;

export interface GameReponse { id: number; text: string; }

export interface GameQuestion {
    questionId: number;
    text: string;
    reponses: GameReponse[];
    questionNumber: number;
    totalQuestions: number;
    saisieLibre: boolean;
    serverTimestamp: number;
}

export interface GameScores {
    scores: Record<string, number>;
    streaks: Record<string, number>;
    answeredUsers: string[];
    answeredCount: number;
    totalPlayers: number;
}

export interface GameAnswerResult {
    userId: number;
    correct: boolean;
    correctReponseId: number | null;
    pointsEarned: number;
    score: number;
    elapsedMs: number;
    streak: number;
    multiplier: number;
}

export interface GameReveal {
    correctReponseId: number | null;
    correctAnswer: string | null;
    scores: Record<string, number>;
}

export interface GameCountdown { value: number; }

export interface GameCallbacks {
    onQuestion: (q: GameQuestion) => void;
    onScores: (s: GameScores) => void;
    onAnswerResult: (r: GameAnswerResult) => void;
    onReveal: (r: GameReveal) => void;
    onResults: (r: GameScores) => void;
    onCountdown: (c: GameCountdown) => void;
    onConnected: () => void;
}

function getAuthHeaders(): Record<string, string> {
    const token = localStorage.getItem("token");
    return token ? { Authorization: `Bearer ${token}` } : {};
}

export const connectToGame = (code: string, userId: number, callbacks: GameCallbacks): Client => {
    const client = new Client({
        webSocketFactory: () => new SockJS(`${API_URL}/ws`),
        reconnectDelay: 5000,
        connectHeaders: getAuthHeaders(),
        onConnect: () => {
            client.subscribe(`/topic/game/${code}/question`, (m: IMessage) => callbacks.onQuestion(JSON.parse(m.body)));
            client.subscribe(`/topic/game/${code}/scores`, (m: IMessage) => callbacks.onScores(JSON.parse(m.body)));
            client.subscribe(`/topic/game/${code}/answer-result/${userId}`, (m: IMessage) => callbacks.onAnswerResult(JSON.parse(m.body)));
            client.subscribe(`/topic/game/${code}/reveal`, (m: IMessage) => callbacks.onReveal(JSON.parse(m.body)));
            client.subscribe(`/topic/game/${code}/results`, (m: IMessage) => callbacks.onResults(JSON.parse(m.body)));
            client.subscribe(`/topic/game/${code}/countdown`, (m: IMessage) => callbacks.onCountdown(JSON.parse(m.body)));
            callbacks.onConnected();

            // Reconnexion mi-partie : demande immédiatement la question courante
            // Le serveur ne répond que si une partie est en cours dans ce salon
            client.publish({
                destination: `/app/game/${code}/rejoin`,
                headers: getAuthHeaders(),
                body: "",
            });
        },
        onStompError: (frame) => console.error("STOMP error:", frame.headers["message"]),
        onWebSocketError: (error) => console.error("WS error:", error),
    });
    client.activate();
    return client;
};

export const sendStartGame = (client: Client, code: string) =>
    client.publish({ destination: `/app/game/${code}/start`, headers: getAuthHeaders(), body: "" });

export const sendAnswer = (client: Client, code: string, reponseId: number | null, answer: string) =>
    client.publish({ destination: `/app/game/${code}/answer`, headers: getAuthHeaders(), body: JSON.stringify({ reponseId, answer }) });

export const sendRejoin = (client: Client, code: string) =>
    client.publish({ destination: `/app/game/${code}/rejoin`, headers: getAuthHeaders(), body: "" });

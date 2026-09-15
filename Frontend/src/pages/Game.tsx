import { useCallback, useEffect, useRef, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { getSalon } from "../services/salonService";
import {
    connectToGame, sendAnswer, sendStartGame,
    type GameAnswerResult, type GameQuestion, type GameReveal, type GameScores,
} from "../services/gameSocket";
import type { Client } from "@stomp/stompjs";

/** Mélange un tableau (Fisher-Yates) sans muter l'original. */
function shuffle<T>(arr: T[]): T[] {
    const a = [...arr];
    for (let i = a.length - 1; i > 0; i--) {
        const j = Math.floor(Math.random() * (i + 1));
        [a[i], a[j]] = [a[j], a[i]];
    }
    return a;
}

/** Mélange les réponses d'une question pour que la bonne ne soit jamais au même endroit. */
function shuffleQuestion(q: GameQuestion): GameQuestion {
    return { ...q, reponses: shuffle(q.reponses) };
}
type Phase = "waiting" | "countdown" | "playing" | "answered" | "reveal" | "finished";

const TIMER_SECONDS = 15;

// ── Utilitaires sonores (Web Audio API) ──────────────────────────────────────
function playTone(freq: number, duration: number, type: OscillatorType = "sine", vol = 0.15) {
    try {
        const ctx = new AudioContext();
        const osc = ctx.createOscillator();
        const gain = ctx.createGain();
        osc.connect(gain);
        gain.connect(ctx.destination);
        osc.type = type;
        osc.frequency.setValueAtTime(freq, ctx.currentTime);
        gain.gain.setValueAtTime(vol, ctx.currentTime);
        gain.gain.exponentialRampToValueAtTime(0.0001, ctx.currentTime + duration);
        osc.start(ctx.currentTime);
        osc.stop(ctx.currentTime + duration);
    } catch { /* silencieux si AudioContext indisponible */ }
}

const playCorrect = () => {
    playTone(523, 0.1, "sine", 0.2);
    setTimeout(() => playTone(659, 0.1, "sine", 0.2), 100);
    setTimeout(() => playTone(784, 0.2, "sine", 0.2), 200);
};
const playWrong = () => playTone(200, 0.4, "sawtooth", 0.12);
const playCountdown = () => playTone(440, 0.15, "square", 0.1);
const playGo = () => { playTone(660, 0.15, "sine", 0.2); setTimeout(() => playTone(880, 0.3, "sine", 0.2), 150); };
const playTick = () => playTone(880, 0.05, "square", 0.05);

// ── Confettis ────────────────────────────────────────────────────────────────
function spawnConfetti(container: HTMLElement) {
    const colors = ["#a78bfa", "#4ade80", "#fb923c", "#38bdf8", "#f472b6", "#facc15"];
    for (let i = 0; i < 36; i++) {
        const el = document.createElement("div");
        el.className = "confetti-piece";
        el.style.cssText = `
            left:${Math.random() * 100}%;
            background:${colors[Math.floor(Math.random() * colors.length)]};
            width:${6 + Math.random() * 8}px;
            height:${6 + Math.random() * 8}px;
            border-radius:${Math.random() > 0.5 ? "50%" : "2px"};
            animation-delay:${Math.random() * 0.4}s;
            animation-duration:${0.8 + Math.random() * 0.6}s;
        `;
        container.appendChild(el);
        el.addEventListener("animationend", () => el.remove());
    }
}

// ── Composant principal ───────────────────────────────────────────────────────
export default function Game() {
    const { code } = useParams<{ code: string }>();
    const { currentUser } = useAuth();
    const navigate = useNavigate();
    const clientRef = useRef<Client | null>(null);
    const timerRef = useRef<ReturnType<typeof setInterval> | null>(null);
    const startedRef = useRef(false);
    const confettiRef = useRef<HTMLDivElement>(null);
    const submittingRef = useRef(false);

    const [connected, setConnected] = useState(false);
    const [isCreator, setIsCreator] = useState(false);
    const [phase, setPhase] = useState<Phase>("waiting");
    const [countdown, setCountdown] = useState<number | null>(null);
    const [question, setQuestion] = useState<GameQuestion | null>(null);
    const [scores, setScores] = useState<GameScores | null>(null);
    const [lastResult, setLastResult] = useState<GameAnswerResult | null>(null);
    const [selectedId, setSelectedId] = useState<number | null>(null);
    const [freeAnswer, setFreeAnswer] = useState("");
    const [revealId, setRevealId] = useState<number | null>(null);
    const [revealAnswer, setRevealAnswer] = useState<string | null>(null);
    const [finalScores, setFinalScores] = useState<GameScores | null>(null);
    const [timeLeft, setTimeLeft] = useState(TIMER_SECONDS);
    const [pointsPopup, setPointsPopup] = useState<{ pts: number; key: number } | null>(null);

    const clearTimer = useCallback(() => {
        if (timerRef.current) { clearInterval(timerRef.current); timerRef.current = null; }
    }, []);

    const startTimer = useCallback((serverTs?: number) => {
        clearTimer();
        // Synchronise sur le timestamp serveur si disponible
        const elapsed = serverTs ? Math.max(0, (Date.now() - serverTs) / 1000) : 0;
        const initial = Math.max(0, Math.round(TIMER_SECONDS - elapsed));
        setTimeLeft(initial);
        timerRef.current = setInterval(() => {
            setTimeLeft(prev => {
                if (prev <= 1) { clearTimer(); return 0; }
                if (prev <= 6) playTick();
                return prev - 1;
            });
        }, 1000);
    }, [clearTimer]);

    // Salon info pour savoir si créateur
    useEffect(() => {
        if (!code || !currentUser) return;
        getSalon(code)
            .then(salon => setIsCreator(salon.createurId === currentUser.id))
            .catch(() => {});
    }, [code, currentUser]);

    // Connexion WebSocket
    useEffect(() => {
        if (!code || !currentUser) return;

        const client = connectToGame(code, currentUser.id, {
            onConnected: () => setConnected(true),

            onCountdown: c => {
                setCountdown(c.value);
                setPhase("countdown");
                playCountdown();
            },

            onQuestion: q => {
                setQuestion(shuffleQuestion(q));
                setPhase("playing");
                setLastResult(null);
                setSelectedId(null);
                setFreeAnswer("");
                setRevealId(null);
                setRevealAnswer(null);
                setCountdown(null);
                submittingRef.current = false;
                startTimer(q.serverTimestamp);
                playGo();
            },

            onScores: s => setScores(s),

            onAnswerResult: r => {
                setLastResult(r);
                setPhase("answered");
                if (r.correct) {
                    playCorrect();
                    if (confettiRef.current) spawnConfetti(confettiRef.current);
                    setPointsPopup({ pts: r.pointsEarned, key: Date.now() });
                } else {
                    playWrong();
                    if (navigator.vibrate) navigator.vibrate([100, 50, 100]);
                }
            },

            onReveal: r => {
                clearTimer();
                setRevealId(r.correctReponseId);
                setRevealAnswer(r.correctAnswer);
                setPhase("reveal");
            },

            onResults: r => {
                clearTimer();
                setFinalScores(r);
                setPhase("finished");
            },
        });

        clientRef.current = client;
        return () => { client.deactivate(); clearTimer(); };
    }, [code, currentUser, startTimer, clearTimer]);

    // Le créateur lance auto dès que connecté
    useEffect(() => {
        if (connected && isCreator && !startedRef.current && clientRef.current && code) {
            startedRef.current = true;
            sendStartGame(clientRef.current, code);
        }
    }, [connected, isCreator, code]);

    const handleAnswer = (reponseId: number | null) => {
        if (!clientRef.current || !code || phase !== "playing" || timeLeft === 0 || submittingRef.current) return;
        submittingRef.current = true;
        setSelectedId(reponseId);
        sendAnswer(clientRef.current, code, reponseId, freeAnswer.trim());
    };

    // ── Résultats finaux ──────────────────────────────────────────────────────
    if (phase === "finished" && finalScores) {
        const sorted = Object.entries(finalScores.scores).sort(([, a], [, b]) => b - a);
        const myName = currentUser?.username;
        const myRank = sorted.findIndex(([u]) => u === myName) + 1;
        const podiumEmojis = ["🥇", "🥈", "🥉"];

        return (
            <div className="results-page">
                <div className="results-header">
                    <h1>🏆 Résultats</h1>
                    {myRank > 0 && (
                        <p className="my-rank">
                            Tu termines <strong>{myRank === 1 ? "🥇 1er" : myRank === 2 ? "🥈 2e" : myRank === 3 ? "🥉 3e" : `${myRank}e`}</strong>
                            {myRank === 1 && " — Félicitations ! 🎉"}
                        </p>
                    )}
                </div>

                <div className="podium">
                    {sorted.slice(0, 3).map(([username, score], i) => (
                        <div key={username} className={`podium-place podium-${i + 1} ${username === myName ? "me" : ""}`}
                            style={{ animationDelay: `${i * 0.15}s` }}>
                            <span className="podium-emoji">{podiumEmojis[i]}</span>
                            <span className="podium-name">{username}</span>
                            <span className="podium-score">{score} pts</span>
                        </div>
                    ))}
                </div>

                {sorted.length > 3 && (
                    <table className="scores-table" style={{ marginTop: 16 }}>
                        <tbody>
                            {sorted.slice(3).map(([username, score], i) => (
                                <tr key={username} className={username === myName ? "me-row" : ""}>
                                    <td style={{ width: 32 }}>{i + 4}</td>
                                    <td>{username}</td>
                                    <td style={{ textAlign: "right" }}>{score} pts</td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                )}

                <button onClick={() => navigate("/dashboard")} style={{ marginTop: 28, width: "100%" }}>
                    Retour au dashboard
                </button>
            </div>
        );
    }

    if (!code) return <p className="error">Code du salon manquant.</p>;

    const timerPct = (timeLeft / TIMER_SECONDS) * 100;
    const myName = currentUser?.username ?? "";

    return (
        <div className="game-page">
            {/* Zone confettis */}
            <div className="confetti-container" ref={confettiRef} />

            {/* Popup points */}
            {pointsPopup && (
                <div className="points-popup" key={pointsPopup.key}>
                    +{pointsPopup.pts} pts
                </div>
            )}

            <div className="game-header">
                <h1>Quiz Arena</h1>
                <p>Salon : <strong>{code}</strong> — {connected ? "✅" : "⏳"}</p>
            </div>

            {/* En attente */}
            {phase === "waiting" && (
                <div className="game-countdown">
                    <p>En attente du lancement...</p>
                    {isCreator && (
                        <button onClick={() => { if (clientRef.current && code) sendStartGame(clientRef.current, code); }} disabled={!connected}>
                            🚀 Démarrer
                        </button>
                    )}
                </div>
            )}

            {/* Compte à rebours */}
            {phase === "countdown" && (
                <div className="game-countdown">
                    <p className="countdown-label">Préparez-vous !</p>
                    <div className="countdown-number" key={countdown}>{countdown}</div>
                </div>
            )}

            {/* Question */}
            {(phase === "playing" || phase === "answered" || phase === "reveal") && question && (
                <div className="question-transition" key={`${question.questionId}-${phase}`}>

                    {/* Progress bar */}
                    <div className="question-meta">
                        <span className="game-progress">
                            Q{question.questionNumber} / {question.totalQuestions}
                        </span>
                        {phase !== "reveal" && (
                            <span className={`timer-text ${timeLeft <= 5 ? "urgent" : ""}`}>{timeLeft}s</span>
                        )}
                    </div>

                    {phase !== "reveal" && (
                        <div className="timer-bar-track">
                            <div className={`timer-bar-fill ${timeLeft <= 5 ? "urgent" : ""}`}
                                style={{ width: `${timerPct}%` }} />
                        </div>
                    )}

                    {phase === "reveal" && (
                        <div className="reveal-banner">
                            {revealAnswer ? `✅ Réponse : ${revealAnswer}` : "⏳ Question suivante..."}
                        </div>
                    )}

                    <p className="game-question">{question.text}</p>

                    {/* Streak badge */}
                    {lastResult && lastResult.streak >= 2 && phase === "answered" && (
                        <div className="streak-badge">
                            🔥 Série de {lastResult.streak} !
                            {lastResult.multiplier > 1 && <span> ×{lastResult.multiplier}</span>}
                        </div>
                    )}

                    {/* Réponses QCM */}
                    {!question.saisieLibre && (
                        <div className="game-answers">
                            {question.reponses.map(r => {
                                let cls = "answer-btn";
                                if (phase === "reveal") {
                                    if (r.id === revealId) cls += " correct";
                                    else if (r.id === selectedId) cls += " wrong";
                                } else if (phase === "answered" && lastResult) {
                                    if (r.id === lastResult.correctReponseId) cls += " correct";
                                    else if (r.id === selectedId && !lastResult.correct) cls += " wrong";
                                } else if (r.id === selectedId) {
                                    cls += " selected";
                                }
                                return (
                                    <button key={r.id} className={cls}
                                        onClick={() => handleAnswer(r.id)}
                                        disabled={phase !== "playing" || timeLeft === 0}>
                                        {r.text}
                                    </button>
                                );
                            })}
                        </div>
                    )}

                    {/* Saisie libre */}
                    {question.saisieLibre && (
                        <div className="free-answer">
                            <input value={freeAnswer}
                                onChange={e => setFreeAnswer(e.target.value)}
                                onKeyDown={e => { if (e.key === "Enter" && freeAnswer.trim()) handleAnswer(null); }}
                                disabled={phase !== "playing" || timeLeft === 0}
                                placeholder="Tape ta réponse..." autoFocus />
                            <button onClick={() => handleAnswer(null)}
                                disabled={phase !== "playing" || timeLeft === 0 || !freeAnswer.trim()}>
                                Valider
                            </button>
                        </div>
                    )}

                    {/* Feedback réponse */}
                    {phase === "answered" && lastResult && (
                        <div className={`game-feedback ${lastResult.correct ? "ok" : "ko"}`}>
                            {lastResult.correct ? "✅ Bonne réponse !" : "❌ Mauvaise réponse"}
                            {" · "}<strong>+{lastResult.pointsEarned} pts</strong>
                            {" · "}Total : {lastResult.score} pts
                        </div>
                    )}

                    {phase === "playing" && timeLeft === 0 && !lastResult && (
                        <div className="game-feedback ko">⏰ Temps écoulé !</div>
                    )}

                    {/* Qui a répondu */}
                    {scores && (
                        <div className="answered-indicators">
                            {scores.answeredUsers && Object.keys(scores.scores).map(username => {
                                const done = scores.answeredUsers?.includes(username);
                                const isMe = username === myName;
                                return (
                                    <span key={username}
                                        className={`player-chip ${done ? "done" : "waiting"} ${isMe ? "me" : ""}`}
                                        title={done ? `${username} a répondu` : `${username} réfléchit...`}>
                                        {done ? "✓" : "…"} {username}
                                    </span>
                                );
                            })}
                        </div>
                    )}
                </div>
            )}

            {/* Leaderboard live */}
            {scores && phase !== "waiting" && phase !== "countdown" && phase !== "finished" && (
                <div className="live-leaderboard">
                    <h3>Classement</h3>
                    {Object.entries(scores.scores)
                        .sort(([, a], [, b]) => b - a)
                        .map(([username, score], i) => {
                            const streak = scores.streaks?.[username] ?? 0;
                            const isMe = username === myName;
                            return (
                                <div key={username} className={`leaderboard-row ${isMe ? "me" : ""}`}>
                                    <span className="lb-rank">{i === 0 ? "🥇" : i === 1 ? "🥈" : i === 2 ? "🥉" : i + 1}</span>
                                    <span className="lb-name">{username}</span>
                                    {streak >= 2 && <span className="lb-streak">🔥{streak}</span>}
                                    <span className="lb-score">{score} pts</span>
                                </div>
                            );
                        })}
                </div>
            )}
        </div>
    );
}
